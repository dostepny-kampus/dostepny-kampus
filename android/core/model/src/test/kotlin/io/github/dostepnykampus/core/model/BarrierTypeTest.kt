/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Klasyfikacja barier zgadza się ze słownikiem projektu: TB-1…TB-29 w sześciu grupach (BR-7, US7.3). */
class BarrierTypeTest {
    @Test
    fun `kody TB-1 do TB-29 po kolei`() {
        assertEquals((1..29).map { "TB-$it" }, BarrierType.entries.map { it.code })
    }

    @Test
    fun `liczba typów w każdej grupie`() {
        val counts = BarrierType.entries.groupingBy { it.group }.eachCount()

        assertEquals(
            mapOf(
                BarrierGroup.ENTRANCE_AND_DOORS to 5,
                BarrierGroup.VERTICAL_CIRCULATION to 7,
                BarrierGroup.HORIZONTAL_CIRCULATION to 5,
                BarrierGroup.TOILETS to 3,
                BarrierGroup.ORIENTATION_AND_INFORMATION to 5,
                BarrierGroup.OTHER to 4,
            ),
            counts,
        )
    }

    @Test
    fun `opis wymagany tylko przy TB-29`() {
        assertEquals(listOf("TB-29"), BarrierType.entries.filter { it.isDescriptionRequired }.map { it.code })
    }

    @Test
    fun `kod z kontraktu i spoza niego`() {
        assertEquals(BarrierType.THRESHOLD, BarrierType.fromCode("TB-13"))
        assertNull(BarrierType.fromCode("TB-30"))
        assertNull(BarrierType.fromCode("tb-13"))
    }

    @Test
    fun `uciążliwość tylko 1 do 3`() {
        assertEquals(listOf(1, 2, 3), Severity.entries.map { it.value })
        assertEquals(Severity.BLOCKING, Severity.fromValue(3))
        assertNull(Severity.fromValue(0))
        assertNull(Severity.fromValue(4))
    }
}
