/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.cameraTransition
import io.github.dostepnykampus.feature.map.camera.fitted
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

private val free = FreeMap(widthDp = 411.0, heightDp = 500.0, endDp = 0.0, marginDp = 24.0)
private val outline = CampusBounds(west = 17.0598, south = 51.1099, east = 17.0602, north = 51.1101)
private val middle = GeoPoint(longitude = 17.06, latitude = 51.11)

/**
 * Ruch mapy do wybranego budynku na punkcie testowym, nie na danych PWr: rodzaj i czas zależą tylko od położenia
 * budynku względem widoku.
 *
 * Wymagania: US2.8, NFR-56, US1.8
 */
class CameraTransitionTest {
    /** Wiersz: V1, cały kontur widoczny — przesunięcie na środek w 300 ms bez zmiany powiększenia. */
    @Test
    fun shiftsVisibleCamera() {
        val move = toBuilding(from = MapCamera(center = viewByDp(-60.0), zoom = START_ZOOM))
        assertEquals(CameraMotion.EASE, move.motion, "widoczny budynek: przesunięcie")
        assertEquals(300L, move.durationMillis, "widoczny budynek: 300 ms")
        assertEquals(START_ZOOM, move.camera.zoom, "powiększenie bez zmian")
        assertEquals(middle.longitude, move.camera.center.longitude, DEGREES, "budynek na środku")
    }

    /** Wiersz: V1, środek budynku najwyżej szerokość wolnej mapy od środka widoku — przesunięcie w 400 ms. */
    @Test
    fun shiftsToNearTarget() {
        val move = toBuilding(from = MapCamera(center = viewByDp(-330.0), zoom = START_ZOOM))
        assertEquals(CameraMotion.EASE, move.motion, "budynek blisko: przesunięcie")
        assertEquals(400L, move.durationMillis, "budynek blisko: 400 ms")
    }

    /** Wiersz: V1, dalej na tym samym terenie — przelot, krótszy niż 1,5 s. */
    @Test
    fun fliesToFarTarget() {
        val move = toBuilding(from = MapCamera(center = viewByMeters(-1000.0), zoom = START_ZOOM))
        assertEquals(CameraMotion.FLIGHT, move.motion, "budynek 1 km dalej: przelot")
        assertTrue(move.durationMillis in 600L..1100L, "przelot 1 km: ${move.durationMillis} ms")
    }

    /** Wiersz: V1, inny teren 2 km dalej — przelot ok. 1 s. */
    @Test
    fun fliesTwoKilometres() {
        val move = toBuilding(from = MapCamera(center = viewByMeters(-2000.0), zoom = START_ZOOM))
        assertEquals(CameraMotion.FLIGHT, move.motion, "teren 2 km dalej: przelot")
        assertTrue(move.durationMillis in 900L..1200L, "przelot 2 km: ${move.durationMillis} ms")
    }

    /** Wiersz: V1, dalszy cel przyspiesza przelot, a nie zamienia go na skok — najwyżej 1,5 s. */
    @Test
    fun capsFlightDuration() {
        val move = toBuilding(from = MapCamera(center = viewByMeters(-7000.0), zoom = START_ZOOM))
        assertEquals(CameraMotion.FLIGHT, move.motion, "teren 7 km dalej: przelot, nie skok")
        assertEquals(1500L, move.durationMillis, "przelot najwyżej 1,5 s")
    }

    /** Wiersz: V1, powiększenie mniejsze niż 17 rośnie do 17, żeby budynek dało się rozróżnić. */
    @Test
    fun raisesZoom() {
        val move = toBuilding(from = MapCamera(center = viewByMeters(-500.0), zoom = 15.0))
        assertEquals(START_ZOOM, move.camera.zoom, "przybliżenie do 17")
    }

    /** Wiersz: V1, powiększenie wybrane przez osobę zostaje, gdy budynek się mieści. */
    @Test
    fun keepsZoom() {
        val move = toBuilding(from = MapCamera(center = viewByDp(-200.0), zoom = 19.0))
        assertEquals(19.0, move.camera.zoom, "powiększenie osoby zostaje")
    }

