/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.MoveLimits
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Granice kamery na czas ruchu mapy na prostokątach testowych (nie dane PWr). */
class MoveLimitsTest {
    private val main = limitsOf(CampusBounds(west = 0.0, south = 0.0, east = 0.01, north = 0.01))
    private val other = limitsOf(CampusBounds(west = 0.03, south = 0.0, east = 0.04, north = 0.01))
    private val applied = mutableListOf<MapLimits?>()
    private val limits = MoveLimits { applied += it }

    /**
     * Wiersz: V1, przelot na inny teren nie jest zatrzymywany przez granice starego ani nowego terenu: start
     * przelotu zdejmuje granice, granice nowego terenu czekają na koniec przelotu.
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun liftsLimitsForFlight() {
        limits.limit(main)
        limits.startMove()
        limits.limit(other)
        assertEquals(listOf(main, null), applied, "w trakcie przelotu bez granic")
        limits.endMove()
        assertEquals(listOf(main, null, other), applied, "po przelocie granice nowego terenu")
    }

    /**
     * Wiersz: V1, przesunięcie do budynku nad kartą nie jest zatrzymywane przez granice środka liczone na nowo
     * w każdej klatce: start przesunięcia zdejmuje granice, koniec je przywraca.
     *
     * Wymagania: US2.8, BR-G5
     */
    @Test
    fun liftsLimitsForShift() {
        limits.limit(main)
        limits.startMove()
        assertEquals(listOf(main, null), applied, "w trakcie przesunięcia bez granic")
        limits.endMove()
        assertEquals(listOf(main, null, main), applied, "po przesunięciu granice terenu")
    }

    /** Wiersz: V1, bez ruchu nowe granice terenu działają od razu, a koniec innego ruchu ich nie rusza. */
    @Test
    fun limitsAtOnceWithoutFlight() {
        limits.limit(main)
        limits.endMove()
        limits.limit(other)
        assertEquals(listOf(main, other), applied, "granice od razu, bez zdejmowania")
    }

    /** Wiersz: V1, ruch przerwany gestem albo innym ruchem kończy się granicami terenu pokazanego na mapie. */
    @Test
    fun restoresLimitsAfterCancel() {
        limits.limit(main)
        limits.startMove()
        limits.endMove()
        limits.endMove()
        assertEquals(listOf(main, null, main), applied, "granice wracają raz")
    }

    private fun limitsOf(bounds: CampusBounds) = MapLimits(bounds = bounds, widthDp = 400.0, heightDp = 800.0)
}
