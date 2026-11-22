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
import org.junit.jupiter.api.Assertions.assertNull
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

    /** Scenariusz: przegladanie-mapy.feature: Wyszukanie budynku po nazwie. */
    @ParameterizedTest
    @ValueSource(strings = ["glow", "GŁÓW", "główny", "budynek gł"])
    fun us1_1_name_word_finds(query: String) {
        assertEquals(listOf("A-1"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = ["atol", "dom stud", "STUDENCKI"])
    fun us1_1_name_any_word(query: String) {
        assertEquals(listOf("T-9"), symbols(query))
    }

    @ParameterizedTest
    @ValueSource(strings = ["gl", "ow", "ówny", "dent"])
    fun us1_1_name_needs_word_start(query: String) {
        assertEquals(emptyList<String>(), symbols(query))
    }

    @Test
    fun us1SymbolBeforeName() {
        val near = listOf(building("C-3", name = "Przy T-1"), building("T-1"), building("T-10"))
        assertEquals(listOf("T-1", "T-10", "C-3"), searchBuildings("t-1", near).map { it.symbol })
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Mapa i wyszukiwanie działają bez logowania. */
    @Test
    fun findsA1InRealFile() {
        val real = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val found = searchBuildings("A-1", real.buildings).map { it.symbol }
        assertEquals("A-1", found.firstOrNull { it == "A-1" }, "A-1 wśród podpowiedzi: $found")
        assertEquals(emptyList<String>(), found.filterNot { it.startsWith("A-1") }, "tylko symbole od A-1: $found")
    }

    @Test
    fun buildingBeforeRoomIsExact() {
        assertEquals("A-10", buildingBeforeRoom("A-10 999", buildings())?.symbol, "Symbol A-10 nie dał budynku A-10")
        assertEquals("A-1", buildingBeforeRoom("a1 999", buildings())?.symbol, "Zapis bez dywizu nie dał budynku A-1")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "329", "A-1", "329 A-1", "Z-9 999", "A-1 sala 999"])
    fun buildingBeforeRoomNeedsBoth(query: String) {
        assertNull(buildingBeforeRoom(query, buildings()), "Znaleziono budynek dla „$query”")
    }

    private fun symbols(query: String): List<String> = searchBuildings(query, buildings()).map { it.symbol }

    private fun buildings(): List<Building> =
        listOf("A-1", "A-2", "A-10", "C-1", "C-13", "C-3", "D-8a").map { building(symbol = it, name = NAMES[it]) } +
            building(symbol = "T-9", name = "Dom Studencki \"Atol\"")

    private fun building(symbol: String, name: String? = null): Building {
        val outline = GeoPolygon(listOf(List(4) { GeoPoint(longitude = 0.0, latitude = 0.0) }))
        return Building(symbol = symbol, name = name, outline = outline)
    }

    private companion object {
        val NAMES = mapOf("A-1" to "Budynek główny")
    }
}
