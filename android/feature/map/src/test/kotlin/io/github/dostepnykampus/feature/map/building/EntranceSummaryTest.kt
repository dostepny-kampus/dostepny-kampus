/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.EntranceMarking.ACCESSIBLE
import io.github.dostepnykampus.core.model.building.EntranceMarking.INACCESSIBLE
import io.github.dostepnykampus.core.model.building.EntranceMarking.NO_DATA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Wiersz o wejściach pod nagłówkiem karty budynku. Wymagania: US2.8, US2.1, US3.1, US17.1 */
class EntranceSummaryTest {
    @Test
    fun acceptsOneAccessible() {
        assertEquals(EntranceSummary.Accessible, summary(INACCESSIBLE, NO_DATA, ACCESSIBLE))
    }

    @Test
    fun reportsNoneWhenInaccessible() {
        assertEquals(EntranceSummary.NoAccessible, summary(INACCESSIBLE, INACCESSIBLE))
    }

    @Test
    fun treatsEmptyAsNoData() {
        assertEquals(EntranceSummary.NoData, summary())
    }

    @Test
    fun treatsMixedUnknownAsNoData() {
        assertEquals(EntranceSummary.NoData, summary(INACCESSIBLE, NO_DATA))
    }

    @Test
    fun treatsAllUnknownAsNoData() {
        assertEquals(EntranceSummary.NoData, summary(NO_DATA, NO_DATA))
    }

    @Test
    fun treatsServiceOnlyAsNoData() {
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
