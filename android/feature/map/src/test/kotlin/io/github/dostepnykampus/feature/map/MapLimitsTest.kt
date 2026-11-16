/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
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
     * Wiersz: V1, najmniejsze powiększenie wypełnia mapę prostokątem terenu w krótszym wymiarze także na telefonie,
     * na którym dp to kilka pikseli; przy gęstości 3 widok 1200 × 2400 pikseli ma 400 × 800 dp, a 3072 × 2304 ma
     * 1024 × 768 dp. Pas 512 × 128 dp przy powiększeniu 14 wypełnia 800 dp wysokości, a pas 64 × 512 dp — 1024 dp
     * szerokości.
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
        assertEquals(14.0 + log2(800.0 / 128), portrait?.minZoom ?: 0.0, ZOOM_DELTA)
        assertEquals(18.0, landscape?.minZoom ?: 0.0, ZOOM_DELTA)
    }

    /**
     * Wiersz: V1, na 60° szerokości obszar w rzucie mapy jest dwa razy wyższy niż w stopniach, więc najmniejsze
     * powiększenie obszaru ograniczonego wysokością jest o jeden poziom niższe niż na równiku: 512 dp zamiast
     * 256 dp wysokości przy powiększeniu 14 wypełnia 1024 dp przy powiększeniu 15, a nie 16.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun computesMinZoomAt60Degrees() {
        val tall = CampusBounds(west = 17.0, south = 60.0 - span / 4, east = 17.0 + span, north = 60.0 + span / 4)
        assertEquals(15.0, MapLimits(tall, widthDp = 512.0, heightDp = 1024.0).minZoom, ZOOM_DELTA)
    }

    /**
     * Wiersz: V1, prostokąt widoku to sam prostokąt terenu, bez rozszerzania do proporcji mapy; podkład jest tylko
     * w nim. Przy najmniejszym powiększeniu pas szeroki na `span` i wysoki na `span / 4` wypełnia mapę 400 × 800 dp
     * w pionie, więc w pionie mapa stoi, a w poziomie da się ją przesuwać.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
     *
     * Wymagania: US1.7, NFR-39
     */
    @Test
    fun keepsAreaWithoutWidening() {
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val limits = MapLimits(wide, widthDp = 400.0, heightDp = 800.0)
        assertEquals(wide, limits.area, "prostokąt terenu bez rozszerzania")
        assertEquals(wide, limits.tileBounds, "kafelki tylko w prostokącie terenu")
        val center = limits.centerBounds(zoom = limits.minZoom)
        assertEquals(center.south, center.north, MERCATOR_DELTA, "w pionie prostokąt wypełnia mapę")
        assertEquals(true, center.east - center.west > span / 2, "w poziomie mapa się przesuwa")
    }

    /**
     * Wiersz: V1, cały widok mapy zostaje w prostokącie terenu: środek leży najwyżej pół widoku od brzegu prostokąta,
     * więc brzeg mapy staje na brzegu prostokąta. Przy najmniejszym powiększeniu środek to środek prostokąta.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Mapa nie wychodzi poza wybrany teren
     *
     * Wymagania: US1.7, NFR-39, NFR-53
     */
    @Test
    fun keepsWholeViewInArea() {
        val square = CampusBounds(west = 0.0, south = -span / 2, east = span, north = span / 2)
        val limits = MapLimits(square, widthDp = 512.0, heightDp = 512.0)
        val far = MapCamera(center = GeoPoint(longitude = 1.0, latitude = 1.0), zoom = 15.0)
        val corner = limits.clamp(far)
        assertEquals(span * 3 / 4, corner.center.longitude, DEGREE_DELTA, "brzeg widoku na wschodnim brzegu")
        assertEquals(span / 4, corner.center.latitude, MERCATOR_DELTA, "brzeg widoku na północnym brzegu")
        val widest = limits.clamp(far.copy(zoom = 3.0))
        assertEquals(14.0, widest.zoom, ZOOM_DELTA, "najmniejsze powiększenie: prostokąt wypełnia widok")
        assertEquals(span / 2, widest.center.longitude, DEGREE_DELTA, "środek prostokąta")
        val small = limits.clamp(far, viewWidthDp = 256.0, viewHeightDp = 256.0)
        assertEquals(span * 7 / 8, small.center.longitude, DEGREE_DELTA, "mniejsza odkryta część sięga dalej")
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
        assertEquals(limits.tileBounds, limits.basemap.bounds, "kafelki podkładu w prostokącie widoku")
    }

    private companion object {
        const val ZOOM_DELTA = 1e-3
        const val DEGREE_DELTA = 1e-9
        const val MERCATOR_DELTA = 1e-6
    }
}
