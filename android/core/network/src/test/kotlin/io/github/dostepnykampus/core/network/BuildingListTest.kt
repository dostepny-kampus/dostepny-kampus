/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingAtPoint
import io.github.dostepnykampus.core.network.building.BuildingDetails
import io.github.dostepnykampus.core.network.building.DetailLevel
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import io.github.dostepnykampus.core.network.building.decodeLocateResult
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

/**
 * Mapowanie JSON kontraktu budynków na modele, bez sieci.
 *
 * Wymagania: US1, US2, US17.1, US6.2
 */
class BuildingListTest {
    private val json = Json

    @Test
    fun readsList() {
        val list = json.decodeBuildingList(BUILDING_LIST_JSON)
        assertEquals("© OpenStreetMap contributors", list.source, "źródło danych listy")
        assertEquals(listOf(c13(DetailLevel.DETAILED)), list.buildings, "budynki z listy")
    }

    @Test
    fun readsServerWithoutName() {
        val listed = json.decodeBuildingList(BUILDING_LIST_JSON.replace(oldValue = NAME_FIELD, newValue = ""))
        val listedAll = listed.buildings
        val listedBuilding = listedAll.single().building
        assertEquals(null, listedBuilding.name, "brak nazwy budynku na liście")
        val card = json.decodeBuildingDetails(BUILDING_DETAILS_JSON.replace(oldValue = NAME_FIELD, newValue = ""))
        val cardBuilding = card.building
        assertEquals(null, cardBuilding.name, "brak nazwy budynku na karcie")
    }

    @Test
    fun keepsServerSymbolName() {
        val listed = json.decodeBuildingList(BUILDING_LIST_JSON.replace(oldValue = NAME, newValue = "C-13"))
        val listedAll = listed.buildings
        val listedBuilding = listedAll.single().building
        assertEquals("C-13", listedBuilding.name, "nazwa-symbol na liście")
        val card = json.decodeBuildingDetails(BUILDING_DETAILS_JSON.replace(oldValue = NAME, newValue = "C-3/C-4"))
        val cardBuilding = card.building
        assertEquals("C-3/C-4", cardBuilding.name, "nazwa-symbol na karcie")
    }

    @Test
    fun readsBuildingWithEntrance() {
        val card = json.decodeBuildingDetails(BUILDING_DETAILS_JSON)
        assertEquals(c13(DetailLevel.BASIC).building, card.building, "budynek z karty")
        assertEquals(DetailLevel.BASIC, card.detailLevel, "poziom szczegółowości karty")
        assertEquals(
            Entrance(
                id = "c13-main",
                buildingSymbol = "C-13",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2026, 10, 1), AssessmentSource.IMPORT),
            ),
            card.entrances.single(),
            "wejście z karty",
        )
    }

    @Test
    fun readsEntranceWithoutName() {
        val entrance =
            card(
                """{"id":"node/1","buildingSymbol":"C-13","type":"emergency","main":false,""" +
                    """"location":{"type":"Point","coordinates":[17,51]},"marking":"NO_DATA",""" +
                    """"markingDate":"2026-11-03","markingSource":"IMPORT"}""",
            ).entrances.single()
        assertEquals(null, entrance.name, "brak nazwy wejścia")
        assertEquals("emergency", entrance.type, "typ wejścia")
    }

    @Test
    fun readsLocateResult() {
        val inside = json.decodeLocateResult("""{"buildingSymbol":"C-13","insideOutline":true}""")
        assertEquals(
            BuildingAtPoint(buildingSymbol = "C-13", isInsideOutline = true),
            inside,
            "budynek w punkcie wewnątrz obrysu",
        )
        val outside = json.decodeLocateResult("""{"insideOutline":false}""")
        assertEquals(
            BuildingAtPoint(buildingSymbol = null, isInsideOutline = false),
            outside,
            "brak budynku poza obrysem",
        )
    }

    @Test
    fun rejectsBadEntrance() {
        assertThrows<IllegalArgumentException> { card(entrance(date = "wczoraj")) }
        assertThrows<IllegalArgumentException> { card(entrance(marking = "MAYBE")) }
        assertThrows<IllegalArgumentException> { card(entrance(pointType = "Line", coordinates = "[1.0]")) }
    }

    private fun c13(level: DetailLevel): ApiBuilding =
        ApiBuilding(
            building = Building(symbol = "C-13", name = "Budynek C-13", outline = square()),
            detailLevel = level,
        )

    private fun card(entrance: String): BuildingDetails =
        json.decodeBuildingDetails(
            """{"symbol":"C-13","detailLevel":"BASIC","outline":$BUILDING_OUTLINE_JSON,"entrances":[$entrance]}""",
        )

    private fun entrance(
        date: String = "2026-10-01",
        marking: String = "ACCESSIBLE",
        pointType: String = "Point",
        coordinates: String = "[17.0,51.0]",
    ): String =
        """{"id":"c13-main","buildingSymbol":"C-13","name":"Wejście główne","main":true,""" +
            """"location":{"type":"$pointType","coordinates":$coordinates},"marking":"$marking",""" +
            """"markingDate":"$date","markingSource":"IMPORT"}"""

    private fun square(): GeoPolygon =
        GeoPolygon(
            rings =
                listOf(
                    listOf(17 to 51, 18 to 51, 18 to 52, 17 to 52, 17 to 51).map { (lon, lat) ->
                        GeoPoint(longitude = lon.toDouble(), latitude = lat.toDouble())
                    },
                ),
        )

    private companion object {
        const val NAME = "Budynek C-13"
        const val NAME_FIELD = """"name":"Budynek C-13","""
    }
}
