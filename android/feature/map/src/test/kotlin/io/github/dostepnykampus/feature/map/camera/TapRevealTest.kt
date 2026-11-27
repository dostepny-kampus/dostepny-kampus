/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.pow

private const val ZOOM = 18.0
private const val DEGREES = 1e-9
private const val DP = 0.5
private const val MARGIN_DP = 16.0
private val free = FreeMap(widthDp = 411.0, heightDp = 500.0, endDp = 0.0, marginDp = 24.0)
private val cardOpen = TapSight(belowDp = 0.0, topDp = 0.0, bottomDp = 800.0)
private val square = CampusBounds(west = 17.0598, south = 51.1099, east = 17.0602, north = 51.1101)
private val tall = CampusBounds(west = 17.0598, south = 51.1096, east = 17.0602, north = 51.1104)
private val middle = GeoPoint(longitude = 17.06, latitude = 51.11)

/**
 * Ruch mapy po dotknięciu konturu: powiększenie zostaje, a mapa przesuwa się w górę tylko wtedy, gdy kontur był
 * cały widoczny, a karta zasłoniłaby jego część.
 *
 * Wymagania: US2.8, US3.2, NFR-56
 */
class TapRevealTest {
    /** Wiersz: V1, kontur cały widoczny nad kartą → kamera bez ruchu, karta w 300 ms. */
    @Test
    fun keepsVisibleOutline() {
        val from = MapCamera(center = middle, zoom = ZOOM)
        val move = tap(from)
        assertEquals(CameraMotion.NONE, move.motion, "widoczny kontur: mapa stoi")
        assertEquals(300L, move.durationMillis, "karta wjeżdża w 300 ms")
        assertEquals(from, move.camera, "widok bez zmian")
    }

    /** Wiersz: V1, kontur cały widoczny, a karta zasłania jego dół → przesunięcie w górę do 16 dp nad kartą. */
    @Test
    fun liftsCoveredOutline() {
        val from = MapCamera(center = middle, zoom = ZOOM).north(dp = 260.0)
        val move = tap(from)
        assertEquals(CameraMotion.EASE, move.motion, "zasłonięty kontur: przesunięcie")
        assertEquals(300L, move.durationMillis, "przesunięcie w 300 ms")
        assertEquals(ZOOM, move.camera.zoom, "powiększenie zostaje")
        assertEquals(from.center.longitude, move.camera.center.longitude, DEGREES, "bez ruchu w poziomie")
        assertTrue(move.camera.center.latitude < from.center.latitude, "mapa jedzie w górę: ${move.camera}")
        val bottom = edgeDp(camera = move.camera, latitude = square.south)
        assertEquals(free.heightDp - MARGIN_DP, bottom, DP, "dolny brzeg 16 dp nad kartą")
    }

    /** Wiersz: V1, ponowne dotknięcie tego samego budynku → mapa stoi. */
    @Test
    fun keepsRepeatedTap() {
        val lifted = tap(MapCamera(center = middle, zoom = ZOOM).north(dp = 260.0)).camera
        val again = tap(lifted)
        assertEquals(CameraMotion.NONE, again.motion, "drugie dotknięcie: mapa stoi")
        assertEquals(lifted, again.camera, "widok bez zmian")
    }

    /** Wiersz: V1, kontur ucięty brzegiem ekranu z boku, u góry albo u dołu → mapa stoi. */
    @Test
    fun keepsCroppedOutline() {
        val start = MapCamera(center = middle, zoom = ZOOM)
        val cropped =
            listOf(
                start.copy(center = middle.copy(longitude = middle.longitude - 0.0006)),
                start.north(dp = -200.0),
                start.north(dp = 560.0),
            )
        cropped.forEach { from ->
            val move = tap(from)
            assertEquals(CameraMotion.NONE, move.motion, "ucięty kontur: mapa stoi, $from")
            assertEquals(from, move.camera, "widok bez zmian, $from")
        }
    }

