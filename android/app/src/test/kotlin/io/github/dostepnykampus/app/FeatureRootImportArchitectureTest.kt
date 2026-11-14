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
 * Plik z podpakietu modułu funkcji nie importuje deklaracji z korzenia swojego modułu; wolno tylko klasę
 * zasobów `R`. To, co wspólne kilku podpakietom, leży w podpakiecie, od którego zależą pozostałe.
 */
class FeatureRootImportArchitectureTest {
    /** Podpakiety modułów funkcji nie importują korzenia swojego modułu. */
    @Test
    fun infra_feature_part_skips_root() {
        assertEquals(emptyList<String>(), violations(Konsist.scopeFromProduction().files), HINT)
    }

    /** Import korzenia z podpakietu łamie regułę; import `R`, innego podpakietu i podpakietu z korzenia nie. */
    @Test
    fun infra_feature_root_rejects(
        @TempDir fixtures: File,
    ) {
        source(fixtures = fixtures, name = "Root", pack = "feature.x", import = "feature.x.part.Part")
        source(fixtures = fixtures, name = "Part", pack = "feature.x.part", import = "feature.x.Root")
        source(fixtures = fixtures, name = "Res", pack = "feature.x.part", import = "feature.x.R")
        source(fixtures = fixtures, name = "Other", pack = "feature.x.other", import = "feature.x.part.Part")
        val files = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).files
        assertEquals(listOf("$BASE.feature.x.part.Part → $BASE.feature.x.Root"), violations(files))
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

    private fun violations(files: List<KoFileDeclaration>): List<String> =
        files
            .flatMap { file -> rootImports(file).map { "${file.packagee?.name}.${file.name} → $it" } }
            .sorted()

    private fun rootImports(file: KoFileDeclaration): List<String> {
        val root = SUBPACKAGE.matchEntire(file.packagee?.name.orEmpty())?.groupValues?.get(1)
        return file.imports
            .map { it.name }
            .filter { root != null && it.substringBeforeLast('.') == root && it.substringAfterLast('.') != RESOURCES }
    }

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val RESOURCES = "R"

        const val HINT = "Podpakiet funkcji importuje korzeń modułu; przenieś wspólny element do podpakietu"

        val SUBPACKAGE = Regex("""^(io\.github\.dostepnykampus\.feature\.\w+)\..+$""")
    }
}
