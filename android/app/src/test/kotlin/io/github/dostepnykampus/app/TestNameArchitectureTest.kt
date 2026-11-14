/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Nazwa funkcji z adnotacją `@Test` w każdym module Androida to camelCase od małej litery, bez podkreśleń,
 * spacji i słowa `test`. Moduły z naruszeniami są na liście wyjątków z numerem issue, a lista może tylko maleć.
 */
class TestNameArchitectureTest {
    /** Testy spoza modułów z listy wyjątków mają nazwy zgodne z regułą. */
    @Test
    fun infraNamesFollowRule() {
        val found = violations(Konsist.scopeFromTest().functions(includeNested = true))
        assertEquals(emptyMap<String, List<String>>(), found - EXCEPTIONS.keys, HINT)
    }

    /** Wpis listy wyjątków dla modułu bez naruszeń albo nieistniejącego czerwieni test. */
    @Test
    fun infraExceptionsShrink() {
        val found = violations(Konsist.scopeFromTest().functions(includeNested = true))
        assertEquals(emptySet<String>(), EXCEPTIONS.keys - found.keys, "Moduł spełnia regułę: usuń jego wpis")
    }

    /** Podkreślenie i słowo `test` łamią regułę; camelCase i „latest” nie. */
    @Test
    fun infraRejectsBadNames(
        @TempDir fixtures: File,
    ) {
        File(fixtures, "Sample.kt").writeText(
            listOf(
                "us8_newest_first",
                "testNewestFirst",
                "us8NewestTestFirst",
                "us8ShowsNewestSubmissionFirst",
                "us8KeepsLatestOrder",
            ).joinToString(separator = "\n", prefix = "class Sample {\n", postfix = "\n}") {
                "    @Test fun $it() = Unit"
            },
        )
        val functions = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).functions(includeNested = true)
        assertEquals(
            listOf("testNewestFirst", "us8NewestTestFirst", "us8_newest_first"),
            violations(functions).values.flatten(),
            "Złe nazwy z próbki",
        )
    }

    private fun violations(functions: List<KoFunctionDeclaration>): Map<String, List<String>> =
        functions
            .filter { function -> function.annotations.any { it.name == "Test" } }
            .filterNot { isValid(it.name) }
            .groupBy(keySelector = { it.containingFile.moduleName }, valueTransform = { it.name })
            .mapValues { (_, names) -> names.sorted() }
            .toSortedMap()

    private fun isValid(name: String): Boolean =
        CAMEL.matches(name) && name.split(UPPER).none { it.equals("test", ignoreCase = true) }

    private companion object {
        val CAMEL = Regex("[a-z][a-zA-Z0-9]*")

        val UPPER = Regex("(?=[A-Z])")

        const val HINT =
            "Nazwa testu: zdanie o zachowaniu w camelCase, bez podkreśleń i słowa „test”"

        val EXCEPTIONS =
            mapOf(
                "core/network" to "#2089",
                "feature/map" to "#2032",
            )
    }
}
