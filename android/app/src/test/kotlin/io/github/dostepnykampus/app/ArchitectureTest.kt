/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {
    private val files = Konsist.scopeFromProject().files

    private val viewModels: List<KoClassDeclaration> =
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { declaration ->
                declaration.hasParentOf(ViewModel::class, AndroidViewModel::class, indirectParents = true) ||
                    declaration.name.endsWith("ViewModel")
            }

    @Test
    fun infra_a1_pure_core_no_android() {
        files
            .filter { it.inPackage("core.model") || it.inPackage("core.geo") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("android.") } }
    }

    @Test
    fun infra_a1_core_not_on_feature() {
        files
            .filter { it.inPackage("core") }
            .assertFalse { file -> file.imports.any { it.name.startsWith("$BASE.feature.") } }
    }

    @Test
    fun infra_a1_feature_not_on_other() {
        files
            .filter { it.inPackage("feature") }
            .assertFalse { file ->
                val own = "$BASE.feature." + file.packagePath().removePrefix("$BASE.feature.").substringBefore('.')
                file.imports.any { it.name.startsWith("$BASE.feature.") && !it.name.startsWith("$own.") }
            }
    }

    @Test
    fun infra_a1_ui_no_network_or_db() {
        files
            .filter { it.inPackage("feature") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("$BASE.core.network.") ||
                        import.name.startsWith("$BASE.core.database.")
                }
            }
    }

    @Test
    fun infra_a1_viewmodel_in_feature() {
        viewModels
            .assertFalse { !it.containingFile.inPackage("feature") }
    }

    @Test
    fun adr7_viewmodel_from_hilt() {
        viewModels
            .assertTrue(strict = true) { it.hasAnnotationWithName("HiltViewModel") }
    }

    @Test
    fun infra_a1_repository_in_data() {
        files
            .flatMap { it.classes() + it.interfaces() }
            .filter { it.name.endsWith("Repository") }
            .assertFalse { !it.containingFile.inPackage("core.data") }
    }

    private fun KoFileDeclaration.packagePath(): String = packagee?.name.orEmpty()

    private fun KoFileDeclaration.inPackage(relative: String): Boolean =
        packagePath() == "$BASE.$relative" || packagePath().startsWith("$BASE.$relative.")

    private companion object {
        const val BASE = "io.github.dostepnykampus"
    }
}
