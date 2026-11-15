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
 * Część „Pakiety” ma linię `- \`pakiet\` — opis` na każdy pakiet produkcyjny modułu i żadnej więcej.
 * Moduły bez opisu są na liście wyjątków z numerem issue, a lista może tylko maleć.
 */
class ModuleReadmeArchitectureTest {
    /** Moduły spoza listy wyjątków mają pełny opis zgodny z zależnościami. */
    @Test
    fun matchesReadmeToBuild() {
        val found = problems(projectRoot())
        assertEquals(emptyMap<String, String>(), found - UNDESCRIBED.keys, HINT)
    }

    /** Wpis listy wyjątków dla modułu, który ma już pełny opis, czerwieni test. */
    @Test
    fun shrinksReadmeList() {
        assertEquals(emptySet<String>(), UNDESCRIBED.keys - problems(projectRoot()).keys)
    }

    /** Moduły spoza listy wyjątków opisują w części „Pakiety” dokładnie swoje pakiety produkcyjne. */
    @Test
    fun matchesReadmePackages() {
        val found = packageProblems(projectRoot())
        assertEquals(emptyMap<String, String>(), found - NO_PACKAGES.keys, PACKAGES_HINT)
    }

    /** Wpis listy wyjątków dla modułu, który ma już poprawną część „Pakiety”, czerwieni test. */
    @Test
    fun shrinksPackageList() {
        assertEquals(emptySet<String>(), NO_PACKAGES.keys - packageProblems(projectRoot()).keys)
    }

    /** Brak części „Pakiety”, pakiet bez linii i linia o nieistniejącym pakiecie łamią regułę. */
    @Test
    fun rejectsReadmePackages(
        @TempDir root: File,
    ) {
        File(root, "settings.gradle.kts")
            .writeText("""include(":core:ok", ":core:none", ":core:missing", ":core:stale")""")
        val ok = listOf("core.ok", "core.ok.sub")
        packages(root = root, path = "core/ok", sources = ok, described = ok)
        packages(root = root, path = "core/none", sources = listOf("core.none"), described = null)
        val missing = listOf("core.missing", "core.missing.sub")
        packages(root = root, path = "core/missing", sources = missing, described = listOf("core.missing"))
        val stale = listOf("core.stale", "core.gone")
        packages(root = root, path = "core/stale", sources = listOf("core.stale"), described = stale)
        assertEquals(setOf(":core:none", ":core:missing", ":core:stale"), packageProblems(root).keys)
    }

    /** Brak części opisu i zależność spoza `build.gradle.kts` łamią regułę, pełny zgodny opis nie. */
    @Test
    fun rejectsReadme(
        @TempDir root: File,
    ) {
        File(root, "settings.gradle.kts").writeText("""include(":core:ok", ":core:partial", ":core:wrong")""")
        module(
            root = root,
            path = "core/ok",
            built = listOf(":core:base"),
            described = listOf(":core:base"),
            sections = SECTIONS,
        )
        module(
            root = root,
            path = "core/partial",
            built = emptyList(),
            described = emptyList(),
            sections = SECTIONS - "API",
        )
        module(
            root = root,
            path = "core/wrong",
            built = listOf(":core:base"),
            described = listOf(":core:other"),
            sections = SECTIONS,
        )
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
        File(dir, "README.md").writeText(listOf("# :$path", text).joinToString(separator = "\n\n", postfix = "\n"))
    }

    private fun packages(root: File, path: String, sources: List<String>, described: List<String>?) {
        val dir = File(root, path).apply { mkdirs() }
        sources.forEach { name ->
            val relative = name.replace(oldChar = '.', newChar = '/')
            val source = File(dir, "src/main/kotlin/$PREFIX_PATH/$relative").apply { mkdirs() }
            File(source, "A.kt").writeText("class A")
        }
        val text =
            buildString {
                appendLine("# :$path")
                described?.let { names ->
                    appendLine()
                    appendLine("## $PACKAGES")
                    appendLine()
                    names.forEach { appendLine("- `$it` — Opis.") }
                }
                appendLine()
                appendLine("## Zadanie")
                appendLine()
                appendLine("Treść.")
            }
        File(dir, "README.md").writeText(text)
    }

