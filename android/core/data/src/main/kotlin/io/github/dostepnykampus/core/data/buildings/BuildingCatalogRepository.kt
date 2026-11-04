/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import android.content.res.AssetManager
import io.github.dostepnykampus.core.geo.parseBuildingCatalog
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Plik konturów OSM w zasobach aplikacji; przy budowie kopiuje go z `data/buildings` zadanie `copyBuildingsAsset`. */
const val BUILDINGS_ASSET = "pwr-buildings.geojson"

/**
 * Katalog budynków dla mapy (US1, US1.2).
 *
 * Gdy [listBuildings] jest podane, kontury i nazwy biorą się z serwera, a wejścia zostają z pliku konturów.
 * Lista z serwera (`GET /buildings`) nie ma wejść, więc katalog ma wejścia z pliku, ale tylko budynków, które
 * serwer podał: katalog opisuje wejścia swoich budynków. Wejścia z dniem i źródłem oznaczenia karta budynku
 * bierze osobno z [ServerEntrances] (US17.3). Atrybucja udanego odczytu jest z serwera, bo dotyczy konturów.
 * Budynki z serwera są sortowane jak te z pliku.
 *
 * Wynik inny niż [ApiResult.Success], czyli brak sieci, błąd serwera albo nieznany zasób, daje cały katalog
 * z pliku, z jego wejściami i atrybucją, gdy z serwera nic jeszcze nie pobrano. Tak samo pusta lista z serwera:
 * baza bez zaimportowanych budynków nie może dać pustej mapy. Bez [listBuildings] odczyt jest od razu
 * z pliku, jak dotychczas.
 *
 * - Budynki są posortowane naturalnie po symbolu: litera, potem numer jako liczba, np. A-2 przed A-10.
 * - Wejścia nie obejmują rodzajów `emergency`, `exit` i `garage`, bo to nie są wejścia dla odwiedzających
 *   (BR-25, decyzja 76).
 *
 * [catalogUpdates] oddaje katalog w dwóch etapach: z pliku zaraz po odczycie, potem z serwera, gdy ten odpowie
 * niepustą listą. Mapa pokazuje więc kontury bez czekania na sieć (NFR-11), a katalog z serwera zastępuje plik bez
 * ponownego otwierania ekranu; plik nigdy nie nadpisuje przyjętego katalogu z serwera. [catalog] czeka na oba etapy,
 * czyta plik i pyta serwer raz, w [ioContext]; kolejne wywołania, także równoległe, dostają ten sam katalog z pamięci.
 * [refresh] pyta serwer i czyta plik od nowa. Bez odpowiedzi serwera katalog pobrany wcześniej z serwera zostaje,
 * a plik jest zapasem tylko wtedy, gdy z serwera nic jeszcze nie pobrano (NFR-10).
 *
 * @param ioContext kontekst odczytu pliku i parsowania, np. dyspozytor wejścia-wyjścia
 * @param listBuildings lista z serwera; `null` zostawia katalog wyłącznie z pliku
 * @param readGeoJson odczyt treści pliku konturów
 */
class BuildingCatalogRepository(
    private val ioContext: CoroutineContext,
    private val listBuildings: (suspend () -> ApiResult<BuildingList>)? = null,
    private val readGeoJson: () -> String,
) {
    private val mutex = Mutex()
    private var cached: BuildingCatalog? = null
    private var downloaded: BuildingCatalog? = null

    /**
     * Zwraca katalog budynków.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun catalog(): BuildingCatalog = mutex.withLock { cached ?: load().also { cached = it } }

    /**
     * Strumień katalogu dla mapy: najpierw katalog z pliku (albo już pobrany z serwera), potem katalog z serwera, gdy
     * ten odpowie niepustą listą. Błąd, brak sieci i pusta lista nie dają drugiej emisji. Gdy katalog jest już
     * w pamięci, strumień oddaje go i kończy się. Anulowanie zbierającego przerywa odczyt i pytanie serwera.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    fun catalogUpdates(): Flow<BuildingCatalog> =
        flow {
            val ready = mutex.withLock { cached }
            if (ready == null) emitFileThenServer() else emit(ready)
        }

    private suspend fun FlowCollector<BuildingCatalog>.emitFileThenServer() {
        val bundled = readBundled()
        emit(mutex.withLock { downloaded } ?: bundled)
        val server = nonEmptyServerList()
        if (server != null) {
            emit(accept(bundled, server))
        } else {
            mutex.withLock { cached = cached ?: downloaded ?: bundled }
        }
    }

    /**
     * Pyta serwer i czyta plik od nowa, zapamiętuje wynik i zwraca katalog dla „Spróbuj ponownie”, gdy budynku
     * nie ma w telefonie (NFR-10). Gdy serwer nie odpowie, zostaje ostatni katalog z serwera, a bez niego plik.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun refresh(): BuildingCatalog = mutex.withLock { load().also { cached = it } }

    private suspend fun readBundled(): BuildingCatalog =
        withContext(ioContext) { parseBuildingCatalog(readGeoJson()).forVisitors() }

    private suspend fun accept(bundled: BuildingCatalog, server: BuildingList): BuildingCatalog =
        mutex.withLock {
            fromServer(bundled, server).also { catalog ->
                downloaded = catalog
                cached = catalog
            }
        }

    private suspend fun load(): BuildingCatalog {
        val bundled = readBundled()
        val server = nonEmptyServerList()
        if (server != null) {
            downloaded = fromServer(bundled, server)
        }
        return downloaded ?: bundled
    }

    private fun fromServer(bundled: BuildingCatalog, server: BuildingList): BuildingCatalog {
        val symbols = server.buildings.map { item -> item.building.symbol }.toSet()
        val outlines =
            bundled.copy(
                buildings = server.buildings.map { item -> item.building },
                entrances = bundled.entrances.filter { entrance -> entrance.buildingSymbol in symbols },
                attribution = server.source,
            )
        return outlines.forVisitors()
    }

    private suspend fun nonEmptyServerList(): BuildingList? {
        val result = listBuildings?.invoke()
        return if (result is ApiResult.Success && result.value.buildings.isNotEmpty()) result.value else null
    }
}

/** [BuildingCatalogRepository] na pliku [BUILDINGS_ASSET]; [listBuildings] to lista z serwera, a bez niej sam plik. */
internal fun assetRepository(
    assets: AssetManager,
    ioContext: CoroutineContext = Dispatchers.IO,
    listBuildings: (suspend () -> ApiResult<BuildingList>)? = null,
): BuildingCatalogRepository =
    BuildingCatalogRepository(
        ioContext = ioContext,
        listBuildings = listBuildings,
        readGeoJson = { assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() } },
    )

private val NOT_FOR_VISITORS = setOf("emergency", "exit", "garage")

private val NATURAL_ORDER =
    compareBy<Building>(
        { it.symbol.substringBefore('-') },
        { it.symbol.filter(Char::isDigit).toIntOrNull() },
        { it.symbol },
    )

/** Katalog dla odwiedzających: budynki w kolejności naturalnej, wejścia bez rodzajów z [NOT_FOR_VISITORS]. */
internal fun BuildingCatalog.forVisitors(): BuildingCatalog =
    copy(
        buildings = buildings.sortedWith(NATURAL_ORDER),
        entrances = entrances.filterNot { it.type in NOT_FOR_VISITORS },
    )
