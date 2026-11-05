/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.precise
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * Klasy `@Serializable` z `core:network` zgodne ze schematami `api/openapi.yaml` (decyzja 122). Spec ma niewiele
 * przykładów, więc strażnik porównuje pola i nullowalność DTO ze schematami oraz waliduje ciała żądań.
 * Kontrakt jest czytany raz na klasę testu.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DtoContractTest {
    private val spec = OpenApiSpec(Path.of(requireNotNull(System.getProperty("openapi.file"))))
    private val serializable = serializableClasses()
    private val json = NetworkModule.json()

    @Test
    fun d1_every_class_has_a_row() {
        val names = serializable.map { it.simpleName }.toSet()
        assertEquals(emptySet<String>(), names - TABLE.keys - HELPERS.keys, "klasa @Serializable bez wpisu")
        assertEquals(emptySet<String>(), TABLE.keys + HELPERS.keys - names, "wpis bez klasy")
        assertEquals(emptyList<String>(), HELPERS.filterValues { it.isBlank() }.keys.toList(), "wpis bez powodu")
    }

    @Test
    fun d1_every_row_has_a_schema() {
        val missing = TABLE.values.flatMap { it.pointers }.filter { spec.node(it).isMissingNode }
        assertEquals(emptyList<String>(), missing, "wpis bez schematu")
    }

    @Test
    fun d2_fields_equal_schema() {
        assertEquals(emptyList<String>(), TABLE.flatMap { (name, row) -> fieldProblems(name, row) })
    }

    @Test
    fun d3_optional_has_default() {
        val responses = TABLE.filterValues { row -> row.isResponse }
        val problems = responses.flatMap { (name, row) -> defaultProblems(name, row) }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun d4_requests_match_schema() {
        val bodies =
            listOf(precise, quick).flatMap { content ->
                listOf(
                    NEW to json.encodeNewSubmission(ID, content),
                    UPDATE to json.encodeSubmissionUpdate(version = 2, content = content),
                )
            }
        val problems =
            bodies.flatMap { (pointer, body) ->
                val allowed = spec.properties(spec.node(pointer)).keys
                val outside = json.parseToJsonElement(body).jsonObject.keys - allowed
                spec.violations(pointer, body) + outside.map { "pole żądania '$it' poza schematem" }
            }
        assertEquals(emptyList<String>(), problems)
    }

    private fun defaultProblems(name: String, row: Row): List<String> {
        val descriptor = descriptorOf(name)
        val required = row.pointers.flatMap { pointer -> spec.required(spec.node(pointer)) }
        val unsafe =
            descriptor.elementNames
                .asSequence()
                .withIndex()
                .filter { (_, field) -> descriptor.kind != SerialKind.ENUM && field !in required }
                .filterNot { (index, _) ->
                    descriptor.isElementOptional(index) || descriptor.getElementDescriptor(index).isNullable
                }
        return unsafe.map { (_, field) -> "$name.$field: pole nieobowiązkowe, bez domyślnej i bez null" }.toList()
    }

    private fun fieldProblems(name: String, row: Row): List<String> {
        val descriptor = descriptorOf(name)
        val schema = row.pointers.map(spec::node)
        val problems = mutableListOf<String>()
        if (descriptor.kind == SerialKind.ENUM) {
            val values = schema.flatMap { node -> node.path("enum").toList().map { value -> value.asString() } }
            if (descriptor.elementNames.toSet() != values.toSet()) {
                problems += "$name: wartości wyliczenia poza schematem"
            }
        } else {
            val fields = descriptor.elementNames.toSet()
            val declared = schema.flatMap { node -> spec.properties(node).keys }.toSet()
            problems += (fields - declared).map { "$name.$it: pole DTO poza schematem" }
            problems += (declared - fields - row.ignored.keys).map { "$name.$it: pole schematu bez pola w DTO" }
            val stale = row.ignored.keys.filter { it in fields || it !in declared }
            problems += stale.map { "$name.$it: nieaktualny wpis „klient ignoruje”" }
        }
        return problems
    }

    private fun descriptorOf(name: String): SerialDescriptor =
        serializer(serializable.first { it.simpleName == name }).descriptor

    private fun serializableClasses(): List<Class<*>> {
        val location = requireNotNull(BuildingsApi::class.java.protectionDomain?.codeSource).location
        val names =
            JarFile(File(location.toURI())).use { jar ->
                jar
                    .entries()
                    .asSequence()
                    .map { entry -> entry.name }
                    .filter { it.endsWith(".class") && '$' !in it }
                    .toList()
            }
        return names
            .asSequence()
            .map { entry -> entry.removeSuffix(".class").replace(oldChar = '/', newChar = '.') }
            .map { className -> Class.forName(className, false, javaClass.classLoader) }
            .filter { it.isAnnotationPresent(Serializable::class.java) }
            .toList()
    }

    /** Wiersz tabeli: wskaźniki schematów, pola schematu pomijane przez klienta (z powodem) i kierunek wymiany. */
    private class Row(
        schemas: List<String>,
        val ignored: Map<String, String> = emptyMap(),
        val isResponse: Boolean = true,
    ) {
        val pointers = schemas.map { "/components/schemas/$it" }

        constructor(schema: String, ignored: Map<String, String> = emptyMap()) : this(listOf(schema), ignored)
    }

    private companion object {
        const val NEW = "/paths/~1submissions/post/requestBody/content/application~1json/schema"
        const val UPDATE = "/paths/~1submissions~1{submissionId}/put/requestBody/content/application~1json/schema"
        const val NOT_SHOWN = "model zgłoszenia w aplikacji nie ma tego pola, żaden ekran go nie pokazuje"

        /** DTO `core:network` ↔ schemat z `components/schemas`; nazwa klasy bez pakietu. */
        val TABLE =
            mapOf(
                "BuildingListDto" to Row("BuildingList"),
                "BuildingDto" to Row("Building"),
                "BuildingDetailsDto" to Row("BuildingDetails"),
                "EntranceDto" to Row("Entrance"),
                "GeoPointDto" to Row("GeoPoint"),
                "LocateResultDto" to Row("LocateResult"),
                "DetailLevel" to Row("DetailLevel"),
                "MarkingSource" to Row("Entrance/properties/markingSource"),
                "SubmissionFloorDto" to Row("SubmissionFloor"),
                "SubmissionRequest" to Row(listOf("NewSubmission", "SubmissionUpdate"), isResponse = false),
                "SubmissionDto" to
                    Row(
                        "Submission",
                        mapOf(
                            "duplicateOfSubmissionId" to NOT_SHOWN,
                            "author" to "$NOT_SHOWN; autora widzi tylko Weryfikator",
                        ),
                    ),
                "VersionConflictDto" to
                    Row(
                        "VersionConflictProblem",
                        listOf("type", "title", "status", "detail", "instance").associateWith {
                            "klient czyta z odpowiedzi 409 tylko serverSubmission, opis błędu nie jest pokazywany"
                        },
                    ),
            )

        /** Klasy `@Serializable` bez odpowiednika w spec, z powodem; dziś nie ma żadnej. */
        val HELPERS = emptyMap<String, String>()
    }
}
