/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.submission

import io.github.dostepnykampus.core.model.BarrierGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Klasyfikacja barier zgadza się ze słownikiem projektu: TB-1…TB-29 w sześciu grupach. Wymagania: BR-7, US7.3 */
class BarrierTypeTest {
    @Test
    fun ordersCodesTb1ToTb29() {
        assertEquals((1..29).map { "TB-$it" }, BarrierType.entries.map { it.code })
    }

    @Test
    fun takesGroupFromDictionary() {
        val expected =
            (1..5).associate { "TB-$it" to BarrierGroup.ENTRANCE_AND_DOORS } +
                (6..12).associate { "TB-$it" to BarrierGroup.VERTICAL_CIRCULATION } +
                (13..17).associate { "TB-$it" to BarrierGroup.HORIZONTAL_CIRCULATION } +
                (18..20).associate { "TB-$it" to BarrierGroup.TOILETS } +
                (21..25).associate { "TB-$it" to BarrierGroup.ORIENTATION_AND_INFORMATION } +
                (26..29).associate { "TB-$it" to BarrierGroup.OTHER }

        assertEquals(expected, BarrierType.entries.associate { it.code to it.group })
    }

    @Test
    fun needsDescriptionOnlyForTb29() {
        assertEquals(listOf("TB-29"), BarrierType.entries.filter { it.isDescriptionRequired }.map { it.code })
    }

    @Test
    fun parsesOnlyContractCodes() {
        assertEquals(BarrierType.THRESHOLD, BarrierType.fromCode("TB-13"))
        assertNull(BarrierType.fromCode("TB-30"))
        assertNull(BarrierType.fromCode("tb-13"))
    }

    @Test
    fun limitsSeverityToOneToThree() {
        assertEquals(listOf(1, 2, 3), Severity.entries.map { it.value })
        assertEquals(Severity.BLOCKING, Severity.fromValue(3))
        assertNull(Severity.fromValue(0))
        assertNull(Severity.fromValue(4))
    }
}
