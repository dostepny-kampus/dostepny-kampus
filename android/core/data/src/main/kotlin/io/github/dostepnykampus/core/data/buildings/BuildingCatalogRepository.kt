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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Plik konturów OSM w zasobach aplikacji; przy budowie kopiuje go z `data/buildings` zadanie `copyBuildingsAsset`. */
const val BUILDINGS_ASSET = "pwr-buildings.geojson"

/**
 * Katalog budynków z pliku konturów w aplikacji, bez sieci i bez logowania (US1, US1.2).
 *
 * - Budynki są posortowane naturalnie po symbolu: litera, potem numer jako liczba, np. A-2 przed A-10.
 * - Wejścia nie obejmują rodzajów `emergency`, `exit` i `garage`, bo to nie są wejścia dla odwiedzających
 *   (BR-25, decyzja 76).
 *
 * Plik jest czytany i parsowany raz, w [ioContext]; kolejne wywołania [catalog], także równoległe, dostają
 * ten sam katalog.
 *
 * @param ioContext kontekst odczytu pliku i parsowania, np. dyspozytor wejścia-wyjścia
 * @param readGeoJson odczyt treści pliku konturów
 */
class BuildingCatalogRepository(private val ioContext: CoroutineContext, private val readGeoJson: () -> String) {
    private val mutex = Mutex()
    private var cached: BuildingCatalog? = null

    /**
     * Zwraca katalog budynków.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    suspend fun catalog(): BuildingCatalog =
        mutex.withLock {
            cached ?: withContext(ioContext) { parseBuildingCatalog(readGeoJson()).forVisitors() }.also { cached = it }
        }
}

/** [BuildingCatalogRepository] na pliku [BUILDINGS_ASSET] z zasobów aplikacji [assets]. */
internal fun assetRepository(
    assets: AssetManager,
    ioContext: CoroutineContext = Dispatchers.IO,
): BuildingCatalogRepository =
    BuildingCatalogRepository(ioContext) { assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() } }

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
