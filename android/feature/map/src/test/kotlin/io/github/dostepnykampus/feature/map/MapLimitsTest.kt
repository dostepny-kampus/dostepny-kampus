/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.mapLimits
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.log2

/** Najmniejsze powiększenie obszaru kampusu V1 na telefonie o dużej gęstości i na wysokiej szerokości. */
class MapLimitsTest {
    private val span = 360.0 / 16_384

    /**
     * Wiersz: V1, najmniejsze powiększenie pokazuje cały obszar kampusu także na telefonie, na którym dp to kilka
     * pikseli; przy gęstości 3 widok 1200 × 2400 pikseli ma 400 × 800 dp, a 3072 × 2304 ma 1024 × 768 dp.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun computesMinZoomInDp() {
        val density = Density(density = 3f)
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val tall = CampusBounds(west = 0.0, south = -span / 2, east = span / 8, north = span / 2)
        val portrait = density.mapLimits(wide, IntSize(width = 1200, height = 2400))
        val landscape = density.mapLimits(tall, IntSize(width = 3072, height = 2304))
        assertEquals(14.0 + log2(400.0 / 512), portrait?.minZoom ?: 0.0, ZOOM_DELTA)
        assertEquals(14.0 + log2(768.0 / 512), landscape?.minZoom ?: 0.0, ZOOM_DELTA)
    }

    /**
     * Wiersz: V1, na 60° szerokości obszar w rzucie mapy jest dwa razy wyższy niż w stopniach, więc najmniejsze
     * powiększenie obszaru ograniczonego wysokością jest o jeden poziom niższe niż na równiku.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun computesMinZoomAt60Degrees() {
        val tall = CampusBounds(west = 17.0, south = 60.0 - span / 4, east = 17.0 + span / 8, north = 60.0 + span / 4)
        assertEquals(14.0 + log2(1.5), MapLimits(tall, widthDp = 1024.0, heightDp = 768.0).minZoom, ZOOM_DELTA)
    }

    /**
     * Wiersz: V1, podkład sięga poza prostokąt terenu o pół widoku mapy przy najmniejszym powiększeniu,
     * więc przy brzegu terenu nie ma pustych pasów: na pionowym telefonie 400 × 800 dp i na poziomym 1024 × 768 dp.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun extendsTilesByHalfView() {
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val tall = CampusBounds(west = 0.0, south = -span / 2, east = span / 8, north = span / 2)
        val portrait = MapLimits(wide, widthDp = 400.0, heightDp = 800.0).tileBounds
        val landscape = MapLimits(tall, widthDp = 1024.0, heightDp = 768.0).tileBounds
        assertEquals(-span / 2, portrait.west, DEGREE_DELTA, "pionowy: zachód o pół szerokości widoku")
        assertEquals(1.5 * span, portrait.east, DEGREE_DELTA, "pionowy: wschód o pół szerokości widoku")
        assertEquals(span / 8 + span, portrait.north, DEGREE_DELTA, "pionowy: północ o pół wysokości widoku")
        assertEquals(span / 8 + 2 * span / 3, landscape.east, DEGREE_DELTA, "poziomy: wschód o pół szerokości")
        assertEquals(span, landscape.north, DEGREE_DELTA, "poziomy: północ o pół wysokości widoku")
    }

    /**
     * Wiersz: V1, przelot na inny teren — podkład zaczyna się pół poziomu poniżej najmniejszego powiększenia terenu,
     * więc w środku przelotu nie jest rysowany ani pobierany, a w kafelkach podkładu terenu jest od tego powiększenia.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun hidesBasemapBelowArea() {
        val area = CampusBounds(west = 0.0, south = 0.0, east = span, north = span)
        val limits = MapLimits(area, widthDp = 400.0, heightDp = 800.0)
        assertEquals(limits.minZoom - 0.5, limits.basemap.minZoom, ZOOM_DELTA, "podkład od pół poziomu niżej")
        assertEquals(limits.tileBounds, limits.basemap.bounds, "kafelki podkładu w prostokącie z pół widoku")
    }

    private companion object {
        const val ZOOM_DELTA = 1e-3
        const val DEGREE_DELTA = 1e-9
    }
}
