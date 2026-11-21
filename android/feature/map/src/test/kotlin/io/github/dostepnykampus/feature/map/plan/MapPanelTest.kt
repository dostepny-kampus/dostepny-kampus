/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.feature.map.navigation.MapFocus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Budynek, którego plan trzyma karta albo arkusz nad mapą, i zasłonięty dół mapy. Wymagania: US16.1, US2.8 */
internal class MapPanelTest {
    @Test
    fun holdsBuildingOfOpenCard() {
        val card = MapFocus.Building(symbol = "A")
        val room = MapFocus.Building(symbol = "A", command = 1L, floor = "P1", room = "7")
        assertEquals(
            listOf("A", "A"),
            listOf(building(card), building(room)),
            "Karta budynku i karta sali nie trzymają planu swojego budynku",
        )
    }

    @Test
    fun leavesMapCenterForCommands() {
        val onMap = MapFocus.Building(symbol = "A", command = 1L)
        val onPlan = onMap.copy(floor = "P1")
        assertEquals(
            listOf(null, null, null),
            listOf(building(null), building(onMap), building(onPlan)),
            "„Pokaż na mapie” albo „Pokaż na planie” trzyma budynek",
        )
    }

    @Test
    fun holdsEntranceSheetBuilding() {
        val marked = MapFocus.Entrance(id = "e1", buildingSymbol = "B-8")
        val held =
            listOf(
                building(marked, entrance = "B-8"),
                building(marked),
                building(MapFocus.Building(symbol = "A"), entrance = "C"),
            )
        assertEquals(
            listOf("B-8", null, "C"),
            held,
            "Arkusz wejścia nie trzyma swojego budynku albo zamknięty arkusz dalej go trzyma",
        )
    }

    @Test
    fun coversMapWithEntranceSheet() {
        val panel = mapPanel(focus = null, sheetBuilding = "A", sheetCover = 640)
        assertEquals(MapPanel(building = "A", cover = 640), panel, "Arkusz wejścia nie zasłania dołu mapy")
    }

    @Test
    fun coversMapWithRoomCard() {
        val room = MapFocus.Building(symbol = "A", command = 1L, floor = "P1", room = "7")
        val panel = mapPanel(focus = room, sheetBuilding = null, sheetCover = 0, roomCover = 480)
        assertEquals(MapPanel(building = "A", cover = 480), panel, "Karta sali nie zasłania dołu mapy dopełnieniem")
    }

    @Test
    fun holdsBuildingOfPlanSignSheet() {
        val panel = mapPanel(focus = null, sheetBuilding = "A-1", sheetCover = 400, roomCover = 0)
        assertEquals(MapPanel(building = "A-1", cover = 400), panel, "Arkusz znaku planu nie trzyma planu budynku")
    }

    /** Arkusz znaku planu żyje tylko ze znakiem na pokazanym piętrze. Wymagania: US16.1, US2.8 */
    @Test
    fun showsSignOnlyOnShownFloor() {
        val shown =
            listOf(
                SIGN_PLAN.showsSign("winda"),
                SIGN_PLAN.showsSign("schody"),
                SIGN_PLAN.copy(floor = "f1").showsSign("schody"),
                null.showsSign("winda"),
            )
        assertEquals(listOf(true, false, true, false), shown, "Znak spoza pokazanego piętra albo bez planu")
    }

    private fun building(focus: MapFocus?, entrance: String? = null): String? =
        mapPanel(focus = focus, sheetBuilding = entrance, sheetCover = 0).building
}
