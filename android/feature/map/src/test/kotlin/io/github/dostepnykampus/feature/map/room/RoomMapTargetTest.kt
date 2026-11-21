/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.selectedBuilding
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.ENTRANCE
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.GROUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.SECOND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.lift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Piętro i punkt dla kamery po otwarciu karty lub wyborze kroku. Wymagania: US18.1, US18.2 */
class RoomMapTargetTest {
    @Test
    fun showsRoomBeforeChoosingStep() {
        val card = RoomCard(found = FOUND, way = null)
        val expected = RoomMapTarget(floor = SECOND.id, point = FOUND.room.labelPoint)
        assertEquals(expected, card.mapTarget(), "Sala na planie")
    }

    @Test
    fun followsEntranceLiftAndRoom() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT)
        val targets = (0 until card.stepCount).map { card.chosen(it).mapTarget() }
        val expected =
            listOf(
                RoomMapTarget(floor = GROUND.id, point = ENTRANCE.location),
                RoomMapTarget(floor = GROUND.id, point = lift(GROUND.id).labelPoint),
                RoomMapTarget(floor = SECOND.id, point = FOUND.room.labelPoint),
            )
        assertEquals(expected, targets, "Punkty i piętra kroków drogi")
    }

    /** Krok staje na środku części mapy nad kartą przez dopełnienie kamery, bez przesuwania środka. */
    @Test
    fun putsStepPointInCameraCenter() {
        val room = OpenRoom(card = RoomCard(found = FOUND, way = VIA_LIFT, step = 1), onClose = {}, onStep = {})
        val lift = lift(GROUND.id).labelPoint
        val centers =
            listOf(
                room.cameraForRoom(isRoomOnMap = true, cover = 320.dp)?.center,
                room.cameraForRoom(isRoomOnMap = true, cover = 0.dp)?.center,
                room.cameraForRoom(isRoomOnMap = false, cover = 0.dp)?.center,
            )
        assertEquals(listOf(lift, null, lift), centers, "Kamera kroku przesunięta albo bez czekania na pomiar karty")
    }

    /** Wiersz: dotknięcie sali na planie nie rusza kamery, a wybrany potem krok już tak. */
    @Test
    fun keepsCameraForTappedRoom() {
        val open = OpenRoom(card = RoomCard(found = FOUND, way = VIA_LIFT), onClose = {}, onStep = {})
        val step = open.copy(card = open.card.copy(step = 1))

        assertEquals(
            listOf(null, lift(GROUND.id).labelPoint),
            listOf(
                open.cameraForRoom(isRoomOnMap = true, cover = 320.dp, isTapped = true)?.center,
                step.cameraForRoom(isRoomOnMap = true, cover = 320.dp)?.center,
            ),
            "Dotknięta sala ruszyła kamerę albo krok jej nie ruszył",
        )
    }

    /** Wiersz: plan budynku sali widoczny przed czynnością zostawia powiększenie, bez niego kamera daje 19. */
    @Test
    fun keepsZoomWithRoomPlan() {
        val open = OpenRoom(card = RoomCard(found = FOUND, way = null), onClose = {}, onStep = {})
        val near = MapCamera(center = FOUND.room.labelPoint, zoom = 18.0)
        val zooms =
            listOf(
                SeenPlan(camera = near, plan = FOUND.building),
                SeenPlan(camera = near, plan = "C-13"),
                SeenPlan(camera = near.copy(zoom = 16.0), plan = FOUND.building),
                null,
            ).map { open.cameraForRoom(isRoomOnMap = true, cover = 320.dp, seen = it)?.zoom }

        assertEquals(listOf(18.0, 19.0, 19.0, 19.0), zooms, "Powiększenie po czynności")
    }

    /** Wiersz: karta 200 dp i pasek 56 dp przesuwają środek dopełnienia o 128 dp, więc obraz mapy zostaje w miejscu. */
    @Test
    fun keepsPictureUnderCardAndBar() {
        val view = MapCamera(center = FOUND.room.labelPoint, zoom = 18.0)

        assertEquals(view.shiftedNorth(128.0), view.stillUnder(card = 200.dp, bar = 56.dp), "Obraz mapy przesunięty")
    }

    /** Wiersz: przed pomiarem z drogą kamera czeka; pierwsza wysokość z drogą zostaje na czynność. */
    @Test
    fun holdsFirstWayCover() {
        val held = listOf(0.heldWith(cover = 0), 0.heldWith(cover = 600), 600.heldWith(cover = 400))

        assertEquals(listOf(0, 600, 600), held, "Wysokość karty kamery")
    }

    @Test
    fun pinsOnlyExplicitRoomFloor() {
        val room = MapFocus.Building(symbol = FOUND.building, command = 1L, room = FOUND.room.id)
        val floor = room.copy(floor = FOUND.floor.id)
        assertEquals(
            listOf(null, FOUND.building),
            listOf(selectedBuilding(room), selectedBuilding(floor)),
            "Tylko sala z piętrem trzyma plan",
        )
    }

    @Test
    fun showsNumbersForCurrentFloor() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT, step = 1)
        val ground = card.markersOn(GROUND.id)
        val second = card.markersOn(SECOND.id)

        assertEquals(listOf(0 to false, 1 to true), ground.map { it.index to it.isSelected }, "Wejście i wybrana winda")
        assertEquals(listOf(2 to false), second.map { it.index to it.isSelected }, "Sala na drugim piętrze")
        assertEquals(listOf(false, false), ground.map { it.isDestination }, "Początek drogi bez przesunięcia")
        assertEquals(true, second.single().isDestination, "Znacznik sali zostawia miejsce na numer")
    }
}