    private fun packageProblems(root: File): Map<String, String> =
        MODULE
            .findAll(File(root, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .mapNotNull { name -> packageProblem(File(root, path(name)))?.let { name to it } }
            .toMap()

    private fun packageProblem(dir: File): String? {
        val text = File(dir, "README.md").takeIf(File::isFile)?.readText()
        val produced = productionPackages(dir)
        val described =
            PACKAGE_LINE
                .findAll(section(text = text.orEmpty(), name = PACKAGES))
                .map { it.groupValues[1] }
                .toSet()
        return when {
            text == null || "## $PACKAGES" !in text -> "brak części $PACKAGES"
            described != produced -> "pakiety $described zamiast $produced"
            else -> null
        }
    }

    private fun productionPackages(dir: File): Set<String> =
        listOf("kotlin", "java")
            .asSequence()
            .map { File(dir, "src/main/$it") }
            .filter(File::isDirectory)
            .flatMap(::sourcePackages)
            .map { it.replace(oldChar = '/', newChar = '.').removePrefix("$PREFIX.") }
            .toSet()

    private fun sourcePackages(base: File): Sequence<String> =
        base
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.relativeTo(base).invariantSeparatorsPath.substringBeforeLast('/', "") }

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
        val headings =
            HEADING
                .findAll(text.orEmpty())
                .map { it.groupValues[1].trim() }
                .filter { it != PACKAGES }
                .toList()
        val described =
            DEPENDENCY
                .findAll(section(text = text.orEmpty(), name = DEPENDENCIES))
                .map { it.groupValues[1] }
                .toSet()
        return when {
            text == null -> "brak README.md"
            headings != SECTIONS -> "części $headings zamiast $SECTIONS"
            described != built -> "zależności $described zamiast $built"
            else -> null
        }
    }

    private fun section(text: String, name: String): String =
        text.substringAfter(delimiter = "## $name", missingDelimiterValue = "").substringBefore("\n## ")

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        const val DEPENDENCIES = "Zależności"

        const val PACKAGES = "Pakiety"

        const val PREFIX = "io.github.dostepnykampus"

        const val PREFIX_PATH = "io/github/dostepnykampus"

        val SECTIONS = listOf("Zadanie", "API", DEPENDENCIES, "Czego moduł nie robi", "Kiedy dodawać tutaj")

        const val HINT = "Opis modułu w README.md nie ma wszystkich części albo nie zgadza się z build.gradle.kts"

        val MODULE = Regex("\"(:[A-Za-z0-9:_-]+)\"")

        val PROJECT = Regex("""^\s*(?:api|implementation)\(project\("(:[^"]+)"\)\)""", RegexOption.MULTILINE)

        val HEADING = Regex("^## (.+)$", RegexOption.MULTILINE)

        val DEPENDENCY = Regex("^- `(:[^`]+)`", RegexOption.MULTILINE)

        const val PACKAGES_HINT = "Część Pakiety w README.md nie zgadza się z pakietami produkcyjnymi modułu"

        val PACKAGE_LINE = Regex("^- `([a-z][A-Za-z0-9_.]*)`", RegexOption.MULTILINE)

        val UNDESCRIBED = emptyMap<String, Int>()

        val NO_PACKAGES =
            mapOf(
                ":core:model" to 1829,
                ":core:geo" to 1829,
                ":core:location" to 1829,
                ":core:database" to 1829,
                ":core:network" to 1829,
                ":core:data" to 1829,
                ":core:designsystem" to 1829,
                ":app" to 1830,
                ":feature:map" to 1830,
                ":feature:capture" to 1830,
                ":feature:help" to 1830,
                ":detekt-rules" to 1830,
                ":systemtest" to 1830,
            )
    }
}
