/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.tiles

import io.github.dostepnykampus.core.map.style.BASEMAP_TILES
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.feature.map.style.BASEMAP_TILES
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val VERSION = "20251119_001001_pt"

/** Kafelki podkładu względem prostokątów terenów; prostokąty testowe, nie dane PWr. Wymagania: NFR-39 */
class BasemapTileTest {
    private val area = CampusBounds(west = 17.06, south = 51.10, east = 17.07, north = 51.11)
    private val far = CampusBounds(west = 17.2, south = 51.0, east = 17.21, north = 51.01)

    /** Kafelek z prostokątem terenu idzie do serwera, sąsiedni na wschód i na południe nie. */
    @Test
    fun keepsOnlyTilesOverArea() {
        assertFalse(isOutside(tile(z = 14, x = 8968, y = 5477), listOf(area)), "kafelek terenu")
        assertTrue(isOutside(tile(z = 14, x = 8969, y = 5477), listOf(area)), "kafelek na wschód")
        assertTrue(isOutside(tile(z = 14, x = 8968, y = 5478), listOf(area)), "kafelek na południe")
    }

    /** Wiersz: V1, zmiana terenu: w przelocie kafelki obu terenów, a kafelek małego powiększenia obejmuje teren. */
    @Test
    fun keepsTilesOfEachArea() {
        assertFalse(isOutside(tile(z = 11, x = 1121, y = 684), listOf(far, area)), "kafelek drugiego terenu")
        assertFalse(isOutside(tile(z = 5, x = 17, y = 10), listOf(area)), "kafelek nad całym terenem")
    }

    /** Bez prostokątów filtr zatrzymuje kafelki podkładu i przepuszcza inne zasoby. */
    @Test
    fun keepsOtherRequests() {
        assertTrue(isOutside(tile(z = 14, x = 1, y = 1), emptyList()), "bez prostokątów kafelek nie idzie do sieci")
        assertFalse(isOutside(BASEMAP_TILES, listOf(area)), "opis źródła")
        assertFalse(isOutside("https://tiles.openfreemap.org/fonts/Noto/0-255.pbf", listOf(area)), "napisy")
    }

    private fun tile(z: Int, x: Int, y: Int) = "$BASEMAP_TILES/$VERSION/$z/$x/$y.pbf"
}
