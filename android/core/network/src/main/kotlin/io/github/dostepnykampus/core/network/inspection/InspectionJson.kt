/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.encodeContract
import io.github.dostepnykampus.core.network.json.AppliedResult
import io.github.dostepnykampus.core.network.json.ConflictResult
import io.github.dostepnykampus.core.network.json.NewInspectionMarker
import io.github.dostepnykampus.core.network.json.RejectedResult
import io.github.dostepnykampus.core.network.json.RevokeInspectionMarkerOperation
import io.github.dostepnykampus.core.network.json.SetInspectionMarkerOperation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Treść żądania `POST /sync/push` z operacjami [operations] w podanej kolejności. Piętro `null` nie trafia do JSON.
 *
 * Wymagania: US15.3, BR-9
 */
internal fun Json.encodeMarkerPush(operations: List<MarkerOperation>): String {
    val compact = Json(from = this) { explicitNulls = false }
    val items = operations.map { compact.toJson(it) }
    return encodeContract(JsonObject.serializer(), JsonObject(mapOf(OPERATIONS to JsonArray(items))))
}

/**
 * Wyniki operacji z odpowiedzi `POST /sync/push` według identyfikatora operacji.
 *
 * @param body tekst odpowiedzi
 * @throws IllegalArgumentException gdy treść nie pasuje do kontraktu, także przy wyniku spoza `SyncOperationResult`
 */
internal fun Json.decodeMarkerOutcomes(body: String): Map<String, MarkerOutcome> =
    decodeContract(JsonObject.serializer(), body)
        .getValue(RESULTS)
        .jsonArray
        .associate { result -> outcomeOf(result) }

private fun Json.toJson(operation: MarkerOperation): JsonElement =
    when (operation) {
        is MarkerOperation.Set -> {
            val marker = NewInspectionMarker(id = operation.markerId, buildingSymbol = operation.buildingSymbol)
            val set =
                SetInspectionMarkerOperation(
                    clientOperationId = operation.operationId,
                    type = SET_TYPE,
                    marker = marker.copy(floorId = operation.floorId),
                )
            encodeToJsonElement(SetInspectionMarkerOperation.serializer(), set)
        }

        is MarkerOperation.Revoke -> {
            val revoke =
                RevokeInspectionMarkerOperation(
                    clientOperationId = operation.operationId,
                    type = REVOKE_TYPE,
                    markerId = operation.markerId,
                )
            encodeToJsonElement(RevokeInspectionMarkerOperation.serializer(), revoke)
        }
    }

private fun Json.outcomeOf(result: JsonElement): Pair<String, MarkerOutcome> {
    val name = result.jsonObject.getValue(OUTCOME)
    val outcome = MarkerOutcome.valueOf(name.jsonPrimitive.content)
    val operationId =
        when (outcome) {
            MarkerOutcome.APPLIED -> decodeFromJsonElement(AppliedResult.serializer(), result).clientOperationId
            MarkerOutcome.CONFLICT -> decodeFromJsonElement(ConflictResult.serializer(), result).clientOperationId
            MarkerOutcome.REJECTED -> decodeFromJsonElement(RejectedResult.serializer(), result).clientOperationId
        }
    return operationId to outcome
}

private const val OPERATIONS = "operations"
private const val RESULTS = "results"
private const val OUTCOME = "outcome"
private const val SET_TYPE = "SET_INSPECTION_MARKER"
private const val REVOKE_TYPE = "REVOKE_INSPECTION_MARKER"
