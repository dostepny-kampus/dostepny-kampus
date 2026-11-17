/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.style.shiftedBuilding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Zaznaczenie budynku na mapie V1 z polecenia trasy: syntetyczny katalog z budynkami A-1 (ma plan) i A-2.
 *
 * Wymagania: US16.1, US2.9
 */
class FocusSelectionTest {
    private val data =
        campusMapData(
            BuildingCatalog(
                buildings = listOf(shiftedBuilding("A-1"), shiftedBuilding("A-2")),
                entrances = emptyList(),
                attribution = "",
                detailed = setOf("A-1"),
            ),
        )

    /**
     * Wiersz: V1, po „Pokaż na planie” karta jest zamknięta i żaden budynek nie jest zaznaczony, a teren budynku
     *   zostaje granicą mapy.
     */
    @Test
    fun leavesNoSelectionAfterShowPlan() {
        val focus = MapFocus.Building(symbol = "A-1", command = 1L, floor = "P0")

        val selection = data.focusSelection(focus = focus, chosen = null, area = null)

        assertNull(selection.building, "Po „Pokaż na planie” budynek został zaznaczony")
        assertEquals(data.selection(building = "A-1").bounds, selection.bounds, "Granica mapy nie jest terenem A-1")
    }

    /** Wiersz: V2, „Pokaż budynek na mapie” zostawia V1 z zaznaczonym budynkiem, bez karty. */
    @Test
    fun keepsSelectionAfterShowOnMap() {
        val focus = MapFocus.Building(symbol = "A-1", command = 1L)

        val selection = data.focusSelection(focus = focus, chosen = null, area = null)

        assertEquals("A-1", selection.building, "„Pokaż budynek na mapie” nie zaznaczył budynku")
    }
}
