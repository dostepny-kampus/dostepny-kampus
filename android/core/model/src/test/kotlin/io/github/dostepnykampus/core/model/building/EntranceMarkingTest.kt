/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/** Oznaczenia wejścia odpowiadają wartościom `EntranceMarking` z kontraktu API. Wymagania: US2.1, US17.1, BR-24 */
class EntranceMarkingTest {
    @Test
    fun matchesContractNames() {
        assertEquals(CONTRACT_VALUES, EntranceMarking.entries.map(EntranceMarking::name))
    }

    @Test
    fun parsesContractValue() {
        assertEquals(EntranceMarking.entries, CONTRACT_VALUES.map(EntranceMarking::valueOf))
    }

    @Test
    fun keepsNoDataDistinct() {
        assertNotEquals(EntranceMarking.INACCESSIBLE, EntranceMarking.valueOf("NO_DATA"))
    }

    private companion object {
        val CONTRACT_VALUES = listOf("ACCESSIBLE", "INACCESSIBLE", "NO_DATA")
    }
}
