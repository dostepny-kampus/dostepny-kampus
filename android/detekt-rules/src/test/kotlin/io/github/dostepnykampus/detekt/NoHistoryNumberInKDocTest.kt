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

/** Reguła [NoHistoryNumberInKDoc] na krótkich źródłach: zgłasza blok KDoc z numerem issue albo decyzji. */
class NoHistoryNumberInKDocTest {
    private val rule = NoHistoryNumberInKDoc(Config.empty)

    @Test
    fun `numer issue to błąd`() {
        assertEquals(listOf(1), lines("/** Zwraca listę (#123). */", "fun a() = 1"))
    }

    @Test
    fun `numer decyzji to błąd`() {
        assertEquals(listOf(1), lines("/** Zwraca listę (decyzja 82). */", "fun a() = 1"))
    }

    @Test
    fun `numer decyzji po złamaniu linii`() {
        assertEquals(listOf(1), lines("/**", " * Zwraca listę (decyzja", " * 82).", " */", "fun a() = 1"))
    }

    @Test
    fun `decyzje z numerem w środku zdania`() {
        assertEquals(listOf(1), lines("/** Motyw z tokenów (decyzje projektu nr 48 i 50). */", "fun a() = 1"))
    }

    @Test
    fun `jeden blok to jedno zgłoszenie`() {
        assertEquals(listOf(1), lines("/** Lista (#1), (#2), decyzja 3. */", "fun a() = 1"))
    }

    @Test
    fun `znacznik pracy odłożonej przechodzi`() {
        assertEquals(emptyList<Int>(), lines("/** Zwraca listę. ${MARKER}sortowanie. */", "fun a() = 1"))
    }

    @Test
    fun `decyzja osoby bez numeru przechodzi`() {
        assertEquals(emptyList<Int>(), lines("/** Czeka na decyzję osoby (BR-26). */", "fun a() = 1"))
    }

    @Test
    fun `kolor i encja przechodzą`() {
        assertEquals(emptyList<Int>(), lines("/** Tło `#1A2B3C`, znak &#123; i adres a/#12. */", "fun a() = 1"))
    }

    @Test
    fun `komentarz blokowy nie jest sprawdzany`() {
        assertEquals(emptyList<Int>(), lines("/* Nagłówek (#123). */", "fun a() = 1"))
    }

    private fun lines(vararg text: String): List<Int> =
        rule.lint(text.joinToString(separator = "\n"), compile = false).map { it.entity.location.source.line }

    private companion object {
        const val MARKER = "T" + "ODO(#12): "
    }
}
