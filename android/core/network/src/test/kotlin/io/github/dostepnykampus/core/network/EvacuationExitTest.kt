/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.EvacuationExit
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Wyjścia ewakuacyjne z planów PWr w karcie budynku `GET /buildings/{symbol}` (`BuildingDetails.evacuationExits`).
 *
 * Wymagania: US2.6, BR-27
 */
class EvacuationExitTest {
    /**
     * Wiersz: wyjście z opisem i `doorWidthCm` 90 → w modelu identyfikator, położenie, opis i szerokość 90 cm.
     *
     * Wymagania: US2.6, BR-27
     */
    @Test
    fun readsExit() {
        val exit = exits(exit(""""description":"$DESCRIPTION","doorWidthCm":90,""")).single()
        assertEquals(
            EvacuationExit(
                id = "pwr/9",
                location = GeoPoint(longitude = 17.0, latitude = 51.0),
                description = DESCRIPTION,
                doorWidthCm = 90,
            ),
            exit,
            "wyjście ewakuacyjne z karty budynku",
        )
    }

    /**
     * Wiersz: wyjście bez opisu i bez szerokości drzwi → pusty opis i szerokość 0, czyli brak danych.
     *
     * Wymagania: BR-27
     */
    @Test
    fun keepsMissingFieldsEmpty() {
        val exit = exits(exit("")).single()
        assertEquals("" to 0, exit.description to exit.doorWidthCm, "brak opisu i szerokości drzwi")
    }

    /**
     * Wiersz: karta bez pola `evacuationExits` → pusta lista wyjść.
     *
     * Wymagania: US2.6
     */
    @Test
    fun readsMissingListAsEmpty() {
        assertEquals(emptyList<EvacuationExit>(), exits(null), "brak wyjść ewakuacyjnych")
    }

    private fun exit(fields: String): String =
        """{"id":"pwr/9",$fields"location":{"type":"Point","coordinates":[17,51]},"source":"$PWR"}"""

    private fun exits(exit: String?): List<EvacuationExit> {
        val list = exit?.let { ""","evacuationExits":[$it]""" }.orEmpty()
        return Json
            .decodeBuildingDetails(
                """{"symbol":"A-1","detailLevel":"DETAILED","outline":$OUTLINE,"entrances":[]$list}""",
            ).evacuationExits
    }

    private companion object {
        const val PWR = "Politechnika Wrocławska"
        const val DESCRIPTION = "Wyjście ewakuacyjne z pomieszczenia 41"
        const val OUTLINE = """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""
    }
}
