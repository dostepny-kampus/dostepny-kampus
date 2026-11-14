/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Klasa danych w `:core:model` i `:core:data` jest niezmienna: jej właściwości, w konstruktorze i w ciele,
 * są `val`.
 */
class DataClassValArchitectureTest {
    /** Klasy danych modułów modelu i danych mają tylko `val`. */
    @Test
    fun infra_data_class_only_val() {
        assertEquals(emptyList<String>(), violations(Konsist.scopeFromProduction(), MODULES), HINT)
    }

    /** Właściwość `var` w konstruktorze i w ciele łamie regułę; zwykła klasa z `var` nie. */
    @Test
    fun infra_data_class_rejects_var(
        @TempDir fixtures: File,
    ) {
        File(fixtures, "Samples.kt").writeText(
            """
                |package sample
                |
                |data class Point(val lat: Double, var lon: Double)
                |
                |data class Note(val text: String) {
                |    var seen: Boolean = false
                |}
                |
                |data class Area(val name: String)
                |
                |class Counter(var count: Int)
                |
            """.trimMargin(),
        )
        assertEquals(
            listOf("Note.seen", "Point.lon"),
            violations(Konsist.scopeFromExternalDirectory(fixtures.absolutePath), listOf("")),
        )
    }

    private fun violations(
        scope: KoScope,
        modules: List<String>,
    ): List<String> =
        scope
            .classes()
            .filter { type -> type.hasDataModifier && modules.any { type.path.replace('\\', '/').contains(it) } }
            .flatMap { type ->
                val parameters = type.primaryConstructor?.parameters.orEmpty().filter { it.isVar }
                val properties = type.properties(includeNested = false).filter { it.isVar }
                (parameters.map { it.name } + properties.map { it.name }).map { "${type.name}.$it" }
            }.distinct().sorted()

    private companion object {
        val MODULES = listOf("/core/model/src/main/", "/core/data/src/main/")
        const val HINT = "Klasa danych ma właściwość var; zmiana obiektu to nowa kopia przez copy()"
    }
}
