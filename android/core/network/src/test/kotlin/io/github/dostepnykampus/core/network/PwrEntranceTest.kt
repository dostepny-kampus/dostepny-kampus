/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Dane wejścia z warstwy wejść planów PWr w karcie budynku `GET /buildings/{symbol}` i w liście `GET /buildings`:
 * opis dojścia, szerokość drzwi i źródło oznaczenia.
 *
 * Wymagania: US2.1, US3.3, US17.3, BR-24, BR-27, US1.4
 */
class PwrEntranceTest {
    /**
     * Wiersz: wejście z `approach`, `doorWidthCm` 120 i `source` PWr, oznaczone importem → w modelu opis dojścia,
     *   szerokość 120 cm i źródło oznaczenia „z planów PWr”.
     *
     * Wymagania: US3.3, US17.3, BR-27
     */
    @Test
    fun readsPwrEntrance() {
        val entrance =
            entrance(""""approach":"Do wejścia prowadzą schody.","doorWidthCm":120,"source":"$PWR",""")
        assertEquals("Do wejścia prowadzą schody.", entrance.approach, "opis dojścia")
        assertEquals(120, entrance.doorWidthCm, "szerokość drzwi")
        assertEquals(AssessmentSource.PWR_PLANS, entrance.assessment?.source, "źródło oznaczenia z planów PWr")
    }

    /**
     * Wiersz: wejście tylko z OpenStreetMap, bez `approach`, `doorWidthCm` i `source` → pusty opis i szerokość 0,
     *   źródło oznaczenia „z map i opisów”.
     *
     * Wymagania: US17.3, BR-24
     */
    @Test
    fun keepsOsmEntrance() {
        val entrance = entrance("")
        assertEquals("", entrance.approach, "brak opisu dojścia")
        assertEquals(0, entrance.doorWidthCm, "brak szerokości drzwi")
        assertEquals(AssessmentSource.IMPORT, entrance.assessment?.source, "źródło oznaczenia z importu")
    }

    /**
     * Wiersz: wejście z planów PWr ze zmianą oznaczenia z obchodu → źródło oznaczenia „obchód”, bo obchód wygrywa
     *   z importem.
     *
     * Wymagania: US17.3, BR-24
     */
    @Test
    fun prefersWalkthrough() {
        val entrance = entrance(""""source":"$PWR",""", markingSource = "WALKTHROUGH")
        assertEquals(AssessmentSource.WALKTHROUGH, entrance.assessment?.source, "źródło oznaczenia z obchodu")
    }

    /**
     * Wiersz: wejście z `generatedName` true → nazwa nadana przez import; bez pola → nazwa z danych.
     *
     * Wymagania: BR-25, BR-27
     */
    @Test
    fun readsGeneratedName() {
        assertEquals(
            listOf(true, false),
            listOf(entrance(""""generatedName":true,"""), entrance("")).map(Entrance::isNameGenerated),
            "nazwa nadana przez import",
        )
    }

    /**
     * Wiersz: szerokość drzwi 0 → w modelu 0, czyli brak szerokości.
     *
     * Wymagania: BR-27
     */
    @Test
    fun dropsZeroDoorWidth() {
        val entrance = entrance(""""doorWidthCm":0,"source":"$PWR",""")
        assertEquals(0, entrance.doorWidthCm, "szerokość 0 to brak szerokości")
    }

    /**
     * Wiersz: lista budynków z wejściem tylko z planów PWr → budynek listy ma to samo wejście co karta; budynek bez
     *   pola `entrances` nie ma wejść.
     *
     * Wymagania: US1.4, BR-27
     */
    @Test
    fun readsEntranceInList() {
        val fields = """"approach":"Do wejścia prowadzą schody.","doorWidthCm":90,"source":"$PWR","""
        val listed =
            Json.decodeBuildingList(
                """{"source":"","buildings":[{"symbol":"A-1","detailLevel":"DETAILED","outline":$OUTLINE,""" +
                    """"entrances":[${entranceJson(fields, "IMPORT")}]},""" +
                    """{"symbol":"A-2","detailLevel":"BASIC","outline":$OUTLINE}]}""",
            )
        assertEquals(
            listOf(listOf(entrance(fields)), emptyList()),
            listed.buildings.map { it.entrances },
            "wejścia budynków z listy",
        )
    }

    private fun entranceJson(fields: String, markingSource: String): String =
        """{"id":"pwr/1","buildingSymbol":"A-1","name":"Wejście","main":false,$fields""" +
            """"location":{"type":"Point","coordinates":[17,51]},"marking":"ACCESSIBLE",""" +
            """"markingDate":"2024-10-12","markingSource":"$markingSource"}"""

    private fun entrance(fields: String, markingSource: String = "IMPORT"): Entrance =
        Json
            .decodeBuildingDetails(
                """{"symbol":"A-1","detailLevel":"DETAILED","outline":$OUTLINE,"entrances":[""" +
                    """${entranceJson(fields, markingSource)}]}""",
            ).entrances
            .single()

    private companion object {
        const val PWR = "Politechnika Wrocławska"
        const val OUTLINE = """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""
    }
}
