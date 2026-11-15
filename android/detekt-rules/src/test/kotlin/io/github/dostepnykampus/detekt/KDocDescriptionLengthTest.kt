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

/** Reguła [KDocDescriptionLength] na krótkich źródłach: zgłasza blok KDoc z opisem dłuższym niż sześć linii. */
class KDocDescriptionLengthTest {
    private val rule = KDocDescriptionLength(Config.empty)

    @Test
    fun rejectsSevenDescriptionLines() {
        assertEquals(listOf(1), lines(kdoc(description = 7)))
    }

    @Test
    fun acceptsSixDescriptionLines() {
        assertEquals(emptyList<Int>(), lines(kdoc(description = 6)))
    }

    @Test
    fun skipsTags() {
        assertEquals(emptyList<Int>(), lines(kdoc(description = 6, tags = 5)))
    }

    @Test
    fun skipsRequirementsLine() {
        val text = kdoc(description = 6).dropLast(2) + listOf(" *", " * Wymagania: US1", " */", "fun a() = 1")
        assertEquals(emptyList<Int>(), lines(text))
    }

    @Test
    fun skipsBlankLines() {
        val text = listOf("/**", " * a", " *", " * b", " *", " * c", " *", " * d", " * e", " * f", " */", "fun a() = 1")
        assertEquals(emptyList<Int>(), lines(text))
    }

    @Test
    fun countsOpeningLineText() {
        val text = listOf("/** a", " * b", " * c", " * d", " * e", " * f", " * g */", "fun a() = 1")
        assertEquals(listOf(1), lines(text))
    }

    private fun kdoc(description: Int, tags: Int = 0): List<String> =
        listOf("/**") +
            List(description) { " * linia $it" } +
            List(tags) { " * @param p$it znaczenie" } +
            listOf(" */", "fun a() = 1")

    private fun lines(text: List<String>): List<Int> =
        rule.lint(text.joinToString(separator = "\n"), compile = false).map { it.entity.location.source.line }
}
