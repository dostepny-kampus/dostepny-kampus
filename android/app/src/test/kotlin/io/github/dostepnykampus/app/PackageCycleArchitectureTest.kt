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
        val gone = CYCLES.keys - cycles(Konsist.scopeFromProduction().files)
        assertEquals(emptySet<Set<String>>(), gone, "Usuń z listy wyjątków cykle, których już nie ma")
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
        assertEquals(setOf(setOf("x.a", "x.b", "x.c")), cycles(files), "Cykl tworzą tylko pakiety x.a, x.b i x.c")
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
        val found = mutableSetOf<Set<String>>()
        for ((from, targets) in reach) {
            val cycle = targets.filterTo(mutableSetOf(from)) { from in reach[it].orEmpty() }
            if (cycle.size > 1) found += cycle.mapTo(mutableSetOf()) { it.removePrefix("$BASE.") }
        }
        return found
    }

    private fun imports(files: List<KoFileDeclaration>): Map<String, Set<String>> {
        val edges = mutableMapOf<String, MutableSet<String>>()
        for (file in files) {
            val from = file.packagee?.name.orEmpty()
            val targets = file.imports.mapNotNull { target(it.name) }.toSet() - from
            if (from.startsWith("$BASE.")) edges.getOrPut(from) { mutableSetOf() } += targets
        }
        return edges
    }

    private fun target(name: String): String? =
        if (name.startsWith("$BASE.") && name.substringAfterLast('.') !in GENERATED) packageOf(name) else null

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
