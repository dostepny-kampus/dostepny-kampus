/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.EntranceMarking.ACCESSIBLE
import io.github.dostepnykampus.core.model.EntranceMarking.INACCESSIBLE
import io.github.dostepnykampus.core.model.EntranceMarking.NO_DATA
import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Wiersz o wejściach pod nagłówkiem karty budynku. Wymagania: US2.8, US2.1, US3.1, US17.1 */
class EntranceSummaryTest {
    @Test
    fun us2_8_one_accessible_is_enough() {
        assertEquals(EntranceSummary.Accessible, summary(INACCESSIBLE, NO_DATA, ACCESSIBLE))
    }

    @Test
    fun us2_8_all_inaccessible_none() {
        assertEquals(EntranceSummary.NoAccessible, summary(INACCESSIBLE, INACCESSIBLE))
    }

    @Test
    fun us2_8_empty_is_no_data() {
        assertEquals(EntranceSummary.NoData, summary())
    }

    @Test
    fun us2_8_mixed_unknown_no_data() {
        assertEquals(EntranceSummary.NoData, summary(INACCESSIBLE, NO_DATA))
    }

    @Test
    fun us2_8_all_unknown_is_no_data() {
        assertEquals(EntranceSummary.NoData, summary(NO_DATA, NO_DATA))
    }

    @Test
    fun us2_8_service_only_is_no_data() {
        val service = listOf(entrance(ACCESSIBLE, type = "emergency"), entrance(ACCESSIBLE, type = "exit"))
        assertEquals(EntranceSummary.NoData, entranceSummary(orderedEntrances(service)))
    }

    private fun summary(vararg markings: EntranceMarking): EntranceSummary =
        entranceSummary(orderedEntrances(markings.map { entrance(it) }))

    private fun entrance(marking: EntranceMarking, type: String = "yes") =
        Entrance(
            id = "${marking.name}-$type",
            buildingSymbol = "A-1",
            name = null,
            isMain = false,
            type = type,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = marking,
        )
}
