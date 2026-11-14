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
 * Nazwa klasy, obiektu ani interfejsu kodu produkcyjnego nie kończy się na `Util`, `Utils`, `Helper`,
 * `Helpers` ani `Manager`: kod trafia do typu nazwanego po swojej odpowiedzialności. Role frameworka,
 * np. `ViewModel` czy `Repository`, są dozwolone.
 */
class ClassNameArchitectureTest {
    /** Kod produkcyjny nie ma zbiorczych klas pomocniczych. */
    @Test
    fun infra_class_name_not_catch_all() {
        assertEquals(emptyList<String>(), violations(Konsist.scopeFromProduction()), HINT)
    }

    /** Nazwy `Utils`, `Helper` i `Manager` łamią regułę, rola `ViewModel` nie. */
    @Test
    fun infra_class_name_rejects(
        @TempDir fixtures: File,
    ) {
        File(fixtures, "Samples.kt").writeText(
            """
                |package sample
                |
                |object DateUtils
                |
                |class JsonHelper
                |
                |interface PhotoManager
                |
                |class MapViewModel
                |
            """.trimMargin(),
        )
        assertEquals(
            listOf("DateUtils", "JsonHelper", "PhotoManager"),
            violations(Konsist.scopeFromExternalDirectory(fixtures.absolutePath)),
        )
    }

    private fun violations(scope: KoScope): List<String> =
        (scope.classes().map { it.name } + scope.objects().map { it.name } + scope.interfaces().map { it.name })
            .filter { name -> CATCH_ALL.matches(name) }
            .sorted()

    private companion object {
        val CATCH_ALL = Regex(".*(Util|Utils|Helper|Helpers|Manager)")
        const val HINT = "Typ nazwany jak zbiorcza klasa pomocnicza; nazwij go po odpowiedzialności"
    }
}
