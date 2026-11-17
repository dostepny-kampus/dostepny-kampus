/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.pow

private const val ZOOM = 18.0
private const val DEGREES = 1e-9
private val free = FreeMap(widthDp = 411.0, heightDp = 500.0, endDp = 0.0, marginDp = 24.0)
private val outline = CampusBounds(west = 17.0598, south = 51.1099, east = 17.0602, north = 51.1101)
private val middle = GeoPoint(longitude = 17.06, latitude = 51.11)

/**
 * Ruch mapy po dotknięciu konturu: mapa stoi, gdy kontur jest cały widoczny nad kartą, a zasłonięty kontur
 * odsłania najmniejsze przesunięcie w tym samym powiększeniu.
 *
 * Wymagania: US2.8, US3.2, NFR-56
 */
class TapRevealTest {
    /** Wiersz: V1, dotknięty kontur widoczny nad kartą → kamera bez ruchu, karta w 300 ms. */
    @Test
    fun keepsVisibleOutline() {
        val from = MapCamera(center = middle, zoom = ZOOM)
        val move = tap(from)
        assertEquals(CameraMotion.NONE, move.motion, "widoczny kontur: mapa stoi")
        assertEquals(300L, move.durationMillis, "karta wjeżdża w 300 ms")
        assertEquals(from, move.camera, "widok bez zmian")
    }

    /** Wiersz: V1, kontur pod kartą → przesunięcie w pionie o brakującą część, to samo powiększenie. */
    @Test
    fun liftsCoveredOutline() {
        val from = MapCamera(center = middle, zoom = ZOOM).north(dp = 260.0)
        val move = tap(from)
        assertEquals(CameraMotion.EASE, move.motion, "zasłonięty kontur: przesunięcie")
        assertEquals(ZOOM, move.camera.zoom, "powiększenie zostaje")
        assertEquals(from.center.longitude, move.camera.center.longitude, DEGREES, "bez ruchu w poziomie")
        assertTrue(move.camera.center.latitude < from.center.latitude, "mapa jedzie na południe: ${move.camera}")
        assertEquals(CameraMotion.NONE, tap(move.camera).motion, "po przesunięciu kontur cały widoczny")
        assertTrue(move.camera.center.latitude > middle.latitude, "przesunięcie najmniejsze, nie na środek")
    }

    /** Wiersz: V1, kontur za prawym brzegiem → przesunięcie w poziomie, szerokość geograficzna zostaje. */
    @Test
    fun revealsEdgeOutline() {
        val from = MapCamera(center = middle.copy(longitude = middle.longitude - 0.0012), zoom = ZOOM)
        val move = tap(from)
        assertEquals(CameraMotion.EASE, move.motion, "kontur za brzegiem: przesunięcie")
        assertTrue(move.camera.center.longitude > from.center.longitude, "mapa jedzie na wschód: ${move.camera}")
        assertEquals(from.center.latitude, move.camera.center.latitude, DEGREES, "bez ruchu w pionie")
        assertEquals(CameraMotion.NONE, tap(move.camera).motion, "po przesunięciu kontur cały widoczny")
    }

    /** Wiersz: V1, kontur większy niż wolna część → oddalenie do zmieszczenia. */
    @Test
    fun zoomsOutForLargeOutline() {
        val move = tap(MapCamera(center = middle, zoom = 21.0))
        assertTrue(move.camera.zoom < 21.0, "oddalenie: ${move.camera.zoom}")
    }

    /** Wiersz: V1, karta zasłoniła środek widoku spod paska → widok liczony dla wolnej części, ekran nie skacze. */
    @Test
    fun keepsScreenUnderBar() {
        val from = MapCamera(center = middle, zoom = ZOOM)
        val move = tap(from, belowDp = 100.0)
        assertEquals(CameraMotion.NONE, move.motion, "kontur dalej widoczny: mapa stoi")
        val expected = from.north(dp = 100.0).center.latitude
        assertEquals(expected, move.camera.center.latitude, DEGREES, "środek wyżej o 100 dp")
    }

    /** Wiersz: „Usuń animacje” → zasłonięty kontur odsłonięty od razu, bez czasu. */
    @Test
    fun jumpsWithoutAnimations() {
        val move = tap(MapCamera(center = middle, zoom = ZOOM).north(dp = 260.0), scale = 0f)
        assertEquals(CameraMotion.NONE, move.motion, "bez animacji: od razu")
        assertEquals(0L, move.durationMillis, "bez animacji: 0 ms")
    }

    private fun tap(from: MapCamera, belowDp: Double = 0.0, scale: Float = 1f): CameraTransition =
        tapTransition(from = from, outline = outline, free = free, belowDp = belowDp, animationScale = scale)

    private fun MapCamera.north(dp: Double): MapCamera {
        val perRadian = 512.0 * 2.0.pow(zoom) / (2 * PI)
        return copy(center = center.copy(latitude = latitudeOf(mercatorY(center.latitude) + dp / perRadian)))
    }
}
