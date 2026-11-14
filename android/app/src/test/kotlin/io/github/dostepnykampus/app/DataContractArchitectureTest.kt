/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.provider.KoKDocProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Kontrakt `:core:data` to jego publiczne interfejsy: każdy z nich, także `fun interface` i `sealed interface`, oraz
 * każda jego publiczna funkcja i właściwość mają KDoc z niepustym opisem. Implementacje są `internal`.
 *
 * Wymagania: NFR-10
 */
class DataContractArchitectureTest {
    /** Publiczne interfejsy `:core:data` i ich publiczne składowe mają KDoc. */
    @Test
    fun infra_data_contract_documented() {
        val interfaces = Konsist.scopeFromProduction(moduleName = MODULE).interfaces(includeNested = true)
        assertEquals(emptyList<String>(), violations(interfaces), HINT)
    }

    /** Brak KDoc lub pusty opis interfejsu albo składowej łamie regułę; `internal` i `private` nie. */
    @Test
    fun infra_data_contract_rejects(
        @TempDir fixtures: File,
    ) {
        source(fixtures = fixtures, name = "Bare", body = BARE)
        source(fixtures = fixtures, name = "Blank", body = BLANK)
        source(fixtures = fixtures, name = "Repo", body = REPO)
        source(fixtures = fixtures, name = "Ok", body = OK)
        source(fixtures = fixtures, name = "Hidden", body = HIDDEN)
        val interfaces = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).interfaces(includeNested = true)
        val expected = listOf("$BASE.Bare", "$BASE.Blank", "$BASE.Repo.read", "$BASE.Repo.size")
        assertEquals(expected, violations(interfaces))
    }

    private fun source(fixtures: File, name: String, body: String) {
        File(fixtures, "$name.kt").writeText(
            """
            |package $BASE
            |
            |$body
            |
            """.trimMargin(),
        )
    }

    private fun violations(interfaces: List<KoInterfaceDeclaration>): List<String> =
        interfaces
            .asSequence()
            .filter { it.hasPublicOrDefaultModifier && packageOf(it).startsWith(BASE) }
            .flatMap { declared -> undocumented(declared) }
            .sorted()
            .toList()

    private fun undocumented(declared: KoInterfaceDeclaration): List<String> {
        val name = "${packageOf(declared)}.${declared.name}"
        val functions =
            declared
                .functions(includeNested = false, includeLocal = false)
                .filter { it.hasPublicOrDefaultModifier && !documented(it) }
                .map { "$name.${it.name}" }
        val properties =
            declared
                .properties(includeNested = false)
                .filter { it.hasPublicOrDefaultModifier && !documented(it) }
                .map { "$name.${it.name}" }
        return listOfNotNull(name.takeUnless { documented(declared) }) + functions + properties
    }

    private fun packageOf(declared: KoInterfaceDeclaration): String = declared.packagee?.name.orEmpty()

    private fun documented(declaration: KoKDocProvider): Boolean = declaration.kDoc?.description?.isNotBlank() == true

    private companion object {
        const val MODULE = "core/data"

        const val BASE = "io.github.dostepnykampus.core.data"

        val BARE =
            """
            |interface Bare {
            |    /** Czyta. */
            |    fun read()
            |}
            """.trimMargin()

        val BLANK =
            """
            |/** */
            |interface Blank
            """.trimMargin()

        val REPO =
            """
            |/** Repo. */
            |interface Repo {
            |    fun read(): Int
            |
            |    val size: Int
            |
            |    private fun hidden(): Int = 1
            |}
            """.trimMargin()

        val OK =
            """
            |/** Ok. */
            |fun interface Ok {
            |    /** Robi. */
            |    fun run()
            |}
            """.trimMargin()

        val HIDDEN =
            """
            |internal interface Hidden {
            |    fun hidden()
            |}
            """.trimMargin()

        const val HINT = "Publiczny interfejs core:data albo jego składowa bez KDoc; opisz kontrakt"
    }
}
