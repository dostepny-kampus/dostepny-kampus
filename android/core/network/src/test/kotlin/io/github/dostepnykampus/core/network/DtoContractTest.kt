/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.precise
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import io.github.dostepnykampus.core.network.building.decodeFloorList
import io.github.dostepnykampus.core.network.building.decodeLocateResult
import io.github.dostepnykampus.core.network.json.Submission
import io.github.dostepnykampus.core.network.submission.decodeConflict
import io.github.dostepnykampus.core.network.submission.decodeSubmission
import io.github.dostepnykampus.core.network.submission.encodeNewSubmission
import io.github.dostepnykampus.core.network.submission.encodeSubmissionUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Path
import java.util.jar.JarFile
import io.github.dostepnykampus.core.network.json.BarrierGroup as BarrierGroupJson
import io.github.dostepnykampus.core.network.json.EntranceMarking as EntranceMarkingJson
import io.github.dostepnykampus.core.network.json.SubmissionContent as SubmissionContentJson
import io.github.dostepnykampus.core.network.json.SubmissionStatus as SubmissionStatusJson

/**
 * Ręczne klasy `@Serializable` z `core:network` zgodne ze schematami `api/openapi.yaml`. Spec ma niewiele
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
    }

    @Test
    fun d2_fields_equal_schema() {
        assertEquals(emptyList<String>(), TABLE.flatMap { (name, row) -> fieldProblems(name, row) })
    }

    @Test
    fun d3_minimal_json_decodes() {
        val problems = TABLE.filterValues { it.isResponse }.mapNotNull { (name, row) -> decodeProblem(name, row) }
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

    @Test
    fun d5_building_responses_decode() {
        val decoders: Map<String, (String) -> Any> =
            mapOf(
                "BuildingList" to json::decodeBuildingList,
                "BuildingDetails" to json::decodeBuildingDetails,
                "FloorList" to json::decodeFloorList,
                "LocateResult" to json::decodeLocateResult,
            )
        val problems =
            decoders.mapNotNull { (schema, decode) ->
                val body = spec.sample(spec.node("/components/schemas/$schema")).toString()
                runCatching { decode(body) }.exceptionOrNull()?.let { "$schema: ${it.message.orEmpty()}" }
            }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun d6_model_enums_equal_contract() {
        val pairs: Map<String, Pair<List<Enum<*>>, List<Enum<*>>>> =
            mapOf(
                "SubmissionStatus" to (SubmissionStatus.entries to SubmissionStatusJson.entries),
                "BarrierGroup" to (BarrierGroup.entries to BarrierGroupJson.entries),
                "EntranceMarking" to (EntranceMarking.entries to EntranceMarkingJson.entries),
                "Submission.kind" to (SubmissionKind.entries to Submission.Kind.entries),
                "SubmissionContent.kind" to (SubmissionKind.entries to SubmissionContentJson.Kind.entries),
            )
        val problems =
            pairs.mapNotNull { (name, values) ->
                val (model, contract) = values
                "$name: model ${model.map { it.name }}, kontrakt ${contract.map { it.name }}"
                    .takeIf { model.map { it.name }.toSet() != contract.map { it.name }.toSet() }
            }
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun d7_submission_responses_decode() {
        val submission = spec.sample(spec.node("/components/schemas/Submission")).jsonObject
        val floor = spec.sample(spec.node("/components/schemas/SubmissionFloor"))
        val bodies: Map<String, () -> Any> =
            mapOf(
                "Submission" to { json.decodeSubmission(submission.toString()) },
                "SubmissionFloor" to { json.decodeSubmission(JsonObject(submission + ("floor" to floor)).toString()) },
                "VersionConflictProblem" to {
                    json.decodeConflict(spec.sample(spec.node("/components/schemas/VersionConflictProblem")).toString())
                },
            )
        val problems =
            bodies.mapNotNull { (schema, decode) ->
                runCatching { decode() }.exceptionOrNull()?.let { "$schema: ${it.message.orEmpty()}" }
            }
        assertEquals(emptyList<String>(), problems)
    }

    private fun decodeProblem(name: String, row: Row): String? {
        val body = spec.sample(spec.node(row.pointers.first())).toString()
        return runCatching { decode(name, body) }.exceptionOrNull()?.let { "$name: ${it.message.orEmpty()}" }
    }

    private fun decode(name: String, body: String) =
        when (name) {
            "SubmissionDto" -> json.decodeSubmission(body)
            "VersionConflictDto" -> json.decodeConflict(body)
            "SubmissionFloorDto" -> json.decodeSubmission(withFloor(body))
            else -> json.decodeFromString(serializer(classOf(name)), body)
        }

    private fun withFloor(floor: String): String {
        val submission = spec.sample(spec.node("/components/schemas/Submission")).jsonObject
        return JsonObject(submission + ("floor" to json.parseToJsonElement(floor))).toString()
    }

    private fun enumProblems(name: String, row: Row): List<String> {
        val descriptor = descriptorOf(name)
        val props = row.pointers.flatMap { spec.properties(spec.node(it)).entries }.associate { it.key to it.value }
        return (0 until descriptor.elementsCount).flatMap { index ->
            val field = descriptor.getElementName(index)
            val element = descriptor.getElementDescriptor(index)
            val values = props[field]?.let { spec.resolve(it).path("enum").toList() }.orEmpty()
            val declared = values.map { value -> value.asString() }.toSet()
            val client = element.elementNames.toSet()
            val missing = (declared - client).map { "spec ma '$it', klient nie" }
            val sent = (client - declared).filter { "$field:$it" !in row.unsent }.map { "klient wysyła '$it'" }
            val issues =
                when {
                    element.kind != SerialKind.ENUM -> emptyList()
                    row.isResponse -> missing
                    else -> sent
                }
            issues.map { "$name.$field: $it" }
        }
    }

    private fun fieldProblems(name: String, row: Row): List<String> {
        val descriptor = descriptorOf(name)
        val schema = row.pointers.map(spec::node)
        if (descriptor.kind == SerialKind.ENUM) {
            val values = schema.flatMap { node -> node.path("enum").toList().map { it.asString() } }.toSet()
            return listOf("$name: wartości poza schematem").filter { descriptor.elementNames.toSet() != values }
        }
        val fields = descriptor.elementNames.toSet()
        val declared = schema.flatMap { node -> spec.properties(node).keys }.toSet()
        val stale = row.ignored.keys.filter { it in fields || it !in declared }
        return (fields - declared).map { "$name.$it: pole DTO poza schematem" } +
            (declared - fields - row.ignored.keys).map { "$name.$it: pole schematu bez pola w DTO" } +
            stale.map { "$name.$it: nieaktualny wpis „klient ignoruje”" } +
            enumProblems(name, row)
    }

    private fun classOf(name: String): Class<*> = serializable.first { it.simpleName == name }

    private fun descriptorOf(name: String): SerialDescriptor = serializer(classOf(name)).descriptor

    private fun serializableClasses(): List<Class<*>> {
        val location = requireNotNull(BuildingsApi::class.java.protectionDomain?.codeSource).location
        return JarFile(File(location.toURI())).use { jar ->
            jar
                .entries()
                .asSequence()
                .map { entry -> entry.name }
                .filter { it.endsWith(".class") && '$' !in it }
                .map { it.removeSuffix(".class").replace(oldChar = '/', newChar = '.') }
                .filterNot { it.startsWith(GENERATED_PACKAGE) }
                .map { className -> Class.forName(className, false, javaClass.classLoader) }
                .filter { it.isAnnotationPresent(Serializable::class.java) }
                .toList()
        }
    }

    private class Row(
        vararg schemas: String,
        val ignored: Map<String, String> = emptyMap(),
        val isResponse: Boolean = true,
        val unsent: Map<String, String> = emptyMap(),
    ) {
        val pointers = schemas.map { "/components/schemas/$it" }
    }

    private companion object {
        const val NEW = "/paths/~1submissions/post/requestBody/content/application~1json/schema"
        const val UPDATE = "/paths/~1submissions~1{submissionId}/put/requestBody/content/application~1json/schema"

        /** Pakiet modeli generowanych z kontraktu; ich pola pochodzą wprost ze schematów. */
        const val GENERATED_PACKAGE = "io.github.dostepnykampus.core.network.json."

        const val NOT_SHOWN = "model zgłoszenia w aplikacji nie ma tego pola, żaden ekran go nie pokazuje"

        /** DTO `core:network` ↔ schemat z `components/schemas`; nazwa klasy bez pakietu. */
        val TABLE =
            mapOf(
                "SubmissionFloorDto" to Row("SubmissionFloor"),
                "SubmissionRequest" to
                    Row(
                        "NewSubmission",
                        "SubmissionUpdate",
                        isResponse = false,
                        unsent = mapOf("entranceMarking:NO_DATA" to "SubmissionContent.init odrzuca NO_DATA"),
                    ),
                "SubmissionDto" to
                    Row(
                        "Submission",
                        ignored =
                            mapOf(
                                "duplicateOfSubmissionId" to NOT_SHOWN,
                                "author" to "$NOT_SHOWN; autora widzi tylko Weryfikator",
                            ),
                    ),
                "VersionConflictDto" to
                    Row(
                        "VersionConflictProblem",
                        ignored =
                            listOf("type", "title", "status", "detail", "instance").associateWith {
                                "klient czyta z odpowiedzi 409 tylko serverSubmission, opis błędu nie jest pokazywany"
                            },
                    ),
            )

        /** Klasy `@Serializable` bez odpowiednika w spec, z powodem; dziś nie ma żadnej. */
        val HELPERS = emptyMap<String, String>()
    }
}
