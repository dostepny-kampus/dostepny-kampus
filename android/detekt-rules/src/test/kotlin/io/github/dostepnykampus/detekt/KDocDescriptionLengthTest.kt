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
    fun `siedem linii opisu to błąd`() {
        assertEquals(listOf(1), lines(kdoc(description = 7)))
    }

    @Test
    fun `sześć linii opisu przechodzi`() {
        assertEquals(emptyList<Int>(), lines(kdoc(description = 6)))
    }

    @Test
    fun `linie znaczników nie są liczone`() {
        assertEquals(emptyList<Int>(), lines(kdoc(description = 6, tags = 5)))
    }

    @Test
    fun `puste linie nie są liczone`() {
        val text = listOf("/**", " * a", " *", " * b", " *", " * c", " *", " * d", " * e", " * f", " */", "fun a() = 1")
        assertEquals(emptyList<Int>(), lines(*text.toTypedArray()))
    }

    @Test
    fun `opis w linii otwarcia jest liczony`() {
        val text = listOf("/** a", " * b", " * c", " * d", " * e", " * f", " * g */", "fun a() = 1")
        assertEquals(listOf(1), lines(*text.toTypedArray()))
    }

    private fun kdoc(description: Int, tags: Int = 0): Array<String> =
        (
            listOf("/**") +
                List(description) { " * linia $it" } +
                List(tags) { " * @param p$it znaczenie" } +
                listOf(" */", "fun a() = 1")
        ).toTypedArray()

    private fun lines(vararg text: String): List<Int> =
        rule.lint(text.joinToString(separator = "\n"), compile = false).map { it.entity.location.source.line }
}