    /** Wiersz: V1, budynek, który się nie mieści, oddala mapę tylko tyle, żeby kontur zmieścił się w mapie. */
    @Test
    fun fitsZoomToBuilding() {
        val wide = CampusBounds(west = 17.058, south = 51.1095, east = 17.062, north = 51.1105)
        val from = MapCamera(center = middle, zoom = 19.0)
        val move = cameraTransition(from = from, outline = wide, free = free, animationScale = 1f)
        val width = (wide.east - wide.west) * dpPerDegree(move.camera.zoom)
        assertTrue(move.camera.zoom < START_ZOOM, "oddalenie poniżej 17 dla dużego konturu")
        assertEquals(free.widthDp - 2 * free.marginDp, width, DP, "kontur mieści się dokładnie w wolnej mapie")
    }

    /** Wiersz: V1, przy systemowym „Usuń animacje” mapa od razu pokazuje stan końcowy. */
    @Test
    fun skipsAnimationsWhenOff() {
        val from = MapCamera(center = viewByMeters(-2000.0), zoom = START_ZOOM)
        val still = toBuilding(from = from, scale = 0f)
        assertEquals(CameraMotion.NONE, still.motion, "bez animacji: bez ruchu")
        assertEquals(0L, still.durationMillis, "bez animacji: 0 ms")
        assertEquals(toBuilding(from = from).camera, still.camera, "ten sam widok końcowy")
    }

    /** Wiersz: V1, skala animacji systemu mnoży czas ruchu. */
    @Test
    fun halvesDurationWithScale() {
        val move = toBuilding(from = MapCamera(center = viewByDp(-330.0), zoom = START_ZOOM), scale = 0.5f)
        assertEquals(200L, move.durationMillis, "skala 0,5: połowa z 400 ms")
    }

    /** Wiersz: V1, budynek kończy na środku wolnej części mapy, na lewo od pasa przycisków. */
    @Test
    fun centersBetweenControls() {
        val controls = free.copy(endDp = 160.0)
        val from = MapCamera(center = middle, zoom = START_ZOOM)
        val move = cameraTransition(from = from, outline = outline, free = controls, animationScale = 1f)
        val shift = (move.camera.center.longitude - middle.longitude) * dpPerDegree(move.camera.zoom)
        assertEquals(80.0, shift, DP, "środek przesunięty o pół pasa przycisków")
        assertEquals(middle.latitude, move.camera.center.latitude, DEGREES, "środek w pionie")
    }

    /**
     * Wiersz: V1, po wyborze terenu wszystkie jego budynki mieszczą się z marginesem w części mapy, której nie
     * zasłaniają przyciski mapy.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Po wyborze terenu widać wszystkie jego budynki
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun landsOnAllBuildings() {
        val controls = free.copy(endDp = 160.0)
        val parts = CampusBounds(west = 17.05, south = 51.105, east = 17.07, north = 51.115)
        val view = fitted(outline = parts, free = controls, maxZoom = START_ZOOM)
        val width = (parts.east - parts.west) * dpPerDegree(view.zoom)
        assertEquals(controls.widthDp - controls.endDp - 2 * controls.marginDp, width, DP, "szerokość wolnej mapy")
        assertTrue(view.zoom < START_ZOOM, "teren większy niż widok: powiększenie ${view.zoom}")
        val shift = (view.center.longitude - (parts.west + parts.east) / 2) * dpPerDegree(view.zoom)
        assertEquals(80.0, shift, DP, "środek przesunięty o pół pasa przycisków")
    }

    /**
     * Wiersz: V1, mały teren nie przybliża mapy ponad powiększenie, przy którym symbole budynków są czytelne.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Po wyborze terenu widać wszystkie jego budynki
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun landsNoCloserThanStartZoom() {
        assertEquals(START_ZOOM, fitted(outline = outline, free = free, maxZoom = START_ZOOM).zoom)
    }

    private fun toBuilding(from: MapCamera, scale: Float = 1f): CameraTransition =
        cameraTransition(from = from, outline = outline, free = free, animationScale = scale)

    private fun viewByDp(east: Double): GeoPoint =
        middle.copy(longitude = middle.longitude + east / dpPerDegree(START_ZOOM))

    private fun viewByMeters(east: Double): GeoPoint =
        middle.copy(longitude = middle.longitude + east / METERS_PER_DEGREE_AT_51)

    private fun dpPerDegree(zoom: Double): Double = 512.0 * 2.0.pow(zoom) / 360.0

    private companion object {
        const val DEGREES = 1e-9
        const val DP = 1e-6
        const val METERS_PER_DEGREE_AT_51 = 69_960.0
    }
}
