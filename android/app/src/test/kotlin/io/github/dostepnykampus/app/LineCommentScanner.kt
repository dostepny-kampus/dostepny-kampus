/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

/**
 * Skaner źródła Kotlina, który zwraca numery wierszy komentarzy liniowych `//`. Rozpoznaje składnię: napisy zwykłe
 * i surowe razem z szablonami `${...}` (także z cudzysłowami w środku), znaki oraz komentarze blokowe, w tym
 * zagnieżdżone, są pomijane w całości. Znacznik pracy odłożonej z numerem issue i opisem, `TODO(#12): opis`, nie
 * jest zgłaszany: stoi przy wierszu, którego dotyczy.
 */
internal class LineCommentScanner(private val text: String) {
    private var pos = 0
    private var line = 1
    private var depth = 0
    private val comments = mutableListOf<Int>()

    /** Numery wierszy (od 1) komentarzy liniowych w kolejności występowania. */
    fun lineComments(): List<Int> {
        pos = 0
        line = 1
        depth = 0
        comments.clear()
        code(inTemplate = false)
        return comments.toList()
    }

    private fun code(inTemplate: Boolean) {
        while (pos < text.length) {
            if (inTemplate && depth == 0 && text[pos] == '}') {
                pos++
                return
            }
            codeStep()
        }
    }

    private fun codeStep() {
        when {
            isAt("//") -> lineComment()
            isAt("/*") -> blockComment()
            isAt(RAW) -> rawString()
            text[pos] == '"' -> string()
            text[pos] == '\'' -> character()
            else -> plain()
        }
    }

    private fun plain() {
        when (text[pos]) {
            '{' -> depth++
            '}' -> depth--
        }
        advance()
    }

    private fun lineComment() {
        val start = pos
        while (pos < text.length && text[pos] != '\n') pos++
        if (!ISSUE_MARKER.matches(text.substring(startIndex = start, endIndex = pos).trimEnd())) comments += line
    }

    private fun blockComment() {
        var nesting = 0
        do {
            nesting += blockStep()
        } while (nesting > 0 && pos < text.length)
    }

    private fun blockStep(): Int =
        when {
            isAt("/*") -> skip(count = 2, delta = 1)
            isAt("*/") -> skip(count = 2, delta = -1)
            else -> skip(count = 1, delta = 0)
        }

    private fun string() {
        pos++
        var isOpen = true
        while (isOpen && pos < text.length && text[pos] != '\n') isOpen = stringStep()
    }

    private fun stringStep(): Boolean {
        val isClosing = text[pos] == '"'
        when {
            isClosing -> pos++
            text[pos] == '\\' -> pos += 2
            isAt(TEMPLATE) -> template()
            else -> pos++
        }
        return !isClosing
    }

    private fun rawString() {
        pos += RAW.length
        var isOpen = true
        while (isOpen && pos < text.length) isOpen = rawStep()
    }

    private fun rawStep(): Boolean {
        val isClosing = isAt(RAW)
        when {
            isClosing -> skipQuotes()
            isAt(TEMPLATE) -> template()
            else -> advance()
        }
        return !isClosing
    }

    private fun skipQuotes() {
        while (pos < text.length && text[pos] == '"') pos++
    }

    private fun template() {
        val outer = depth
        pos += TEMPLATE.length
        depth = 0
        code(inTemplate = true)
        depth = outer
    }

    private fun character() {
        pos++
        while (pos < text.length && text[pos] != '\'' && text[pos] != '\n') {
            pos += if (text[pos] == '\\') 2 else 1
        }
        if (pos < text.length && text[pos] == '\'') pos++
    }

    private fun skip(count: Int, delta: Int): Int {
        repeat(count) { advance() }
        return delta
    }

    private fun isAt(token: String): Boolean = text.startsWith(token, pos)

    private fun advance() {
        if (pos < text.length && text[pos] == '\n') line++
        pos++
    }

    private companion object {
        const val RAW = "\"\"\""
        const val TEMPLATE = "\${"
        val ISSUE_MARKER = Regex("""// TODO\(#\d+\): \S.*""")
    }
}
