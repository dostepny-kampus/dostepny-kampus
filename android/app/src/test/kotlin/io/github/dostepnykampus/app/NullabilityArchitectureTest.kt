/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import com.lemonappdev.konsist.api.Konsist
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
                        "",
                        "    data class Door(val width: Int?)",
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
            setOf("Room.label", "Room.floor", "Room.note", "Room.Door.width", "Building.alias", "Room.kt.lastRoom"),
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
        val nullable = owner.properties(includeNested = false).filter { it.type?.isNullable == true }
        return nullable.map { "$ownerName.${it.name}" }
    }

    private fun dtoOutsideNetwork(files: List<KoFileDeclaration>): Set<String> =
        nonEmpty(files, "projekt")
            .asSequence()
            .flatMap { it.classesAndInterfacesAndObjects() }
            .filter { it.name.endsWith("Dto") && !it.containingFile.inPackage(NETWORK) }
            .map { it.fullyQualifiedName.orEmpty() }
            .toSet()

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
        const val NETWORK = "core.network"

        const val OPTIONAL = "pole nieobowiązkowe OpenAPI (decyzja 127 P1)"
        const val ABSENT_FLAG = "brak pola w odpowiedzi zachowany przy odesłaniu (decyzja K4 naprawy klienta)"
        const val BY_STATUS = "zależne od statusu"

        /** Zamknięta lista nullable w modelu: para „klasa.właściwość” → powód (decyzje 122, 124, 127). */
        val NULLABLE_IN_MODEL =
            mapOf(
                "Building.name" to "budynek bez nazwy w danych, decyzja 124",
                "Entrance.name" to "dane OpenStreetMap nie podają opisu wejścia",
                "Entrance.type" to "wejście spoza danych OpenStreetMap nie ma tagu entrance",
                "Entrance.assessment" to "wejście z pliku konturów nie ma dnia i źródła oznaczenia, US17.3",
                "Submission.rejectionReason" to "$BY_STATUS, BR-16",
                "Submission.verifiedAt" to "$BY_STATUS, BR-17",
                "SubmissionContent.accuracyMeters" to OPTIONAL,
                "SubmissionContent.isManualCorrection" to ABSENT_FLAG,
                "SubmissionContent.buildingSymbol" to "$OPTIONAL, BR-G2: poza budynkiem",
                "SubmissionContent.floor" to "$OPTIONAL, BR-G1",
                "SubmissionContent.barrierGroup" to OPTIONAL,
                "SubmissionContent.barrierType" to OPTIONAL,
                "SubmissionContent.severity" to OPTIONAL,
                "SubmissionContent.description" to OPTIONAL,
                "SubmissionContent.replacesSubmissionId" to OPTIONAL,
                "SubmissionContent.isBarrierRemoved" to ABSENT_FLAG,
                "SubmissionContent.entranceId" to OPTIONAL,
                "SubmissionContent.entranceMarking" to OPTIONAL,
                "SubmissionFloor.floorId" to OPTIONAL,
                "SubmissionFloor.number" to OPTIONAL,
                "SubmissionFloor.isMezzanine" to ABSENT_FLAG,
            )
    }
}
