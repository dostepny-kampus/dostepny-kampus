/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.inspection.decodeProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

/**
 * Odczyt `BuildingProgress[]` z kontraktu: budynek szczegółowy z piętrami i znacznikami, budynek podstawowy ze
 * znacznikiem „obejrzany”.
 *
 * Wymagania: US11.1, US11.3, BR-21
 */
class ProgressJsonTest {
    private val json = NetworkModule.json()

    @Test
    fun us11_1_detailed_and_basic_building() {
        val marker =
            InspectionMarker(
                id = MARKER,
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor("A-1-2"),
                createdAt = Instant.parse("2026-11-10T09:30:00Z"),
                isRevoked = false,
                isMine = false,
            )
        val detailed =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 3),
                pendingSubmissions = 2,
                acceptedSubmissions = 7,
                entrancesTotal = 4,
                entrancesCheckedOnSite = 1,
                markers = listOf(marker),
            )
        val basic = BuildingProgress("B-4", InspectionCoverage.Whole(isInspected = true), 0, 1, 2, 2, emptyList())
        assertEquals(listOf(detailed, basic), json.decodeProgress(PROGRESS), "postęp dwóch budynków")
    }

    @Test
    fun us11_1_empty_list() {
        assertEquals(emptyList<BuildingProgress>(), json.decodeProgress("[]"), "brak budynków")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "to nie JSON",
            """[{"buildingSymbol":"A-1","detailLevel":"DETAILED"}]""",
            """[{"buildingSymbol":"A-1","detailLevel":"DETAILED","pendingSubmissions":0,"acceptedSubmissions":0,
                "entrancesTotal":0,"entrancesCheckedOnSite":0,"markers":[]}]""",
            """[{"buildingSymbol":"B-4","detailLevel":"BASIC","pendingSubmissions":0,"acceptedSubmissions":0,
                "entrancesTotal":0,"entrancesCheckedOnSite":0,"markers":[]}]""",
            """[{"buildingSymbol":"B-4","detailLevel":"BASIC","inspected":true,"pendingSubmissions":0,
                "acceptedSubmissions":0,"entrancesTotal":0,"entrancesCheckedOnSite":0,"markers":[
                {"id":"$MARKER","buildingSymbol":"B-4","createdAt":"wczoraj","revoked":false,"mine":true}]}]""",
        ],
    )
    fun bad_body_is_illegal_argument(body: String) {
        assertThrows(IllegalArgumentException::class.java) { json.decodeProgress(body) }
    }

    private companion object {
        const val MARKER = "3c2b1a09-8f7e-4d6c-b5a4-918273645546"
        const val PROGRESS =
            """[
                {"buildingSymbol":"A-1","detailLevel":"DETAILED","floorsTotal":5,"floorsInspected":3,
                 "pendingSubmissions":2,"acceptedSubmissions":7,"entrancesTotal":4,"entrancesCheckedOnSite":1,
                 "markers":[{"id":"$MARKER","buildingSymbol":"A-1","floorId":"A-1-2",
                  "createdAt":"2026-11-10T09:30:00Z","revoked":false,"mine":false}]},
                {"buildingSymbol":"B-4","detailLevel":"BASIC","inspected":true,
                 "pendingSubmissions":0,"acceptedSubmissions":1,"entrancesTotal":2,"entrancesCheckedOnSite":2,
                 "markers":[]}
            ]"""
    }
}
