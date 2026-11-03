/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Mapowanie JSON kontraktu budynków na modele, bez sieci. */
class BuildingListTest {
    private val json = Json

    @Test
    fun us1_list() {
        val list = json.decodeFromString<BuildingListDto>(LIST).toModel()
        assertEquals("© OpenStreetMap contributors", list.source)
        assertEquals(listOf(c13(DetailLevel.DETAILED)), list.buildings)
    }

    @Test
    fun us1_server_symbol_no_name() {
        val listed = json.decodeFromString<BuildingListDto>(LIST.replace(oldValue = NAME, newValue = "C-13"))
        val building = listed.toModel().buildings.single()
        assertEquals(null, building.building.name)
        val card = json.decodeFromString<BuildingDetailsDto>(DETAILS.replace(oldValue = NAME, newValue = "C-3/C-4"))
        assertEquals(null, card.toModel().building.name)
    }

    @Test
    fun us2_building_with_entrance() {
        val card = json.decodeFromString<BuildingDetailsDto>(DETAILS).toModel()
        assertEquals(c13(DetailLevel.BASIC).building, card.building)
        assertEquals(DetailLevel.BASIC, card.detailLevel)
        assertEquals(
            Entrance(
                id = "c13-main",
                buildingSymbol = "C-13",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                marking = EntranceMarking.ACCESSIBLE,
            ),
            card.entrances.single(),
        )
    }

    @Test
    fun us17_1_entrance_without_name() {
        val dto =
            json.decodeFromString<EntranceDto>(
                """{"id":"node/1","buildingSymbol":"C-13","type":"emergency","main":false,""" +
                    """"location":{"type":"Point","coordinates":[17,51]},"marking":"NO_DATA",""" +
                    """"markingDate":"2026-11-03","markingSource":"IMPORT"}""",
            )
        val entrance = dto.toModel()
        assertEquals(null, entrance.name)
        assertEquals("emergency", entrance.type)
    }

    @Test
    fun us6_2_locate() {
        val inside = json.decodeFromString<LocateResultDto>("""{"buildingSymbol":"C-13","insideOutline":true}""")
        assertEquals(BuildingAtPoint(buildingSymbol = "C-13", isInsideOutline = true), inside.toModel())
        val outside = json.decodeFromString<LocateResultDto>("""{"insideOutline":false}""")
        assertEquals(BuildingAtPoint(buildingSymbol = null, isInsideOutline = false), outside.toModel())
    }

    @Test
    fun bad_entrance_rejected() {
        assertThrows<IllegalArgumentException> { entrance(date = "wczoraj").toModel() }
        assertThrows<IllegalArgumentException> { entrance(marking = "MAYBE").toModel() }
        assertThrows<IllegalArgumentException> { entrance(pointType = "Line", coordinates = listOf(1.0)).toModel() }
    }

    private fun c13(level: DetailLevel): ApiBuilding =
        ApiBuilding(
            building = Building(symbol = "C-13", name = "Budynek C-13", outline = square()),
            detailLevel = level,
        )

    private fun entrance(
        date: String = "2026-10-01",
        marking: String = "ACCESSIBLE",
        pointType: String = "Point",
        coordinates: List<Double> = listOf(17.0, 51.0),
    ): EntranceDto =
        EntranceDto(
            id = "c13-main",
            buildingSymbol = "C-13",
            name = "Wejście główne",
            isMain = true,
            location = GeoPointDto(type = pointType, coordinates = coordinates),
            marking = marking,
            markingDate = date,
            markingSource = MarkingSource.IMPORT,
        )

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
        const val OUTLINE =
            """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""
        const val LIST =
            """{"source":"© OpenStreetMap contributors","buildings":[""" +
                """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"DETAILED","outline":$OUTLINE}]}"""
        const val DETAILS =
            """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"BASIC","outline":$OUTLINE,""" +
                """"entrances":[{"id":"c13-main","buildingSymbol":"C-13","name":"Wejście główne","main":true,""" +
                """"location":{"type":"Point","coordinates":[17,51]},"marking":"ACCESSIBLE",""" +
                """"markingDate":"2026-10-01","markingSource":"IMPORT"}]}"""
    }
}
