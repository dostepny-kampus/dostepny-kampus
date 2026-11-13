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
 * Każdy moduł z `settings.gradle.kts` ma `README.md` z częściami [SECTIONS], a część „Zależności” wymienia
 * te same moduły projektu co `api(project(…))` i `implementation(project(…))` w `build.gradle.kts`.
 * Moduły bez opisu są na liście wyjątków z numerem issue, a lista może tylko maleć.
 */
class ModuleReadmeArchitectureTest {
    /** Moduły spoza listy wyjątków mają pełny opis zgodny z zależnościami. */
    @Test
    fun infra_readme_matches_build() {
        val found = problems(projectRoot())
        assertEquals(emptyMap<String, String>(), found - UNDESCRIBED.keys, HINT)
    }

    /** Wpis listy wyjątków dla modułu, który ma już pełny opis, czerwieni test. */
    @Test
    fun infra_readme_list_shrinks() {
        assertEquals(emptySet<String>(), UNDESCRIBED.keys - problems(projectRoot()).keys)
    }

    /** Brak części opisu i zależność spoza `build.gradle.kts` łamią regułę, pełny zgodny opis nie. */
    @Test
    fun infra_readme_rule_rejects(
        @TempDir root: File,
    ) {
        File(root, "settings.gradle.kts").writeText("""include(":core:ok", ":core:partial", ":core:wrong")""")
        module(root, "core/ok", listOf(":core:base"), listOf(":core:base"), SECTIONS)
        module(root, "core/partial", emptyList(), emptyList(), SECTIONS - "API")
        module(root, "core/wrong", listOf(":core:base"), listOf(":core:other"), SECTIONS)
        assertEquals(setOf(":core:partial", ":core:wrong"), problems(root).keys)
    }

    private fun module(
        root: File,
        path: String,
        built: List<String>,
        described: List<String>,
        sections: List<String>,
    ) {
        val dir = File(root, path).apply { mkdirs() }
        File(dir, "build.gradle.kts").writeText(built.joinToString("\n") { """    api(project("$it"))""" })
        val text =
            sections.joinToString("\n\n") { section ->
                val body = if (section == DEPENDENCIES) described.joinToString("\n") { "- `$it`" } else "Treść."
                "## $section\n\n$body"
            }
        File(dir, "README.md").writeText("# :$path\n\n$text\n")
    }

    private fun problems(root: File): Map<String, String> =
        MODULE
            .findAll(File(root, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .mapNotNull { name -> problem(File(root, path(name)))?.let { name to it } }
            .toMap()

    private fun path(module: String): String = module.removePrefix(":").replace(oldChar = ':', newChar = '/')

    private fun problem(dir: File): String? {
        val text = File(dir, "README.md").takeIf(File::isFile)?.readText()
        val built = PROJECT.findAll(File(dir, "build.gradle.kts").readText()).map { it.groupValues[1] }.toSet()
        val headings = HEADING.findAll(text.orEmpty()).map { it.groupValues[1].trim() }.toList()
        val described = DEPENDENCY.findAll(section(text.orEmpty())).map { it.groupValues[1] }.toSet()
        return when {
            text == null -> "brak README.md"
            headings != SECTIONS -> "części $headings zamiast $SECTIONS"
            described != built -> "zależności $described zamiast $built"
            else -> null
        }
    }

    private fun section(text: String): String =
        text.substringAfter(delimiter = "## $DEPENDENCIES", missingDelimiterValue = "").substringBefore("\n## ")

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        const val DEPENDENCIES = "Zależności"

        val SECTIONS = listOf("Zadanie", "API", DEPENDENCIES, "Czego moduł nie robi", "Kiedy dodawać tutaj")

        const val HINT = "Opis modułu w README.md nie ma wszystkich części albo nie zgadza się z build.gradle.kts"

        val MODULE = Regex("\"(:[A-Za-z0-9:_-]+)\"")

        val PROJECT = Regex("""^\s*(?:api|implementation)\(project\("(:[^"]+)"\)\)""", RegexOption.MULTILINE)

        val HEADING = Regex("^## (.+)$", RegexOption.MULTILINE)

        val DEPENDENCY = Regex("^- `(:[^`]+)`", RegexOption.MULTILINE)

        val UNDESCRIBED =
            mapOf(
                ":app" to 1580,
                ":feature:map" to 1580,
                ":feature:capture" to 1580,
                ":feature:help" to 1580,
                ":detekt-rules" to 1580,
                ":systemtest" to 1580,
            )
    }
}
