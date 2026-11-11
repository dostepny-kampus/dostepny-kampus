/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

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

    /** Kod ekranów bez sieci i bazy; testy modułu feature mogą budować repozytorium z wynikami sieci (#874). */
    @Test
    fun infra_a1_ui_no_network_or_db() {
        Konsist
            .scopeFromProduction()
            .files
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

    /** Moduły funkcji biorą klienta HTTP z `core:network` przez Hilt, bez własnych importów HTTP (#888). */
    @Test
    fun infra_a1_feature_no_own_http() {
        assertEquals(emptySet<String>(), httpFiles(featureFiles()) - OWN_HTTP.keys, HTTP_HINT)
    }

    /** #888: wpis listy wyjątków bez importu HTTP trzeba usunąć, więc lista może tylko maleć. */
    @Test
    fun infra_a1_http_list_shrinks() {
        assertEquals(emptySet<String>(), OWN_HTTP.keys - httpFiles(featureFiles()))
    }

    /** #888: pełna nazwa bez importu też wymaga biblioteki HTTP w zależnościach modułu funkcji. */
    @Test
    fun infra_a1_build_without_http() {
        val allowed = OWN_HTTP.keys.map { it.removePrefix("$BASE.feature.").substringBefore('.') }.toSet()
        val modules =
            File("../feature")
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter { HTTP_DEPENDENCY.containsMatchIn(File(it, "build.gradle.kts").readTextOrEmpty()) }
                .map { it.name }
                .toSet()
        assertEquals(emptySet<String>(), modules - allowed, HTTP_HINT)
    }

    /**
     * #888: import klienta HTTP w module funkcji łamie regułę, także z gwiazdką. Konsist podaje import z gwiazdką
     * bez niej, z `isWildcard`, więc reguła dokleja ją z powrotem.
     */
    @Test
    fun infra_a1_own_http_rule_rejects(
        @TempDir fixtures: File,
    ) {
        val directory = File(fixtures, "map").apply { mkdirs() }
        val client = fixture(imports = "import okhttp3.OkHttpClient", body = "val c = OkHttpClient()")
        File(directory, "Tiles.kt").writeText(client)
        File(directory, "Url.kt").writeText(fixture(imports = "import java.net.*", body = "val u = URL(\"https://x\")"))
        File(directory, "Plain.kt").writeText(fixture(imports = "", body = "val a = 1"))
        val files = Konsist.scopeFromExternalDirectory(directory.absolutePath).files
        assertEquals(setOf("$BASE.feature.map.Tiles", "$BASE.feature.map.Url"), httpFiles(files))
    }

    private fun fixture(imports: String, body: String): String =
        listOf("package $BASE.feature.map", imports, body).joinToString(separator = System.lineSeparator())

    private fun File.readTextOrEmpty(): String = takeIf(File::exists)?.readText().orEmpty()

    private fun featureFiles(): List<KoFileDeclaration> =
        Konsist.scopeFromProduction().files.filter { it.inPackage("feature") }

    private fun httpFiles(files: List<KoFileDeclaration>): Set<String> =
        files
            .filter { file -> file.imports.any { it.importsHttp() } }
            .map { "${it.packagePath()}.${it.name}" }
            .toSet()

    private fun KoImportDeclaration.importsHttp(): Boolean {
        val full = if (isWildcard) "$name.*" else name
        return HTTP_IMPORTS.any { full.startsWith(it) }
    }

    private fun KoFileDeclaration.packagePath(): String = packagee?.name.orEmpty()

    private fun KoFileDeclaration.inPackage(relative: String): Boolean =
        packagePath() == "$BASE.$relative" || packagePath().startsWith("$BASE.$relative.")

    private companion object {
        const val BASE = "io.github.dostepnykampus"

        const val HTTP_HINT = "Moduł funkcji buduje własnego klienta HTTP: weź klienta z core:network przez Hilt"

        val HTTP_IMPORTS = listOf("okhttp3.", "retrofit2.", "java.net.HttpURLConnection", "java.net.URL", "java.net.*")

        val HTTP_DEPENDENCY =
            Regex(
                pattern = """^\s*(implementation|api|compileOnly|runtimeOnly)\(libs\.(okhttp|retrofit)\b""",
                option = RegexOption.MULTILINE,
            )

        /** Pliki modułów funkcji z importem HTTP, z powodem; lista może tylko maleć (#888). */
        val OWN_HTTP =
            mapOf(
                "$BASE.feature.map.MapTileHttp" to
                    "#822: MapLibre przyjmuje tylko Call.Factory z okhttp; klient powstaje z klienta core:network",
            )
    }
}
