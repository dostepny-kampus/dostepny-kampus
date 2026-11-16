/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.plan.ShownPlan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Budynek pod widocznym planem piętra na mapie V1: syntetyczny katalog z budynkami A-1 (ma plan) i A-2, nie dane
 * PWr.
 *
 * Wymagania: US16.1, US2.8, NFR-21
 */
class BuildingUnderPlanStyleTest {
    private val catalog =
        BuildingCatalog(
            buildings = listOf(shiftedBuilding("A-1"), shiftedBuilding("A-2")),
            entrances = emptyList(),
            attribution = "",
            detailed = setOf("A-1"),
        )
    private val colors =
        MapStyleColors(
            land = "#E8EDEE",
            building = "#BFE0EC",
            buildingOutline = "#004E5E",
            selectedOutline = "#001F27",
            dimmed = "#D6DBDD",
            dimmedOutline = "#C2C9CC",
        )
    private val data = campusMapData(catalog)
    private val underPlan = """["case",["boolean",["feature-state","$UNDER_PLAN_STATE"],false],0.0,1.0]"""

    /** Tło i kontur budynku z OpenStreetMap znikają pod jego planem; inne budynki zostają. */
    @Test
    fun hidesBuildingUnderPlan() {
        assertEquals(listOf(true, false), look(building = null).underPlan, "Pod planem nie zniknął tylko A-1")
        assertEquals(listOf(false, false), selectionLook(data, data.selection()).underPlan, "Bez planu coś zniknęło")
        val style = style(building = null)
        val fill = style.layerById(BUILDINGS_LAYER).paintText("fill-opacity")
        assertEquals(underPlan, fill, "Tło budynku nie znika pod planem")
        assertEquals(underPlan, style.layerById(OUTLINES_LAYER).paintText("line-opacity"), "Kontur nie znika")
    }

    /** Przy widocznym planie pozostałe budynki są przygaszone tym samym stanem co przy wybranym budynku. */
    @Test
    fun dimsNeighboursOfPlan() {
        assertEquals(listOf(false, true), look(building = null).dimmed, "Sąsiad planu nie jest przygaszony")
        assertEquals(listOf(false, false), look(building = "A-2").dimmed, "Wybrany budynek obok planu przygaszony")
    }

    /** Symbol budynku z widocznym planem znika, symbole innych budynków zostają. */
    @Test
    fun hidesLabelOfPlanBuilding() {
        val filter = look(building = null).labelsFilter
        assertFalse(passes(filter, label("A-1")), "Symbol A-1 leży na jego planie")
        assertTrue(passes(filter, label("A-2")), "Symbol sąsiada zniknął")
        val all = selectionLook(data, data.selection()).labelsFilter
        assertTrue(passes(all, label("A-1")), "Bez planu symbol zniknął")
        val layer = style(building = null).layerById(LABELS_LAYER)
        assertEquals(filter, layer.getValue("filter"), "Warstwa symboli nie ma filtra planu")
    }

    /** Obrys wybranego budynku z widocznym planem idzie po ścianie piętra, a nie po konturze z OpenStreetMap. */
    @Test
    fun outlinesSelectedAlongWall() {
        val look = look(building = "A-1")
        assertEquals("""["!",true]""", look.selectedFilter.toString(), "Obrys wybranego idzie po konturze OSM")
        assertEquals(planFilter(floor = "P0", kind = "outline"), look.selectedPlanFilter, "Obrys nie idzie po ścianie")
        val other = look(building = "A-2")
        assertEquals("""["!",true]""", other.selectedPlanFilter.toString(), "Ściana planu ma obrys innego budynku")
        val layer = style(building = "A-1").layerById(SELECTED_PLAN_LAYER)
        assertEquals(PLAN_SOURCE, layer.getValue("source").jsonPrimitive.content, "Obrys nie leży na danych planu")
        assertEquals("\"#001F27\"", layer.paintText("line-color"), "Obrys po ścianie nie ma barwy wybranego budynku")
        assertEquals(look.selectedPlanFilter, layer.getValue("filter"), "Warstwa obrysu po ścianie bez filtra")
    }

    private fun look(building: String?): SelectionLook =
        selectionLook(data, data.selection(building = building).copy(plan = plan()))

    private fun style(building: String?): JsonObject {
        val selection = data.selection(building = building).copy(plan = plan())
        return Json.parseToJsonElement(campusMapStyle(data, selection, colors)).jsonObject
    }

    private fun label(text: String): JsonObject = buildJsonObject { put(LABEL, JsonPrimitive(text)) }

    private fun plan(): ShownPlan {
        val floor = RoomList(source = "", floorOutline = shiftedBuilding("A-1").outline, rooms = emptyList())
        return ShownPlan(building = "A-1", floors = emptyList(), floor = "P0", rooms = mapOf("P0" to floor))
    }
}
