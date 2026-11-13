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
 * Każdy moduł z `settings.gradle.kts` ma kod produkcyjny w `src/main`. Moduł powstaje razem ze swoim kodem,
 * nie na zapas.
 */
class ModuleContentArchitectureTest {
    /** Moduły projektu mają kod produkcyjny. */
    @Test
    fun infra_module_has_production_code() {
        val root = projectRoot()
        assertEquals(emptyList<String>(), empty(root, modules(File(root, "settings.gradle.kts").readText())), HINT)
    }

    /** Moduł z samym plikiem budowania łamie regułę, moduł z plikiem Kotlin w `src/main` nie. */
    @Test
    fun infra_module_rule_rejects(
        @TempDir root: File,
    ) {
        File(root, "feature/empty").mkdirs()
        File(root, "feature/empty/build.gradle.kts").writeText("")
        val code = File(root, "feature/full/src/main/kotlin").apply { mkdirs() }
        File(code, "Full.kt").writeText("package $BASE.sample\nval full = 1")
        val settings = "include(\n    \":feature:empty\",\n    \":feature:full\",\n)"
        assertEquals(listOf(":feature:empty"), empty(root, modules(settings)))
    }

    private fun modules(settings: String): List<String> = MODULE.findAll(settings).map { it.groupValues[1] }.toList()

    private fun empty(
        root: File,
        modules: List<String>,
    ): List<String> =
        modules.filter { module ->
            File(root, module.removePrefix(":").replace(':', '/') + "/src/main")
                .walk()
                .none { it.isFile && it.extension in SOURCES }
        }

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val HINT = "Moduł nie ma kodu produkcyjnego; dodaj go razem z kodem albo usuń z settings.gradle.kts"

        val MODULE = Regex("\"(:[A-Za-z0-9:_-]+)\"")

        val SOURCES = setOf("kt", "java")
    }
}
