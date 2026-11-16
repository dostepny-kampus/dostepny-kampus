/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Sterownik kamery na udawanej mapie i prostokątach testowych (nie dane PWr). */
class CameraControllerTest {
    private val start = view(longitude = 17.0)
    private val near = view(longitude = 17.001)
    private val far = view(longitude = 17.03)
    private val middle = view(longitude = 17.015)
    private val main = limitsOf(west = 16.99)
    private val other = limitsOf(west = 17.02)
    private val bar = MapInset(top = 506.0)
    private val card = MapInset(bottom = 1160.0)
    private val map = FakeCamera(start)
    private val controller = CameraController(map)

    /**
     * Wiersz: V1, przelot na inny teren nie jest zatrzymywany przez granice: start zdejmuje granice, granice nowego
     * terenu z czasu przelotu idą do mapy dopiero po jego końcu.
     *
     * Wymagania: US1.8, BR-G5
     */
    @Test
    fun keepsLimitsUntilRest() {
        controller.limit(main)
        controller.move(flight(far), card)
        controller.limit(other)
        assertEquals(listOf("granice $main", "zdjęte", "FLIGHT do $far"), map.calls, "w przelocie bez granic")
        map.end(reached = true)
        assertEquals("granice $other", map.calls.last(), "po przelocie granice nowego terenu")
        assertEquals(CameraMoveState.Idle, controller.state, "kamera stoi")
    }

    /**
     * Wiersz: V2, nowy budynek w trakcie przelotu: wygrywa nowy cel, koniec starego ruchu niczego nie zmienia.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun letsLatestGoalWin() {
        controller.move(flight(far), card)
        controller.move(ease(near), card)
        map.endFirst(reached = false)
        assertEquals(2, (controller.state as CameraMoveState.Moving).number, "trwa ruch do nowego celu")
        map.end(reached = true)
        assertEquals(near, map.camera, "kamera w nowym celu")
        assertEquals(CameraMoveState.Idle, controller.state, "kamera stoi")
    }

    /**
     * Wiersz: V1, przelot przerwany bez udziału osoby (skok kamery w MapLibre zgłasza się jak koniec) dochodzi do
     * celu jednym przesunięciem.
     *
     * Wymagania: US1.8, NFR-56
     */
    @Test
    fun resumesCutMove() {
        controller.move(flight(far), card)
        map.end(reached = false)
        assertEquals("EASE do $far", map.calls.last(), "przesunięcie do celu")
        map.end(reached = true)
        assertEquals(far, map.camera, "kamera w celu")
        assertEquals(CameraMoveState.Idle, controller.state, "kamera stoi")
    }

    /** Wiersz: V1, gest osoby przerywa ruch, a mapa zostaje tam, gdzie osoba ją zatrzymała. Wymagania: NFR-56 */
    @Test
    fun leavesMapToPerson() {
        controller.move(flight(far), card)
        controller.byPerson()
        map.end(reached = false)
        assertEquals(middle, map.camera, "mapa stoi w miejscu gestu")
        assertEquals(CameraMoveState.Idle, controller.state, "bez ruchu do celu")
    }

    /**
     * Wiersz: V2, dopełnienie zmienione w trakcie ruchu nie przerywa go; dosuwa się po końcu, poza wywołaniem
     * MapLibre.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun padsAfterMove() {
        controller.move(ease(near), bar)
        controller.pad(card)
        assertEquals("EASE do $near", map.calls.last(), "bez drugiego ruchu w trakcie")
        map.endWithoutPost()
        assertEquals("EASE do $near", map.calls.last(), "nic w wywołaniu końca")
        map.runPosted()
        assertEquals("EASE do $near z $card", map.calls.last(), "dopełnienie po końcu ruchu")
    }

    /** Wiersz: V1, kamera już w celu z tym samym dopełnieniem nie rusza. Wymagania: NFR-56 */
    @Test
    fun staysAtGoal() {
        controller.pad(card)
        map.end(reached = true)
        val calls = map.calls.size
        controller.move(ease(start), card)
        assertEquals(calls, map.calls.size, "bez ruchu")
    }

    private fun view(longitude: Double) =
        MapCamera(center = GeoPoint(longitude = longitude, latitude = 51.005), zoom = 17.0)

    private fun limitsOf(west: Double) =
        MapLimits(
            bounds = CampusBounds(west = west, south = 51.0, east = west + 0.02, north = 51.01),
            widthDp = 400.0,
            heightDp = 600.0,
        )

    private fun flight(to: MapCamera) =
        CameraTransition(camera = to, motion = CameraMotion.FLIGHT, durationMillis = 1000L)

    private fun ease(to: MapCamera) = CameraTransition(camera = to, motion = CameraMotion.EASE, durationMillis = 400L)

    private inner class FakeCamera(override var camera: MapCamera) : CameraPort {
        override var inset = MapInset()
        val calls = mutableListOf<String>()
        private val running = mutableListOf<Pair<MapCamera, () -> Unit>>()
        private val posted = mutableListOf<() -> Unit>()

        override fun lift() {
            calls += "zdjęte"
        }

        override fun move(camera: MapCamera, inset: MapInset, motion: CameraMotion, millis: Long, end: () -> Unit) {
            val padding = if (camera == this.camera) " z $inset" else ""
            calls += "$motion do $camera$padding"
            this.inset = inset
            running += camera to end
        }

        override fun rest(limits: MapLimits?) {
            calls += "granice ${limits ?: "brak"}"
        }

        override fun post(block: () -> Unit) {
            posted += block
        }

        fun end(reached: Boolean) {
            endLast(reached)
            runPosted()
        }

        fun endFirst(reached: Boolean) {
            val (target, end) = running.removeAt(0)
            camera = if (reached) target else middle
            end()
            runPosted()
        }

        fun endWithoutPost() = endLast(reached = true)

        fun runPosted() {
            while (posted.isNotEmpty()) posted.removeAt(0)()
        }

        private fun endLast(reached: Boolean) {
            val (target, end) = running.removeAt(running.lastIndex)
            camera = if (reached) target else middle
            end()
        }
    }
}
