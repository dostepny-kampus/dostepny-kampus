/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.provider.KoContainingDeclarationProvider
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.KoNullableTypeProvider
import com.lemonappdev.konsist.api.provider.KoParametersProvider
import com.lemonappdev.konsist.api.provider.KoPrimaryConstructorProvider
import com.lemonappdev.konsist.api.provider.KoReceiverTypeProvider
import com.lemonappdev.konsist.api.provider.KoReturnProvider
import com.lemonappdev.konsist.api.provider.KoSecondaryConstructorsProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Publiczna deklaracja `:core:data` spoza wiązań Hilt (`di`) nie ma w sygnaturze typu z `:core:network`: moduł
 * funkcji wywołuje ją bez typów klienta HTTP. Odpowiedź serwera to `ServerReply`.
 *
 * Wymagania: NFR-10
 */
class DataApiArchitectureTest {
    /** Publiczne API `:core:data` nie ma typów `:core:network`. */
    @Test
    fun infra_data_api_skips_network() {
        val files = Konsist.scopeFromProduction(moduleName = MODULE).files
        assertEquals(emptyList<String>(), violations(files), HINT)
    }

    /** Typ sieci w publicznej sygnaturze łamie regułę; w wewnętrznej, w konstruktorze `internal` i w `di` nie. */
    @Test
    fun infra_data_api_rule_rejects(
        @TempDir fixtures: File,
    ) {
        source(fixtures = fixtures, name = "Open", pack = "x", body = "class Open(val reply: ApiResult<Int>)")
        source(fixtures = fixtures, name = "Read", pack = "x", body = "fun read(): ApiResult<Int>? = null")
        source(fixtures = fixtures, name = "Hidden", pack = "x", body = "internal class Hidden(val r: ApiResult<Int>)")
        source(fixtures = fixtures, name = "Inner", pack = "x", body = "class Inner internal constructor(r: ApiResult<Int>)")
        source(fixtures = fixtures, name = "Provide", pack = "di", body = "fun provide(r: ApiResult<Int>): Int = 1")
        val files = Konsist.scopeFromExternalDirectory(fixtures.absolutePath).files
        assertEquals(listOf("$BASE.x.Open: ApiResult", "$BASE.x.read: ApiResult"), violations(files))
    }

    private fun source(fixtures: File, name: String, pack: String, body: String) {
        File(fixtures, "$name.kt").writeText(
            """
                |package $BASE.$pack
                |
                |import $NETWORK.ApiResult
                |
                |$body
                |
            """.trimMargin(),
        )
    }

    private fun violations(files: List<KoFileDeclaration>): List<String> =
        files
            .filter { it.packagee?.name.orEmpty().startsWith(BASE) && !isBinding(it) }
            .flatMap { file -> fileViolations(file) }
            .sorted()

    private fun isBinding(file: KoFileDeclaration): Boolean = file.packagee?.name.orEmpty().startsWith("$BASE.di")

    private fun fileViolations(file: KoFileDeclaration): List<String> {
        val network = file.imports.map { it.name }.filter { it.startsWith("$NETWORK.") }.map { it.substringAfterLast('.') }
        val pack = file.packagee?.name.orEmpty()
        return file
            .declarations(includeNested = true, includeLocal = false)
            .filter { isPublic(it) }
            .flatMap { declaration ->
                val types = signatureTypes(declaration)
                network
                    .filter { name -> types.any { Regex("""\b$name\b""").containsMatchIn(it) } }
                    .map { name -> "$pack.${(declaration as? KoNameProvider)?.name}: $name" }
            }
    }

    private fun isPublic(declaration: KoBaseDeclaration): Boolean {
        val own = (declaration as? KoVisibilityModifierProvider)?.hasPublicOrDefaultModifier ?: true
        val parent = (declaration as? KoContainingDeclarationProvider)?.containingDeclaration
        return own && (parent == null || parent is KoFileDeclaration || isPublic(parent))
    }

    private fun signatureTypes(declaration: KoBaseDeclaration): List<String> =
        buildList {
            (declaration as? KoParametersProvider)?.parameters?.forEach { add(it.type.text) }
            (declaration as? KoReturnProvider)?.returnType?.let { add(it.text) }
            (declaration as? KoReceiverTypeProvider)?.receiverType?.let { add(it.text) }
            (declaration as? KoNullableTypeProvider)?.type?.let { add(it.text) }
            (declaration as? KoPrimaryConstructorProvider)
                ?.primaryConstructor
                ?.takeIf { it.hasPublicOrDefaultModifier }
                ?.parameters
                ?.forEach { add(it.type.text) }
            (declaration as? KoSecondaryConstructorsProvider)
                ?.secondaryConstructors
                ?.filter { it.hasPublicOrDefaultModifier }
                ?.forEach { constructor -> constructor.parameters.forEach { add(it.type.text) } }
        }

    private companion object {
        const val MODULE = "core/data"

        const val BASE = "io.github.dostepnykampus.core.data"

        const val NETWORK = "io.github.dostepnykampus.core.network"

        const val HINT = "Publiczne API core:data ma typ core:network; użyj ServerReply albo typu core:data"
    }
}
