/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.combined.KoClassAndInterfaceAndObjectDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NullabilityArchitectureTest {
    private val production = Konsist.scopeFromProduction().files

    @Test
    fun dec122_model_nullable_listed() {
        assertEquals(emptySet<String>(), nullablePropertiesOutsideList(production))
    }

    @Test
    fun dec124_listed_nullable_exist() {
        val present = nullableProperties(scope(production, MODEL))
        assertEquals(NULLABLE_IN_MODEL.keys, present.intersect(NULLABLE_IN_MODEL.keys))
    }

    @Test
    fun dec122_dto_only_in_network() {
        assertEquals(emptySet<String>(), dtoOutsideNetwork(Konsist.scopeFromProject().files))
    }

    @Test
    fun dec122_pure_core_no_androidx() {
        assertEquals(emptySet<String>(), androidImportsInPureCore(production))
    }

    @Test
    fun dec122_rule_rejects_nullable(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Room.kt",
                source =
                    lines(
                        "package $BASE.$MODEL",
                        "",
                        "val lastRoom: String? = null",
                        "",
                        "data class Room(val label: String?, var floor: Int?, val id: String) {",
                        "    var note: String? = null",
                        "    val code: String = id",
                        "}",
                        "",
                        "class Draft(raw: String?) {",
                        "    val size: Int = raw.orEmpty().length",
                        "}",
                        "",
                        "data class Building(val symbol: String, val name: String?, val alias: String?)",
                    ),
            )
        assertEquals(
            setOf("Room.label", "Room.floor", "Room.note", "Building.alias", "Room.kt.lastRoom"),
            nullablePropertiesOutsideList(files),
        )
    }

    @Test
    fun dec122_rule_rejects_dto(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Data.kt",
                source = "package $BASE.core.data\n\ndata class BuildingDto(val name: String?)",
            ) +
                fixtures.fixture(
                    name = "Network.kt",
                    source = "package $BASE.$NETWORK\n\ndata class EntranceDto(val name: String?)",
                )
        assertEquals(setOf("$BASE.core.data.BuildingDto"), dtoOutsideNetwork(files))
    }

    @Test
    fun dec122_rule_rejects_androidx(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Geo.kt",
                source =
                    lines(
                        "package $BASE.$GEO",
                        "",
                        "import androidx.annotation.Keep",
                        "import kotlin.math.abs",
                    ),
            ) +
                fixtures.fixture(
                    name = "Model.kt",
                    source = "package $BASE.$MODEL\n\nimport android.util.Log",
                )
        assertEquals(
            setOf("Geo.kt: androidx.annotation.Keep", "Model.kt: android.util.Log"),
            androidImportsInPureCore(files),
        )
    }

    @Test
    fun dec122_empty_scope_fails(
        @TempDir fixtures: File,
    ) {
        val files =
            fixtures.fixture(
                name = "Other.kt",
                source = "package $BASE.feature.map\n\ndata class Pin(val label: String?)",
            )
        val errors =
            listOf(
                assertThrows<IllegalStateException> { nullablePropertiesOutsideList(files) },
                assertThrows<IllegalStateException> { dtoOutsideNetwork(emptyList()) },
                assertThrows<IllegalStateException> { androidImportsInPureCore(files) },
            )
        assertTrue(errors.all { it.message.orEmpty().startsWith("Pusty zakres") }) { errors.toString() }
    }

    private fun File.fixture(name: String, source: String): List<KoFileDeclaration> {
        val directory = File(this, name.substringBefore('.')).apply { mkdirs() }
        File(directory, name).writeText(source)
        return Konsist.scopeFromExternalDirectory(directory.absolutePath).files
    }

    private fun nullablePropertiesOutsideList(files: List<KoFileDeclaration>): Set<String> =
        nullableProperties(scope(files, MODEL)) - NULLABLE_IN_MODEL.keys

    private fun nullableProperties(files: List<KoFileDeclaration>): Set<String> =
        files.flatMap { nullableIn(it) }.toSet()

    private fun nullableIn(file: KoFileDeclaration): List<String> {
        val members = file.classesAndInterfacesAndObjects(includeLocal = false).flatMap { nullableMembers(it) }
        val topLevel = file.properties(includeNested = false).filter { it.type?.isNullable == true }
        return members + topLevel.map { "${file.nameWithExtension}.${it.name}" }
    }

    private fun nullableMembers(owner: KoClassAndInterfaceAndObjectDeclaration): List<String> {
        val ownerName = owner.fullyQualifiedName.orEmpty().removePrefix("${owner.containingFile.packagePath()}.")
        val body = owner.properties(includeNested = false).filter { it.type?.isNullable == true }
        val parameters = (owner as? KoClassDeclaration)?.primaryConstructor?.parameters.orEmpty()
        val constructor = parameters.filter { (it.isVal || it.isVar) && it.type.isNullable }
        return (body.map { it.name } + constructor.map { it.name }).map { "$ownerName.$it" }
    }

    private fun dtoOutsideNetwork(files: List<KoFileDeclaration>): Set<String> =
        nonEmpty(files, "projekt")
            .asSequence()
            .flatMap { it.classesAndInterfacesAndObjects() }
            .filter { it.name.endsWith("Dto") && !it.containingFile.inPackage(NETWORK) }
            .map { it.fullyQualifiedName.orEmpty() }
            .toSet()

    private fun androidImportsInPureCore(files: List<KoFileDeclaration>): Set<String> =
        listOf(MODEL, GEO)
            .asSequence()
            .flatMap { scope(files, it) }
            .flatMap { androidImports(it) }
            .toSet()

    private fun androidImports(file: KoFileDeclaration): List<String> =
        file.imports
            .filter { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            .map { "${file.nameWithExtension}: ${it.name}" }

    private fun scope(files: List<KoFileDeclaration>, relative: String): List<KoFileDeclaration> =
        nonEmpty(files.filter { it.inPackage(relative) }, relative)

    private fun lines(vararg line: String): String = line.joinToString(separator = "\n")

    private fun nonEmpty(files: List<KoFileDeclaration>, what: String): List<KoFileDeclaration> {
        check(files.isNotEmpty()) { "Pusty zakres: $what" }
        return files
    }

    private fun KoFileDeclaration.packagePath(): String = packagee?.name.orEmpty()

    private fun KoFileDeclaration.inPackage(relative: String): Boolean =
        packagePath() == "$BASE.$relative" || packagePath().startsWith("$BASE.$relative.")

    private companion object {
        const val BASE = "io.github.dostepnykampus"
        const val MODEL = "core.model"
        const val GEO = "core.geo"
        const val NETWORK = "core.network"

        /** Zamknięta lista nullable w modelu: para „klasa.właściwość” → powód (decyzje 122, 124). */
        val NULLABLE_IN_MODEL =
            mapOf(
                "Building.name" to "budynek bez nazwy w danych, decyzja 124",
                "Entrance.name" to "dane OpenStreetMap nie podają opisu wejścia",
                "Entrance.type" to "wejście spoza danych OpenStreetMap nie ma tagu entrance",
            )
    }
}
