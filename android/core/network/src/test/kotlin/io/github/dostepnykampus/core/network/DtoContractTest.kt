/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.precise
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import io.github.dostepnykampus.core.network.building.decodeFloorList
import io.github.dostepnykampus.core.network.building.decodeLocateResult
import io.github.dostepnykampus.core.network.submission.decodeConflict
import io.github.dostepnykampus.core.network.submission.decodeSubmission
import io.github.dostepnykampus.core.network.submission.encodeNewSubmission
import io.github.dostepnykampus.core.network.submission.encodeSubmissionUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
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
 * Klient `core:network` zgodny z `api/openapi.yaml`: modele JSON pochodzą z generatora, więc strażnik pilnuje, że
 * ręcznych klas `@Serializable` nie ma, przykłady odpowiedzi z kontraktu dają się odczytać, wysyłane ciała spełniają
 * schematy, a wyliczenia modelu mają te same wartości co wyliczenia z kontraktu. Kontrakt jest czytany raz na klasę.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DtoContractTest {
    private val spec = OpenApiSpec(Path.of(requireNotNull(System.getProperty("openapi.file"))))
    private val json = NetworkModule.json()

    @Test
    fun d1_no_hand_written_dtos() {
        val found = serializableClasses()
        assertEquals(emptyList<String>(), found, "ręczna klasa @Serializable zamiast modelu z kontraktu")
    }

    @Test
    fun d1_detects_hand_written_dto() {
        val checked = listOf(HandWrittenDto::class.java, Submission::class.java, BuildingsApi::class.java)
        assertEquals(listOf(true, false, false), checked.map(::isHandWrittenDto))
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
                val model = values.first.map(Enum<*>::name)
                val contract = values.second.map(Enum<*>::name)
                "$name: model $model, kontrakt $contract"
                    .takeIf { model.toSet() != contract.toSet() }
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

    private fun isHandWrittenDto(type: Class<*>): Boolean =
        !type.name.startsWith(GENERATED_PACKAGE) && type.isAnnotationPresent(Serializable::class.java)

    private fun serializableClasses(): List<String> {
        val location = requireNotNull(BuildingsApi::class.java.protectionDomain?.codeSource).location
        return JarFile(File(location.toURI())).use { jar ->
            jar
                .entries()
                .asSequence()
                .map { entry -> entry.name }
                .filter { it.endsWith(".class") && '$' !in it }
                .map { it.removeSuffix(".class").replace(oldChar = '/', newChar = '.') }
                .map { name -> Class.forName(name, false, javaClass.classLoader) }
                .filter(::isHandWrittenDto)
                .map { type -> type.name }
                .toList()
        }
    }

    private companion object {
        const val NEW = "/paths/~1submissions/post/requestBody/content/application~1json/schema"
        const val UPDATE = "/paths/~1submissions~1{submissionId}/put/requestBody/content/application~1json/schema"

        /** Pakiet modeli generowanych z kontraktu; ich pola pochodzą wprost ze schematów. */
        const val GENERATED_PACKAGE = "io.github.dostepnykampus.core.network.json."
    }
}

@Serializable
private data class HandWrittenDto(val value: Int)
