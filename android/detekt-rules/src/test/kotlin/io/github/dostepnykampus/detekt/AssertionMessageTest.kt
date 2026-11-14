/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

/** Reguła [AssertionMessage] na krótkich źródłach z prawdziwymi sygnaturami JUnit 4 i JUnit 5. */
class AssertionMessageTest {
    private val rule = AssertionMessage(Config.empty)

    @Test
    fun `JUnit 5 bez komunikatu`() {
        val calls =
            listOf(
                "assertEquals(1, 2)",
                "assertTrue(true)",
                "assertNull(null)",
                "assertThrows(E::class.java) {}",
            )
        assertEquals(List(calls.size) { it + 1 }, lines(JUPITER, calls))
    }

    @Test
    fun `JUnit 5 z komunikatem`() {
        val calls =
            listOf(
                "assertEquals(1, 2, \"opis\")",
                "assertTrue(true) { \"opis\" }",
                "assertNull(null, \"opis\")",
            )
        assertEquals(emptyList<Int>(), lines(JUPITER, calls))
    }

    @Test
    fun `fail z komunikatem i bez`() {
        assertEquals(listOf(2), lines(JUPITER, listOf("fail<Unit>(\"opis\")", "fail<Unit>()")))
    }

    @Test
    fun `assertAll bez komunikatu`() {
        assertEquals(emptyList<Int>(), lines(JUPITER, listOf("assertAll({ }, { })")))
    }

    @Test
    fun `JUnit 4 bez komunikatu`() {
        val calls = listOf("assertEquals(1, 2)", "assertTrue(true)", "assertNull(null)")
        assertEquals(listOf(1, 2, 3), lines(JUNIT4, calls))
    }

    @Test
    fun `JUnit 4 z komunikatem`() {
        val calls = listOf("assertEquals(\"opis\", 1, 2)", "assertTrue(\"opis\", true)", "fail(\"opis\")")
        assertEquals(emptyList<Int>(), lines(JUNIT4, calls))
    }

    @Test
    fun `wywołanie spoza JUnit`() {
        val source = listOf("fun assertEquals(a: Int, b: Int) = Unit", "fun test() {", "    assertEquals(1, 2)", "}")
        val findings = rule.lintWithContext(environment, source.joinToString(separator = "\n"))
        assertEquals(emptyList<Int>(), findings.map { it.entity.location.source.line })
    }

    private fun lines(import: String, calls: List<String>): List<Int> {
        val source =
            buildString {
                appendLine("import $import")
                appendLine("class E : Exception()")
                appendLine("fun test() {")
                calls.forEach { appendLine("    $it") }
                appendLine("}")
            }
        return rule.lintWithContext(environment, source).map { it.entity.location.source.line - HEADER }
    }

    private companion object {
        const val JUPITER = "org.junit.jupiter.api.Assertions.*"
        const val JUNIT4 = "org.junit.Assert.*"
        const val HEADER = 3
        val environment =
            createEnvironment(
                System
                    .getProperty("java.class.path")
                    .split(File.pathSeparator)
                    .filter { "junit" in it || "opentest4j" in it || "apiguardian" in it }
                    .map(Path::of),
            )
    }
}
