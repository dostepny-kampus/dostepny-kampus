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
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
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

    /** Stan zmienny tylko w klasach z grafu Hilt, nie na poziomie pliku ani w `object` (#887). */
    @Test
    fun infra_a1_no_global_state() {
        val production = Konsist.scopeFromProduction().files
        assertEquals(emptySet<String>(), globalState(production) - GLOBAL_STATE.keys, STATE_HINT)
        assertEquals(emptySet<String>(), GLOBAL_STATE.keys - globalState(production))
    }

    /** #887: `var` na poziomie pliku i w `object`, `MutableStateFlow` w `companion object` łamią regułę. */
    @Test
    fun infra_a1_global_state_rejects(
        @TempDir fixtures: File,
    ) {
        val directory = File(fixtures, "state").apply { mkdirs() }
        val source =
            listOf(
                "package $BASE.feature.map",
                "import kotlinx.coroutines.flow.MutableStateFlow",
                "var agent: String? = null",
                "val fixed = 1",
                "object Cache { var last = 0\n val seen = mutableListOf<String>() }",
                "class Screen { var local = 0\n companion object { val state = MutableStateFlow(0) } }",
                "@Singleton class Store { companion object { val held = MutableStateFlow(0) } }",
            ).joinToString(separator = System.lineSeparator())
        File(directory, "State.kt").writeText(source)
        val files = Konsist.scopeFromExternalDirectory(directory.absolutePath).files
        val expected = setOf("agent", "Cache.last", "Cache.seen", "Screen.Companion.state")
        assertEquals(expected.map { "$BASE.feature.map.$it" }.toSet(), globalState(files))
    }

    /** Pliki z `@Composable` nie liczą geometrii `core.geo`; wynik przychodzi w stanie z modelu widoku (#892). */
    @Test
    fun nfr11_composable_no_geometry() {
        val production = Konsist.scopeFromProduction().files
        assertEquals(emptySet<String>(), composableGeometry(production) - GEOMETRY_OUTSIDE_VM.keys, GEOMETRY_HINT)
    }

    /** #892: obliczenia `core.geo` importują tylko modele widoku i pakiety `data`, więc nie ma pliku pośredniego. */
    @Test
    fun nfr11_geometry_only_in_vm() {
        val production = Konsist.scopeFromProduction().files
        val found = geometryOutsideViewModels(production)
        assertEquals(emptySet<String>(), found - GEOMETRY_OUTSIDE_VM.keys, GEOMETRY_HINT)
        assertEquals(emptySet<String>(), GEOMETRY_OUTSIDE_VM.keys - found)
    }

    /** #892: `@Composable` z importem obliczeń geometrii łamie obie reguły, a model widoku żadnej. */
    @Test
    fun nfr11_geometry_rule_rejects(
        @TempDir fixtures: File,
    ) {
        val directory = File(fixtures, "geo").apply { mkdirs() }
        val compute = "package $BASE.core.geo\nfun labelPoint(x: Int): Int = x"
        File(directory, "LabelPoint.kt").writeText(compute)
        val screen =
            listOf(
                "package $BASE.feature.map",
                "import $BASE.core.geo.labelPoint",
                "@Composable fun Pin() { labelPoint(1) }",
            ).joinToString(separator = System.lineSeparator())
        File(directory, "Pin.kt").writeText(screen)
        val model =
            "package $BASE.feature.map\nimport $BASE.core.geo.labelPoint\nclass PinViewModel { val p = labelPoint(1) }"
        File(directory, "PinViewModel.kt").writeText(model)
        val files = Konsist.scopeFromExternalDirectory(directory.absolutePath).files
        assertEquals(setOf("$BASE.feature.map.Pin"), composableGeometry(files))
        assertEquals(setOf("$BASE.feature.map.Pin"), geometryOutsideViewModels(files))
    }

    private fun geometryImporters(files: List<KoFileDeclaration>): List<KoFileDeclaration> {
        val computations =
            files
                .filter { it.packagePath() == "$BASE.core.geo" && it.name !in GEOMETRY_CONVERSIONS }
                .flatMap { file -> file.functions(includeNested = false).map { "${file.packagePath()}.${it.name}" } }
                .toSet()
        return files
            .filter { it.inPackage("feature") || it.inPackage("app") }
            .filter { file -> file.imports.any { it.name in computations } }
    }

    private fun composableGeometry(files: List<KoFileDeclaration>): Set<String> =
        geometryImporters(files)
            .filter { file -> file.functions(includeNested = true).any { it.hasAnnotationWithName("Composable") } }
            .map { "${it.packagePath()}.${it.name}" }
            .toSet()

    private fun geometryOutsideViewModels(files: List<KoFileDeclaration>): Set<String> =
        geometryImporters(files)
            .filterNot { it.name.endsWith("ViewModel") || it.packagePath().split('.').contains("data") }
            .map { "${it.packagePath()}.${it.name}" }
            .toSet()

    private fun globalState(files: List<KoFileDeclaration>): Set<String> {
        val topLevel =
            files
                .asSequence()
                .flatMap { it.properties(includeNested = false) }
                .filter { it.isMutable() }
                .map { "${it.containingFile.packagePath()}.${it.name}" }
        val inObjects =
            files
                .asSequence()
                .flatMap { it.objects(includeNested = true) }
                .filterNot { it.isInSingleton() }
                .flatMap { owner -> owner.mutableProperties().map { "${owner.fullyQualifiedName.orEmpty()}.$it" } }
        return (topLevel + inObjects).toSet()
    }

    private fun KoObjectDeclaration.mutableProperties(): List<String> =
        properties(includeNested = false).filter { it.isMutable() }.map { it.name }

    private fun KoObjectDeclaration.isInSingleton(): Boolean =
        (containingDeclaration as? KoClassDeclaration)?.hasAnnotationWithName("Singleton") == true

    private fun KoPropertyDeclaration.isMutable(): Boolean {
        val declaration = text.substringBefore('=')
        val shape = if (':' in declaration) declaration.substringAfter(':') else text.substringAfter('=')
        return isVar || MUTABLE_TYPE.containsMatchIn(shape)
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

        const val STATE_HINT =
            "Stan globalny: przenieś stan do klasy wstrzykiwanej przez Hilt i podaj go przez konstruktor"

        val MUTABLE_TYPE =
            listOf(
                "Mutable(State|Shared)Flow",
                """Atomic[A-Z]\w*""",
                "mutable(List|Map|Set)Of",
                "Mutable(List|Map|Set)",
                "ArrayList|HashMap|HashSet",
            ).joinToString(separator = "|", prefix = """\b(""", postfix = """)\b""").toRegex()

        const val GEOMETRY_HINT =
            "Geometria poza modelem widoku: policz w modelu widoku poza wątkiem głównym i podaj wynik w stanie ekranu"

        /** Zamiany modelu na typy spatialk, bez obliczeń geometrii. */
        val GEOMETRY_CONVERSIONS = setOf("SpatialkGeometry")

        /** Pliki z obliczeniami `core.geo` poza modelem widoku i pakietem `data`; lista może tylko maleć (#892). */
        val GEOMETRY_OUTSIDE_VM =
            mapOf(
                "$BASE.feature.map.BuildingList" to
                    "#1053: lista z nagłówkami terenów (#1058) liczy wyszukiwanie w kompozycji",
                "$BASE.feature.map.CampusSearch" to
                    "#1053: wyszukiwanie liczone w kompozycji, jak punkt podpisu w #818",
                "$BASE.feature.capture.BuildingLookup" to
                    "#892: liczy w computeContext dla modeli widoku, poza wątkiem głównym; do pakietu data",
            )

        /** Stan globalny w kodzie produkcyjnym, z numerem zadania; lista może tylko maleć (#887). */
        val GLOBAL_STATE = emptyMap<String, String>()

        /** Pliki modułów funkcji z importem HTTP, z powodem; lista może tylko maleć (#888). */
        val OWN_HTTP =
            mapOf(
                "$BASE.feature.map.MapTileHttp" to
                    "#822: MapLibre przyjmuje tylko Call.Factory z okhttp; klient powstaje z klienta core:network",
            )
    }
}
