/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Dwa poziomy budynków na mapie V1: syntetyczny katalog z budynkiem szczegółowym A-1 (ma plan pięter)
 * i podstawowym A-2 (bez planu), nie dane PWr.
 *
 * Wymagania: US16.1, US16.2, BR-G1, BR-G3, NFR-21
 */
class BuildingLevelsStyleTest {
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
            basicBuilding = "#DDECF1",
            buildingOutline = "#004E5E",
            selectedOutline = "#001F27",
            dimmed = "#D6DBDD",
            dimmedOutline = "#C2C9CC",
        )
    private val data = campusMapData(catalog)
    private val level = """["==",["get","detailed"],true]"""
    private val state = """["boolean",["feature-state","$DIMMED_STATE"],false]"""

    @Test
    fun takesLevelFromCatalog() {
        val features = data.buildings.jsonObject.getValue("features")
        val levels =
            features.jsonArray.associate { feature ->
                val properties = feature.jsonObject.getValue("properties").jsonObject
                properties.getValue("label").jsonPrimitive.content to properties.getValue("detailed").toString()
            }
        assertEquals(mapOf("A-1" to "true", "A-2" to "false"), levels, "Poziom budynku nie wynika z katalogu")
    }

    @Test
    fun fillsByLevel() {
        val fill = style().layerById(BUILDINGS_LAYER).paintText(BUILDINGS_FILL)
        assertEquals(
            """["case",$state,"#D6DBDD",["case",$level,"#BFE0EC","#DDECF1"]]""",
            fill,
            "Budynek podstawowy nie ma bladego tła",
        )
    }

    @Test
    fun drawsThinOutlineOfBasic() {
        val width = style().layerById(OUTLINES_LAYER).paintText("line-width")
        assertEquals(
            """["case",$level,$DETAILED_OUTLINE_DP,$BASIC_OUTLINE_DP]""",
            width,
            "Grubość konturu nie zależy od poziomu budynku",
        )
        assertTrue(BASIC_OUTLINE_DP < DETAILED_OUTLINE_DP, "Kontur budynku podstawowego nie jest cieńszy")
    }

    @Test
    fun dimsNeighboursOfBothLevels() {
        val fill = style(building = "A-2").layerById(BUILDINGS_LAYER).paintText(BUILDINGS_FILL)
        assertEquals(
            """["case",$state,"#D6DBDD",["case",$level,"#BFE0EC","#DDECF1"]]""",
            fill,
            "Przygaszenie sąsiadów nie obejmuje obu poziomów albo budynki straciły poziom",
        )
        val dimmed = selectionLook(data, data.selection(building = "A-2")).dimmed
        assertEquals(listOf(true, false), dimmed, "Przy zaznaczonym budynku podstawowym A-1 nie jest przygaszony")
    }

    private fun style(building: String? = null): JsonObject =
        Json.parseToJsonElement(campusMapStyle(data, data.selection(building = building), colors)).jsonObject
}
