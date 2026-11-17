/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

/**
 * Podpowiedzi budynków po początku symbolu na budynkach testowych o podobnych symbolach i w prawdziwym pliku
 * konturów.
 *
 * Wymagania: US1.1, US1.2
 */
class SearchBuildingsTest {
    /** Scenariusz (częściowo): przegladanie-mapy.feature: Wyszukanie budynku po symbolu w różnym zapisie. */
    @ParameterizedTest
    @ValueSource(strings = ["C13", "c-13", "C-13"])
    fun us1_1_spelling_finds_c13(query: String) {
        assertEquals(listOf("C-13"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = [" c 13 ", "C–13", "c−13"])
    fun us1_1_spaces_dashes_find(query: String) {
        assertEquals(listOf("C-13"), symbols(query))
    }

    /** Scenariusz: przegladanie-mapy.feature: Podpowiedzi budynków od pierwszego znaku symbolu. */
    @ParameterizedTest
    @ValueSource(strings = ["A", "a", "A-", "a –"])
    fun us1_1_first_letter_suggests(query: String) {
        assertEquals(listOf("A-1", "A-2", "A-10"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = ["c1", "C-1", "C 1"])
    fun us1_1_symbol_start_suggests(query: String) {
        assertEquals(listOf("C-1", "C-13"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = ["d8", "d8A", "D-8a"])
    fun us1_1_suffix_any_case(query: String) {
        assertEquals(listOf("D-8a"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "13", "C-13x", "C--13", "A-1 329", "Z-99", "C-0013", "AB"])
    fun us1_1_no_match_is_empty(query: String) {
        assertEquals(emptyList<String>(), symbols(query))
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Mapa i wyszukiwanie działają bez logowania. */
    @Test
    fun findsA1InRealFile() {
        val real = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val found = searchBuildings("A-1", real.buildings).map { it.symbol }
        assertEquals("A-1", found.firstOrNull { it == "A-1" }, "A-1 wśród podpowiedzi: $found")
        assertEquals(emptyList<String>(), found.filterNot { it.startsWith("A-1") }, "tylko symbole od A-1: $found")
    }

    private fun symbols(query: String): List<String> = searchBuildings(query, buildings()).map { it.symbol }

    private fun buildings(): List<Building> {
        val outline = GeoPolygon(listOf(List(4) { GeoPoint(longitude = 0.0, latitude = 0.0) }))
        return listOf("A-1", "A-2", "A-10", "C-1", "C-13", "C-3", "D-8a")
            .map { Building(symbol = it, name = null, outline = outline) }
    }
}
