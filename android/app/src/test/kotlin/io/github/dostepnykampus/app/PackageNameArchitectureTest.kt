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
 * Każdy segment pakietu pod korzeniem modułu Androida jest tematem lub ścieżką ze słownika
 * `docs/docs/architektura/tematy.tsv` albo nazwą warstwy z [LAYERS]. Pod korzeniem leżą najwyżej trzy segmenty,
 * a moduł nazwany od tematu nie powtarza go w pierwszym segmencie. Pakiety łamiące regułę są na liście
 * wyjątków z numerem issue, a lista może tylko maleć.
 */
class PackageNameArchitectureTest {
    /** Pakiety spoza listy wyjątków mają nazwy ze słownika i mieszczą się w trzech segmentach. */
    @Test
    fun infra_package_names_in_dict() {
        val found = problems(Konsist.scopeFromProduction().files, dictionary())
        assertEquals(emptyMap<String, String>(), found - EXCEPTIONS.keys, HINT)
    }

    /** Wpis listy wyjątków dla pakietu, który spełnia regułę albo już nie istnieje, czerwieni test. */
    @Test
    fun infra_package_names_shrink() {
        val found = problems(Konsist.scopeFromProduction().files, dictionary())
        assertEquals(emptySet<String>(), EXCEPTIONS.keys - found.keys)
    }

    /** Nazwa spoza słownika, zbyt głęboki pakiet i powtórzony temat modułu łamią regułę; warstwa i temat nie. */
    @Test
    fun infra_package_names_reject(
        @TempDir fixtures: File,
    ) {
        listOf(
            "feature.x.report.form",
            "feature.x.navigation",
            "core.geo.building",
            "core.geo.geo",
            "core.x.util",
            "core.x.submission.sync.own.pending",
            "core.x.submissions",
            "core.x",
            "other.util",
        ).forEachIndexed { index, pack ->
            File(fixtures, "Sample$index.kt").writeText("package $BASE.$pack\nval sample$index = $index")
        }
        val files = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).files
        assertEquals(
            mapOf(
                "$BASE.core.geo.geo" to "geo",
                "$BASE.core.x.submission.sync.own.pending" to "głębokość",
                "$BASE.core.x.submissions" to "submissions",
                "$BASE.core.x.util" to "util",
            ),
            problems(files, setOf("report", "form", "geo", "building", "submission", "sync", "own", "pending")),
        )
    }

    private fun problems(files: List<KoFileDeclaration>, names: Set<String>): Map<String, String> =
        files
            .asSequence()
            .mapNotNull { it.packagee?.name }
            .distinct()
            .mapNotNull { pack -> problem(pack, names)?.let { pack to it } }
            .toMap()
            .toSortedMap()

    private fun problem(pack: String, names: Set<String>): String? {
        val match = MODULE.matchEntire(pack) ?: return null
        val module = match.groupValues[MODULE_NAME]
        val parts = match.groupValues[REST].split('.').filter { it.isNotEmpty() }
        return when {
            parts.size > MAX_SEGMENTS -> "głębokość"
            parts.firstOrNull() == module -> module
            else -> parts.firstOrNull { it !in names && it !in LAYERS }
        }
    }

    private fun dictionary(): Set<String> =
        File(projectRoot(), "docs/docs/architektura/tematy.tsv")
            .readLines()
            .asSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { it.substringBefore('\t') }
            .toSet()

    private fun projectRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "docs/docs/architektura/tematy.tsv").isFile }

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val MAX_SEGMENTS = 3

        const val MODULE_NAME = 2

        const val REST = 3

        val MODULE = Regex("""${Regex.escape(BASE)}\.(core|feature)\.([a-z0-9]+)((?:\.[a-z0-9]+)*)""")

        val LAYERS = setOf("navigation", "di")

        const val HINT =
            "Pakiet ma nazwę spoza słownika tematów i warstw, leży za głęboko albo powtarza temat modułu"

        val EXCEPTIONS =
            mapOf(
                "$BASE.core.data.barriers" to "#1680",
                "$BASE.core.data.buildings" to "#1680",
                "$BASE.core.data.photos" to "#1680",
                "$BASE.core.data.submissions" to "#1680",
                "$BASE.core.data.map" to "#1878",
                "$BASE.core.data.network" to "#1878",
                "$BASE.core.data.server" to "#1878",
                "$BASE.core.data.start" to "#1878",
                "$BASE.feature.map.controls" to "#1688",
                "$BASE.feature.map.data" to "#1689",
            )
    }
}
