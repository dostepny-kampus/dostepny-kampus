/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Moduły funkcji testuje się przez fejki: ani testy w `feature/…/src/test*` i `src/androidTest`, ani
 * `build.gradle.kts` modułu funkcji nie sięgają po MockK ani Mockito.
 */
class MockFreeArchitectureTest {
    /** Moduły funkcji projektu nie mają mocków w testach ani w zależnościach. */
    @Test
    fun forbidsMocksInFeature() {
        assertEquals(emptySet<String>(), violations(projectRoot()), HINT)
    }

    /** Import MockK w teście i zależność od Mockito łamią regułę w module funkcji, a w `core` nie. */
    @Test
    fun rejectsMock(
        @TempDir root: File,
    ) {
        listOf(
            "feature/x/src/test/kotlin/A.kt" to
                "import io.mockk.mockk\n",
            "feature/x/src/main/kotlin/B.kt" to
                "import org.mockito.Mockito\n",
            "feature/y/build.gradle.kts" to
                "dependencies { testImplementation(libs.mockito.kotlin) }\n",
            "feature/y/src/androidTest/kotlin/C.kt" to
                "import org.mockito.kotlin.mock\n",
            "feature/z/src/test/kotlin/D.kt" to
                "import io.github.dostepnykampus.core.data.Fake\n",
            "core/data/src/test/kotlin/E.kt" to
                "import io.mockk.every\n",
        ).forEach { (path, text) -> write(root = root, path = path, text = text) }
        assertEquals(
            setOf(
                "feature/x/src/test/kotlin/A.kt",
                "feature/y/build.gradle.kts",
                "feature/y/src/androidTest/kotlin/C.kt",
            ),
            violations(root),
        )
    }

    private fun write(root: File, path: String, text: String) {
        File(root, path).apply { parentFile?.mkdirs() }.writeText(text)
    }

    private fun violations(root: File): Set<String> =
        File(root, "feature")
            .walkTopDown()
            .onEnter { it.name != "build" }
            .filter { it.isFile && isMocked(root, it) }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toSet()

    private fun isMocked(root: File, file: File): Boolean {
        val path = file.relativeTo(root).invariantSeparatorsPath
        return when {
            file.name == "build.gradle.kts" -> DEPENDENCY.containsMatchIn(file.readText())
            file.extension in SOURCES && TEST_SOURCE.containsMatchIn(path) -> IMPORT.containsMatchIn(file.readText())
            else -> false
        }
    }

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }

    private companion object {
        const val HINT = "Test modułu funkcji używa mocka; zamiast niego fejk"

        val SOURCES = setOf("kt", "java")

        val TEST_SOURCE = Regex("""/src/(test\w*|androidTest\w*)/""")

        val IMPORT = Regex("""^\s*import\s+(io\.mockk|org\.mockito)\b""", RegexOption.MULTILINE)

        val DEPENDENCY = Regex("""\b(mockk|mockito)\b""", RegexOption.IGNORE_CASE)
    }
}
