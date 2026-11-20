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
 * Pakiety kodu produkcyjnego nie tworzą cyklu importów: gdy pakiet importuje deklarację z drugiego pakietu, drugi
 * nie importuje niczego z pierwszego, ani wprost, ani przez inne pakiety. Klasy generowane [GENERATED] się nie
 * liczą. Cykle do usunięcia są na liście wyjątków z numerem issue, a lista może tylko maleć.
 */
class PackageCycleArchitectureTest {
    /** Pakiety spoza listy wyjątków nie tworzą cyklu. */
    @Test
    fun keepsPackagesAcyclic() {
        val found = cycles(Konsist.scopeFromProduction().files)
        assertEquals(emptySet<Set<String>>(), found - CYCLES.keys, HINT)
    }

    /** Wpis listy wyjątków dla cyklu, którego już nie ma, czerwieni test. */
    @Test
    fun shrinksCycleList() {
        assertEquals(emptySet<Set<String>>(), CYCLES.keys - cycles(Konsist.scopeFromProduction().files))
    }

    /**
     * Trzy pakiety importujące się po kolei w kółko tworzą jeden cykl; import w jedną stronę, import funkcji
     * i import klasy zasobów `R` korzenia z podpakietu cyklu nie tworzą.
     */
    @Test
    fun rejectsCycle(
        @TempDir fixtures: File,
    ) {
        source(fixtures = fixtures, name = "A", pack = "x.a", import = "x.b.B")
        source(fixtures = fixtures, name = "B", pack = "x.b", import = "x.c.joined")
        source(fixtures = fixtures, name = "C", pack = "x.c", import = "x.a.A")
        source(fixtures = fixtures, name = "D", pack = "x.d", import = "x.a.A")
        source(fixtures = fixtures, name = "Root", pack = "y", import = "y.part.Part")
        source(fixtures = fixtures, name = "Part", pack = "y.part", import = "y.R")
        val files = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).files
        assertEquals(setOf(setOf("x.a", "x.b", "x.c")), cycles(files))
    }

    private fun source(fixtures: File, name: String, pack: String, import: String) {
        File(fixtures, "$name.kt").writeText(
            """
                |package $BASE.$pack
                |
                |import $BASE.$import
                |
                |val $name = 1
                |
            """.trimMargin(),
        )
    }

    private fun cycles(files: List<KoFileDeclaration>): Set<Set<String>> {
        val edges = imports(files)
        val reach = edges.keys.associateWith { reachable(start = it, edges = edges) }
        return edges.keys
            .map { from -> reach.getValue(from).filter { from in reach[it].orEmpty() }.toSet() + from }
            .filter { it.size > 1 }
            .map { cycle -> cycle.map { it.removePrefix("$BASE.") }.toSet() }
            .toSet()
    }

    private fun imports(files: List<KoFileDeclaration>): Map<String, Set<String>> =
        files
            .filter { it.packagee?.name.orEmpty().startsWith("$BASE.") }
            .groupBy(
                keySelector = { it.packagee?.name.orEmpty() },
                valueTransform = { file -> file.imports.map { it.name } },
            )
            .mapValues { (from, names) ->
                names
                    .flatten()
                    .filter { it.startsWith("$BASE.") && it.substringAfterLast('.') !in GENERATED }
                    .map(::packageOf)
                    .filter { it != from }
                    .toSet()
            }

    private fun packageOf(name: String): String {
        val parts = name.split('.')
        val pack = parts.takeWhile { it.first().isLowerCase() }
        return (if (pack.size == parts.size) pack.dropLast(1) else pack).joinToString(separator = ".")
    }

    private fun reachable(start: String, edges: Map<String, Set<String>>): Set<String> {
        val seen = mutableSetOf<String>()
        val queue = ArrayDeque(edges[start].orEmpty())
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (seen.add(next)) queue.addAll(edges[next].orEmpty())
        }
        return seen
    }

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val HINT = "Pakiety importują się w kółko; wspólną część przenieś do pakietu, od którego zależą oba"

        val GENERATED = setOf("R", "BuildConfig")

        val CYCLES =
            mapOf(
                setOf("app", "app.navigation") to "#3538",
                setOf(
                    "feature.capture.location",
                    "feature.capture.measure",
                    "feature.capture.quick",
                    "feature.capture.report",
                    "feature.capture.report.edit",
                    "feature.capture.report.form",
                ) to "#3538",
                setOf(
                    "core.network",
                    "core.network.auth",
                    "core.network.building",
                    "core.network.inspection",
                    "core.network.submission",
                    "core.network.sync",
                ) to "#3539",
                setOf(
                    "core.data.account",
                    "core.data.di",
                    "core.data.submission.pending",
                    "core.data.submission.sync",
                ) to "#3540",
                setOf("core.geo", "core.geo.building") to "#3541",
            )
    }
}
