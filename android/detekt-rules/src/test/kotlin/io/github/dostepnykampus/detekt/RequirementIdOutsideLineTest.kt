/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Reguła [RequirementIdOutsideLine] na krótkich źródłach: identyfikator wymagania tylko w linii `Wymagania:`. */
class RequirementIdOutsideLineTest {
    private val rule = RequirementIdOutsideLine(Config.empty)

    @Test
    fun `identyfikator w zdaniu to błąd`() {
        assertEquals(listOf(1), lines("/** Zwraca listę budynków (US1.1). */", "fun a() = 1"))
    }

    @Test
    fun `identyfikator przy parametrze to błąd`() {
        assertEquals(listOf(1), lines("/**", " * Zwraca listę.", " *", " * @param a próg z NFR-3", " */", "fun a() = 1"))
    }

    @Test
    fun `linia wymagań przechodzi`() {
        val text = listOf("/**", " * Zwraca listę.", " *", " * Wymagania: US1.1, NFR-19, BR-G2", " */", "fun a() = 1")
        assertEquals(emptyList<Int>(), lines(*text.toTypedArray()))
    }

    @Test
    fun `wymagania na końcu opisu jednowierszowego przechodzą`() {
        assertEquals(emptyList<Int>(), lines("/** Zwraca listę. Wymagania: US1.1, RO-GOS */", "fun a() = 1"))
    }

    @Test
    fun `symbol budynku i kod barier przechodzą`() {
        assertEquals(emptyList<Int>(), lines("/** Budynek C-13, M-6 i typ TB-29, adres US1.1 w `US1.1`. */", "fun a() = 1"))
    }

    @Test
    fun `wymagania wspólne bez dwukropka nie zwalniają zdania`() {
        assertEquals(listOf(1), lines("/** Wymagania wspólne i NFR-19 mówią o tym. */", "fun a() = 1"))
    }

    private fun lines(vararg text: String): List<Int> =
        rule.lint(text.joinToString(separator = "\n"), compile = false).map { it.entity.location.source.line }
}
