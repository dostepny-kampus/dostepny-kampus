/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Wybór budynków po dotknięciu mapy V1: kontur pod palcem przed konturami w zasięgu dotknięcia.
 *
 * Wymagania: US2.8, NFR-17
 */
class OutlineTouchTest {
    private val data =
        CampusMapData(
            buildings = JsonObject(emptyMap()),
            labels = emptyList(),
            labelPoints = JsonObject(emptyMap()),
            entrances = JsonObject(emptyMap()),
            start = null,
            outlines = listOf(listOf("B-1"), listOf("B-2"), listOf("C-3", "C-4")),
        )

    /** Wiersz: V1, dotknięcie wewnątrz konturu B-2 obok konturu B-1 otwiera B-2 bez arkusza wyboru. */
    @Test
    fun opensOutlineUnderFinger() {
        val symbols = data.touchedSymbols(hit = listOf(1)) { listOf(0, 1) }
        assertEquals(listOf("B-2"), symbols, "kontur pod palcem")
    }

    /** Wiersz: V1, dotknięcie obok dwóch konturów daje oba budynki do arkusza „Który budynek?”. */
    @Test
    fun choosesBetweenNearOutlines() {
        val symbols = data.touchedSymbols(hit = emptyList()) { listOf(0, 1) }
        assertEquals(listOf("B-1", "B-2"), symbols, "kontury w zasięgu")
    }

    /** Wiersz: V1, dotknięcie wewnątrz konturu wspólnego zwraca wszystkie jego symbole. */
    @Test
    fun choosesWithinSharedOutline() {
        val symbols = data.touchedSymbols(hit = listOf(2)) { listOf(1, 2) }
        assertEquals(listOf("C-3", "C-4"), symbols, "symbole konturu wspólnego")
    }

    /** Trafienie w kontur nie pyta o kontury w zasięgu dotknięcia. */
    @Test
    fun skipsNearQueryOnHit() {
        var queries = 0
        data.touchedSymbols(hit = listOf(0)) { listOf(1).also { queries++ } }
        assertEquals(0, queries, "zapytania o kontury w zasięgu")
    }
}
