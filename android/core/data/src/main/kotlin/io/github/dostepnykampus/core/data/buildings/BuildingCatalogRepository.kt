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
 * Lista z serwera nie ma jeszcze wejść, więc zostają wejścia z pliku, ale tylko budynków, które serwer podał:
 * katalog opisuje wejścia swoich budynków. Atrybucja udanego odczytu jest z serwera, bo dotyczy konturów.
 * Budynki z serwera są sortowane jak te z pliku.
 *
 * Wynik inny niż [ApiResult.Success], czyli brak sieci, błąd serwera albo nieznany zasób, daje cały katalog
 * z pliku, z jego wejściami i atrybucją. Bez [listBuildings] odczyt jest od razu z pliku, jak dotychczas.
 *
 * - Budynki są posortowane naturalnie po symbolu: litera, potem numer jako liczba, np. A-2 przed A-10.
 * - Wejścia nie obejmują rodzajów `emergency`, `exit` i `garage`, bo to nie są wejścia dla odwiedzających
 *   (BR-25, decyzja 76).
 *
 * Plik jest czytany i parsowany raz, w [ioContext]; kolejne wywołania [catalog], także równoległe, dostają
 * ten sam katalog. Pierwszy wynik, także zapasowy z pliku, zostaje na czas procesu.
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

    /**
     * Zwraca katalog budynków.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun catalog(): BuildingCatalog = mutex.withLock { cached ?: load().also { cached = it } }

    private suspend fun load(): BuildingCatalog {
        val bundled = withContext(ioContext) { parseBuildingCatalog(readGeoJson()).forVisitors() }
        val server = (listBuildings?.invoke() as? ApiResult.Success)?.value
        return if (server == null) {
            bundled
        } else {
            val symbols = server.buildings.map { item -> item.building.symbol }.toSet()
            val outlines =
                bundled.copy(
                    buildings = server.buildings.map { item -> item.building },
                    entrances = bundled.entrances.filter { entrance -> entrance.buildingSymbol in symbols },
                    attribution = server.source,
                )
            outlines.forVisitors()
        }
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
