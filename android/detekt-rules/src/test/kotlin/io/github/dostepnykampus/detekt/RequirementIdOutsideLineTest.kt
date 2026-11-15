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
    fun rejectsIdInSentence() {
        assertEquals(listOf(1), lines("/** Zwraca listę budynków (US1.1). */"))
    }

    @Test
    fun rejectsMarkerWithRequirement() {
        assertEquals(listOf(1), lines("/**", " * Zwraca listę.", " *", " * @param a próg z NFR-3", " */"))
    }

    @Test
    fun acceptsRequirementsLine() {
        assertEquals(emptyList<Int>(), lines("/**", " * Zwraca listę.", " *", " * Wymagania: US1.1, BR-G2", " */"))
    }

    @Test
    fun acceptsTrailingRequirements() {
        assertEquals(emptyList<Int>(), lines("/** Zwraca listę. Wymagania: US1.1, RO-GOS */"))
    }

    @Test
    fun acceptsSymbolsAndCode() {
        assertEquals(emptyList<Int>(), lines("/** Budynek C-13, M-6 i typ TB-29, kod `US1.1`. */"))
    }

    @Test
    fun treatsNoColonAsSentence() {
        assertEquals(listOf(1), lines("/** Wymagania wspólne i NFR-19 mówią o tym. */"))
    }

    private fun lines(vararg kdoc: String): List<Int> =
        rule
            .lint((kdoc.toList() + "fun a() = 1").joinToString(separator = "\n"), compile = false)
            .map { it.entity.location.source.line }
}
