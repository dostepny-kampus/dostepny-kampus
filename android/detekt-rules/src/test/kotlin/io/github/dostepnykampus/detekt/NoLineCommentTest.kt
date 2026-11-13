/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Reguła [NoLineComment] na krótkich źródłach: zgłasza komentarze `//`, a pomija `//` w napisach. */
class NoLineCommentTest {
    private val rule = NoLineComment(Config.empty)

    @Test
    fun `komentarz liniowy to błąd`() {
        assertEquals(listOf(4), lines("package a", "", "class Bad {", "    // opis", "}"))
    }

    @Test
    fun `komentarz na końcu linii`() {
        assertEquals(listOf(3), lines("package a", "", "val x = 1 // opis"))
    }

    @Test
    fun `skrypt kts też jest sprawdzany`() {
        val script =
            compileContentForTest(
                content = listOf("plugins {", "    // opis", "}").joinToString(separator = "\n"),
                filename = "a.gradle.kts",
            )
        assertEquals(listOf(2), rule.lint(script).map(::line))
    }

    @Test
    fun `surowy napis przechodzi`() {
        assertEquals(emptyList<Int>(), lines("val s = $QUOTES", "// to nie komentarz", QUOTES))
    }

    @Test
    fun `napis z wieloma dolarami`() {
        val source = "val s = $DOLLARS$QUOTES$DOLLARS{${QUOTE}a$QUOTE} $DOLLAR// tekst$QUOTES"
        assertEquals(listOf(2), lines(source, "val y = $DOLLARS$QUOTE//$QUOTE // opis"))
    }

    @Test
    fun `KDoc i adres przechodzą`() {
        val source = listOf("/*", " * Dostępny Kampus", " */", "package a", "", "/** Opis. */", "val url = $URL")
        assertEquals(emptyList<Int>(), lines(*source.toTypedArray()))
    }

    @Test
    fun `blok zagnieżdżony z ukośnikami`() {
        assertEquals(emptyList<Int>(), lines("/* zewnętrzny /* wewnętrzny */ // dalej w zewnętrznym */", "val x = 1"))
    }

    @Test
    fun `komentarz po szablonie napisu`() {
        assertEquals(listOf(2), lines("val s = $TEMPLATE", "val y = 2 // opis"))
    }

    @Test
    fun `znacznik z numerem przechodzi`() {
        assertEquals(emptyList<Int>(), lines("class Later {", "    $MARKER", "    val x = 1 $MARKER", "}"))
    }

    @Test
    fun `znacznik niepełny to błąd`() {
        val loose = listOf("// TODO: opis", "// TODO(#12)", "// TODO(12): opis", "// TODO(#12):")
        assertEquals(listOf(1, 2, 3, 4), lines(*loose.toTypedArray()))
    }

    private fun lines(vararg text: String): List<Int> =
        rule.lint(text.joinToString(separator = "\n"), compile = false).map(::line)

    private fun line(finding: Finding): Int = finding.entity.location.source.line

    private companion object {
        const val QUOTE = '"'
        const val QUOTES = "$QUOTE$QUOTE$QUOTE"
        const val DOLLAR = '$'
        const val DOLLARS = "$DOLLAR$DOLLAR"
        const val TEMPLATE = "$QUOTE$DOLLAR{${QUOTE}a$QUOTE + $QUOTE//$QUOTE}$QUOTE"
        const val MARKER = "// TODO(#12): opis"
        const val URL = "${QUOTE}https://example.org$QUOTE"
    }
}
