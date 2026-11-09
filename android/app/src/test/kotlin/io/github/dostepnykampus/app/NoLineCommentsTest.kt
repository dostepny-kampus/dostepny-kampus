/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Strażnik komentarzy: w kodzie i testach Kotlina nie ma linii zaczynających się od `//`; opis idzie do KDoc
 * deklaracji, której dotyczy.
 *
 * `offenders`: Wpisy `plik:wiersz` dla każdej takiej linii, więc komunikat wskazuje miejsce do poprawy.
 */
class NoLineCommentsTest {
    @Test
    fun `kod i testy bez komentarzy liniowych`() {
        assertEquals(emptyList<String>(), offenders(Konsist.scopeFromProject().files))
    }

    @Test
    fun `komentarz liniowy to błąd z plikiem i wierszem`(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Bad.kt",
                source = "package a\n\nclass Bad {\n    // opis\n    fun f() = 1\n}\n",
            )
        assertEquals(listOf("Bad.kt:4"), offenders(files))
    }

    @Test
    fun `KDoc nagłówek licencji i adres w tekście przechodzą`(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Good.kt",
                source =
                    "/*\n * Dostępny Kampus\n */\npackage a\n\n/** Opis. */\nclass Good {\n" +
                        "    val url = \"https://example.org\" // nie na początku linii\n}\n",
            )
        assertEquals(emptyList<String>(), offenders(files))
    }

    private fun offenders(files: List<KoFileDeclaration>): List<String> =
        files
            .filterNot { File(it.path).invariantSeparatorsPath.contains("/build/") }
            .flatMap { file ->
                file.text.lines().mapIndexedNotNull { index, line ->
                    if (line.trimStart().startsWith("//")) "${file.nameWithExtension}:${index + 1}" else null
                }
            }

    private fun File.fixture(name: String, source: String): List<KoFileDeclaration> {
        val directory = File(this, name.substringBefore('.')).apply { mkdirs() }
        File(directory, name).writeText(source)
        return Konsist.scopeFromExternalDirectory(directory.absolutePath).files
    }
}
