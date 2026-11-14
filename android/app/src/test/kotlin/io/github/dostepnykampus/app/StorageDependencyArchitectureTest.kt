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
 * Od `:core:database` i `:core:network` zależą w `build.gradle.kts` tylko moduły z [ALLOWED], w każdej
 * konfiguracji, także testowej, i nigdy przez `api(…)`. Moduł funkcji bierze dane z `:core:data`.
 */
class StorageDependencyArchitectureTest {
    /** Moduły projektu zależą od bazy i sieci tylko według [ALLOWED]. */
    @Test
    fun infra_storage_only_in_data() {
        assertEquals(emptySet<String>(), violations(projectRoot()), HINT)
    }

    /** Zależność testowa modułu funkcji od sieci i `api` bazy łamią regułę, `implementation` w danych nie. */
    @Test
    fun infra_storage_rule_rejects(
        @TempDir root: File,
    ) {
        File(root, "settings.gradle.kts").writeText("""include(":core:data", ":core:data-testing", ":feature:x")""")
        module(root = root, path = "core/data", line = """implementation(project(":core:database"))""")
        module(root = root, path = "core/data-testing", line = """api(project(":core:database"))""")
        module(root = root, path = "feature/x", line = """testImplementation(project(":core:network"))""")
        assertEquals(
            setOf(":core:data-testing api :core:database", ":feature:x testImplementation :core:network"),
            violations(root),
        )
    }

    private fun module(root: File, path: String, line: String) {
        val dir = File(root, path).apply { mkdirs() }
        File(dir, "build.gradle.kts").writeText(
            """
                |dependencies {
                |    $line
                |}
                |
            """.trimMargin(),
        )
    }

    private fun violations(root: File): Set<String> =
        MODULE
            .findAll(File(root, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .flatMap { name ->
                dependencies(root, name)
                    .filter { (config, target) -> isForbidden(module = name, config = config, target = target) }
                    .map { (config, target) -> "$name $config $target" }
            }.toSet()

    private fun isForbidden(module: String, config: String, target: String): Boolean =
        target in ALLOWED && (config == API || module !in ALLOWED.getValue(target))

    private fun dependencies(root: File, module: String): Sequence<Pair<String, String>> {
        val build = File(root, module.removePrefix(":").replace(oldChar = ':', newChar = '/') + "/build.gradle.kts")
        return if (build.isFile) {
            PROJECT.findAll(build.readText()).map { it.groupValues[1] to it.groupValues[2] }
        } else {
            emptySequence()
        }
    }

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        const val API = "api"

        const val HINT = "Moduł zależy od bazy albo sieci; dane z :core:data, a te moduły tylko przez implementation"

        val MODULE = Regex("\"(:[A-Za-z0-9:_-]+)\"")

        val PROJECT = Regex("""^\s*(\w+)\(project\("(:[^"]+)"\)\)""", RegexOption.MULTILINE)

        val ALLOWED =
            mapOf(
                ":core:database" to setOf(":app", ":core:data", ":core:data-testing"),
                ":core:network" to setOf(":app", ":core:data", ":core:data-testing", ":systemtest"),
            )
    }
}
