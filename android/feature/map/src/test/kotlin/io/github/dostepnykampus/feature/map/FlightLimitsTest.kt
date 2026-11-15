/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.FlightLimits
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Granice kamery na czas przelotu na inny teren na prostokątach testowych (nie dane PWr). */
class FlightLimitsTest {
    private val main = MapLimits(CampusBounds(west = 0.0, south = 0.0, east = 0.01, north = 0.01), 400.0, 800.0)
    private val other = MapLimits(CampusBounds(west = 0.03, south = 0.0, east = 0.04, north = 0.01), 400.0, 800.0)
    private val applied = mutableListOf<MapLimits?>()
    private val limits = FlightLimits { applied += it }

    /**
     * Wiersz: V1, przelot na inny teren nie jest zatrzymywany przez granice starego ani nowego terenu: start
     * przelotu zdejmuje granice, granice nowego terenu czekają na koniec przelotu.
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun liftsLimitsForFlight() {
        limits.limit(main)
        limits.startFlight()
        limits.limit(other)
        assertEquals(listOf(main, null), applied, "w trakcie przelotu bez granic")
        limits.endFlight()
        assertEquals(listOf(main, null, other), applied, "po przelocie granice nowego terenu")
    }

    /** Wiersz: V1, bez przelotu nowe granice terenu działają od razu, a koniec innego ruchu ich nie rusza. */
    @Test
    fun limitsAtOnceWithoutFlight() {
        limits.limit(main)
        limits.endFlight()
        limits.limit(other)
        assertEquals(listOf(main, other), applied, "granice od razu, bez zdejmowania")
    }

    /** Wiersz: V1, przelot przerwany gestem albo innym ruchem kończy się granicami terenu pokazanego na mapie. */
    @Test
    fun restoresLimitsAfterCancel() {
        limits.limit(main)
        limits.startFlight()
        limits.endFlight()
        limits.endFlight()
        assertEquals(listOf(main, null, main), applied, "granice wracają raz")
    }
}