    /** Wiersz: V1, przed kartą górę konturu zasłaniał pasek z polem wyszukiwania → kontur ucięty, mapa stoi. */
    @Test
    fun keepsOutlineUnderBar() {
        val from = MapCamera(center = middle, zoom = ZOOM).north(dp = 200.0)
        val sight = TapSight(belowDp = 0.0, topDp = 420.0, bottomDp = 800.0)
        assertEquals(CameraMotion.NONE, tap(from, sight = sight).motion, "kontur pod paskiem: mapa stoi")
    }

    /** Wiersz: V1, mapa przybliżona do wnętrza budynku → mapa stoi, powiększenie zostaje. */
    @Test
    fun keepsZoomInsideBuilding() {
        val from = MapCamera(center = middle, zoom = 21.0)
        val move = tap(from)
        assertEquals(CameraMotion.NONE, move.motion, "wnętrze budynku: mapa stoi")
        assertEquals(21.0, move.camera.zoom, "powiększenie zostaje")
    }

    /** Wiersz: V1, budynek wyższy niż wolna część mapy → przesunięcie najwyżej do 16 dp pod górą wolnej mapy. */
    @Test
    fun stopsTallOutlineAtTop() {
        val centered = MapCamera(center = middle, zoom = ZOOM)
        val top = edgeDp(camera = centered, latitude = tall.north)
        val near = centered.north(dp = 10.0 - top)
        assertEquals(CameraMotion.NONE, tap(near, outline = tall).motion, "góra konturu przy górze mapy: mapa stoi")
        val lower = centered.north(dp = 100.0 - top)
        val move = tap(lower, outline = tall)
        assertEquals(CameraMotion.EASE, move.motion, "przesunięcie ograniczone górą konturu")
        assertEquals(MARGIN_DP, edgeDp(camera = move.camera, latitude = tall.north), DP, "góra 16 dp pod górą mapy")
    }

    /** Wiersz: V1, karta zasłoniła środek widoku spod paska → widok liczony dla wolnej części, ekran nie skacze. */
    @Test
    fun keepsScreenUnderBar() {
        val from = MapCamera(center = middle, zoom = ZOOM)
        val move = tap(from, sight = TapSight(belowDp = 100.0, topDp = 50.0, bottomDp = 800.0))
        assertEquals(CameraMotion.NONE, move.motion, "kontur dalej widoczny: mapa stoi")
        val expected = from.north(dp = 100.0).center.latitude
        assertEquals(expected, move.camera.center.latitude, DEGREES, "środek wyżej o 100 dp")
    }

    /** Wiersz: „Usuń animacje” → zasłonięty kontur odsłonięty od razu, bez czasu. */
    @Test
    fun jumpsWithoutAnimations() {
        val from = MapCamera(center = middle, zoom = ZOOM).north(dp = 260.0)
        val move = tap(from, scale = 0f)
        assertEquals(CameraMotion.NONE, move.motion, "bez animacji: od razu")
        assertEquals(0L, move.durationMillis, "bez animacji: 0 ms")
        assertEquals(tap(from).camera, move.camera, "ten sam stan końcowy")
    }

    private fun tap(
        from: MapCamera,
        outline: CampusBounds = square,
        sight: TapSight = cardOpen,
        scale: Float = 1f,
    ): CameraTransition =
        tapTransition(from = from, outline = outline, free = free, sight = sight, animationScale = scale)

    private fun edgeDp(camera: MapCamera, latitude: Double): Double =
        free.heightDp / 2 + (mercatorY(camera.center.latitude) - mercatorY(latitude)) * perRadian(camera.zoom)

    private fun perRadian(zoom: Double): Double = 512.0 * 2.0.pow(zoom) / (2 * PI)

    private fun MapCamera.north(dp: Double): MapCamera =
        copy(center = center.copy(latitude = latitudeOf(mercatorY(center.latitude) + dp / perRadian(zoom))))
}
