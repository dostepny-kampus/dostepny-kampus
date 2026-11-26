/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.unitSquareBuilding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Dane mapy liczone raz na proces dla tych samych budynków. Wymagania: NFR-11 */
class CampusMapDataCacheTest {
    private var computed = 0
    private val cache =
        CampusMapDataCache { catalog ->
            computed += 1
            campusMapData(catalog)
        }

    /** Ten sam katalog z pliku, a potem z serwera z innym opisem źródła, liczy się raz. Wymagania: NFR-11 */
    @Test
    fun computesOnceForSameBuildings() {
        val first = cache.dataFor(catalog(symbol = "A-1", attribution = "plik"))
        val second = cache.dataFor(catalog(symbol = "A-1", attribution = "serwer"))
        assertEquals(1 to first, computed to second, "jedno liczenie i ten sam wynik")
    }

    /** Katalog z innym budynkiem liczy się od nowa. Wymagania: NFR-11 */
    @Test
    fun computesAgainForOtherBuildings() {
        cache.dataFor(catalog(symbol = "A-1", attribution = "plik"))
        val other = cache.dataFor(catalog(symbol = "C-13", attribution = "plik"))
        assertEquals(2 to listOf("C-13"), computed to other.labels, "drugie liczenie dla nowego budynku")
    }

    /**
     * Te same budynki z serwera, który zna już plany pięter, liczą się od nowa, bo poziom budynku jest w danych
     * konturów. Wymagania: US16.1, NFR-11
     */
    @Test
    fun computesAgainForOtherLevels() {
        cache.dataFor(catalog(symbol = "A-1", attribution = "plik"))
        cache.dataFor(catalog(symbol = "A-1", attribution = "serwer", detailed = setOf("A-1")))
        assertEquals(2, computed, "drugie liczenie po zmianie budynków z planem pięter")
    }

    private fun catalog(symbol: String, attribution: String, detailed: Set<String> = emptySet()): BuildingCatalog =
        BuildingCatalog(
            buildings = listOf(unitSquareBuilding(symbol)),
            entrances = emptyList(),
            attribution = attribution,
            detailed = detailed,
        )
}
