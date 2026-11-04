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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 * Serwer jest pytany jednym wspólnym zapytaniem: [catalogUpdates], [catalog] i [refresh] wywołane w trakcie trwającego
 * pobrania czekają na jego wynik, zamiast pytać drugi raz. Katalog pobrany z serwera jest widoczny dla wszystkich,
 * także dla mapy, która sama go nie zamawiała. [catalogUpdates] oddaje najpierw katalog z pliku (albo już pobrany
 * z serwera), bez czekania na pobranie (NFR-11), a potem katalog z serwera, gdy ten odpowie niepustą listą; plik nie
 * nadpisuje przyjętego katalogu z serwera. [catalog] czeka na wynik pobrania i zapamiętuje go; [refresh] czyta plik
 * od nowa i pyta serwer, chyba że pobranie już trwa. Bez odpowiedzi serwera katalog pobrany wcześniej z serwera
 * zostaje, a plik jest zapasem tylko wtedy, gdy z serwera nic jeszcze nie pobrano (NFR-10).
 *
 * Anulowanie przerywa pytanie serwera, o ile [listBuildings] jest anulowalne; odczyt pliku [readGeoJson] jest
 * blokujący, więc anulowanie działa dopiero po nim. Gdy anulowany zostanie ten, kto prowadził wspólne pobranie,
 * pozostali czekający pytają serwer od nowa.
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
    private val lock = Any()
    private var inflight: CompletableDeferred<BuildingCatalog>? = null
    private val downloaded = MutableStateFlow<BuildingCatalog?>(null)

    @Volatile private var cached: BuildingCatalog? = null

    @Volatile private var bundled: BuildingCatalog? = null

    /**
     * Zwraca katalog budynków; gdy trwa pobranie z serwera, czeka na jego wynik.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun catalog(): BuildingCatalog = fetched(isFresh = false)

    /**
     * Strumień katalogu dla mapy: najpierw katalog z pliku (albo już pobrany z serwera), bez czekania na pobranie,
     * potem katalog z serwera, gdy ten odpowie niepustą listą, także przyjęty przez [catalog] lub [refresh]. Błąd,
     * brak sieci i pusta lista nie dają drugiej emisji. Strumień kończy się po katalogu z serwera; bez niego czeka na
     * katalog przyjęty później (np. przez [refresh]) i kończy go anulowanie zbierającego.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    fun catalogUpdates(): Flow<BuildingCatalog> =
        channelFlow {
            val first = downloaded.value ?: readBundled(isFresh = false)
            send(first)
            launch { fetched(isFresh = false) }
            send(downloaded.filterNotNull().first { server -> server !== first })
        }

    /**
     * Pyta serwer i czyta plik od nowa, zapamiętuje wynik i zwraca katalog dla „Spróbuj ponownie”, gdy budynku
     * nie ma w telefonie (NFR-10). Gdy pobranie już trwa, czeka na jego wynik. Gdy serwer nie odpowie, zostaje
     * ostatni katalog z serwera, a bez niego plik.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun refresh(): BuildingCatalog = fetched(isFresh = true)

    private suspend fun fetched(isFresh: Boolean): BuildingCatalog {
        val ready = if (isFresh) null else cached
        if (ready != null) return ready
        val mine = CompletableDeferred<BuildingCatalog>()
        val running =
            synchronized(lock) {
                val current = inflight
                if (current == null) inflight = mine
                current
            }
        return if (running == null) lead(mine, isFresh) else follow(running, isFresh)
    }

    private suspend fun follow(running: CompletableDeferred<BuildingCatalog>, isFresh: Boolean): BuildingCatalog {
        running.join()
        return if (running.isCancelled) fetched(isFresh) else running.await()
    }

    private suspend fun lead(mine: CompletableDeferred<BuildingCatalog>, isFresh: Boolean): BuildingCatalog {
        try {
            val file = readBundled(isFresh)
            val server = nonEmptyServerList()
            if (server != null) {
                downloaded.value = fromServer(file, server)
            }
            val result = downloaded.value ?: file
            cached = result
            mine.complete(result)
            return result
        } finally {
            mine.cancel()
            synchronized(lock) { inflight = null }
        }
    }

    private suspend fun readBundled(isFresh: Boolean): BuildingCatalog =
        (if (isFresh) null else bundled)
            ?: withContext(ioContext) { parseBuildingCatalog(readGeoJson()).forVisitors() }.also { bundled = it }

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
