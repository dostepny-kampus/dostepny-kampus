/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Strażnik komentarzy: w kodzie i skryptach Kotlina (`*.kt` i `*.kts` w `android/`) nie ma komentarzy liniowych
 * `//`, ani na początku linii, ani na końcu linii z kodem; opis idzie do KDoc deklaracji, której dotyczy.
 *
 * Komentarz rozpoznaje po składni, nie po tekście linii: napisy zwykłe i surowe, znaki oraz komentarze blokowe
 * są pomijane w całości, więc `//` w adresie albo w surowym napisie nie jest komentarzem.
 *
 * `offenders`: Wpisy `ścieżka:wiersz` względem katalogu nadrzędnego `android/`, więc komunikat wskazuje miejsce
 * do poprawy.
 */
class NoLineCommentsTest {
    @Test
    fun `kod bez komentarzy liniowych`() {
        val root =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .first { File(it, "settings.gradle.kts").exists() }
        assertEquals(emptyList<String>(), offenders(root))
    }

    @Test
    fun `komentarz liniowy to błąd`(
        @TempDir fixtures: File,
    ) {
        val source = lines("package a", "", "class Bad {", "    // opis", "}")
        fixtures.fixture(name = "Bad.kt", source = source)
        assertEquals(listOf("android/Bad.kt:4"), offenders(File(fixtures, "android")))
    }

    @Test
    fun `komentarz na końcu linii`(
        @TempDir fixtures: File,
    ) {
        val source = lines("package a", "", "val x = 1 // opis")
        fixtures.fixture(name = "Tail.kt", source = source)
        assertEquals(listOf("android/Tail.kt:3"), offenders(File(fixtures, "android")))
    }

    @Test
    fun `skrypt kts też jest sprawdzany`(
        @TempDir fixtures: File,
    ) {
        val source = lines("plugins {", "    // opis", "}")
        fixtures.fixture(name = "build.gradle.kts", source = source)
        assertEquals(listOf("android/build.gradle.kts:2"), offenders(File(fixtures, "android")))
    }

    @Test
    fun `surowy napis przechodzi`(
        @TempDir fixtures: File,
    ) {
        val source = lines("val s = $QUOTES", "// to nie komentarz", QUOTES)
        fixtures.fixture(name = "Raw.kt", source = source)
        assertEquals(emptyList<String>(), offenders(File(fixtures, "android")))
    }

    @Test
    fun `KDoc i adres przechodzą`(
        @TempDir fixtures: File,
    ) {
        val source = lines("/*", " * Dostępny Kampus", " */", "package a", "", "/** Opis. */", "val url = $URL")
        fixtures.fixture(name = "Good.kt", source = source)
        assertEquals(emptyList<String>(), offenders(File(fixtures, "android")))
    }

    private fun offenders(android: File): List<String> =
        android
            .walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" }
            .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
            .flatMap { file -> lineComments(file, android) }
            .toList()

    private fun lineComments(file: File, android: File): Sequence<String> {
        val text = file.readText()
        val shown = file.relativeTo(android.absoluteFile.parentFile ?: android).invariantSeparatorsPath
        return TOKEN
            .findAll(text)
            .filter { it.value.startsWith("//") }
            .map { "$shown:${text.take(it.range.first).count { c -> c == '\n' } + 1}" }
    }

    private fun lines(vararg text: String): String = text.joinToString(separator = "\n")

    private fun File.fixture(name: String, source: String) {
        File(this, "android").apply { mkdirs() }.resolve(name).writeText(source)
    }

    private companion object {
        const val QUOTE = '"'
        const val QUOTES = "$QUOTE$QUOTE$QUOTE"
        const val URL = "${QUOTE}https://example.org$QUOTE"
        const val PATTERN =
            """$QUOTES[\s\S]*?$QUOTES|$QUOTE(?:\\.|[^$QUOTE\\\n])*$QUOTE|'(?:\\.|[^'\\\n])+'|/\*[\s\S]*?\*/|//[^\n]*"""
        val TOKEN = Regex(PATTERN)
    }
}
