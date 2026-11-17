/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.auth.QueueOwner
import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.encodeContract
import io.github.dostepnykampus.core.network.exchange
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP wysyłki zmian znaczników piętra obejrzanego zapisanych w telefonie (`POST /sync/push`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Kod inny niż 200 to
 * [ApiResult.ServerError] z tym kodem. Anulowanie korutyny przerywa żądanie HTTP.
 *
 * Wymagania: US15.3, BR-9, BR-20, NFR-55
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class MarkerChangesApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val target =
        baseUrl
            .toHttpUrl()
            .newBuilder()
            .addPathSegments(PATH)
            .build()

    /**
     * Wysyła [changes] w kolejności zapisu w telefonie i zwraca wynik każdej zmiany w tej samej kolejności.
     * Powtórzona operacja zwraca pierwszy wynik i niczego nie zapisuje drugi raz. Pusta lista nie wysyła
     * żądania. Kontrakt przyjmuje najwyżej 100 operacji w jednej paczce; dłuższą listę dzieli wywołujący.
     *
     * @param changes zmiany znaczników, najwyżej 100
     * @param owner konto zapisujące zmiany w kolejce
     */
    public suspend fun push(changes: List<MarkerChange>, owner: String? = null): ApiResult<List<MarkerChangeResult>> =
        if (changes.isEmpty()) ApiResult.Success(emptyList()) else http.exchange(request(changes, owner), ::results)

    private fun request(changes: List<MarkerChange>, owner: String?): Request {
        val operations = buildJsonArray { changes.forEach { add(it.toOperation()) } }
        val body = json.encodeContract(JsonObject.serializer(), JsonObject(mapOf(OPERATIONS to operations)))
        return Request
            .Builder()
            .url(target)
            .tag(QueueOwner::class.java, owner?.let(::QueueOwner))
            .header(name = ACCEPT, value = ACCEPTED_TYPES)
            .post(body.toRequestBody(JSON))
            .build()
    }

    private fun results(code: Int, body: String): ApiResult<List<MarkerChangeResult>> =
        if (code == HTTP_OK) decodeResults(code, body) else ApiResult.ServerError(code)

    private fun decodeResults(code: Int, body: String): ApiResult<List<MarkerChangeResult>> =
        try {
            ApiResult.Success(json.decodeContract(JsonElement.serializer(), body).results())
        } catch (error: IllegalArgumentException) {
            ApiResult.ServerError(code = code, cause = error)
        }

    private companion object {
        const val PATH = "sync/push"
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
        const val OPERATIONS = "operations"
        const val RESULTS = "results"
        const val OPERATION_ID = "clientOperationId"
        const val TYPE = "type"
        const val OUTCOME = "outcome"
        const val APPLIED = "APPLIED"
        val REFUSED = setOf("REJECTED", "CONFLICT")
        val JSON = "application/json".toMediaType()

        fun MarkerChange.toOperation(): JsonObject =
            when (this) {
                is MarkerChange.Mark -> toSetOperation()
                is MarkerChange.Revoke -> toRevokeOperation()
            }

        fun MarkerChange.Revoke.toRevokeOperation(): JsonObject =
            buildJsonObject {
                put(key = OPERATION_ID, value = operationId)
                put(key = TYPE, value = "REVOKE_INSPECTION_MARKER")
                put(key = "markerId", value = markerId)
            }

        fun MarkerChange.Mark.toSetOperation(): JsonObject {
            val marker =
                buildJsonObject {
                    put(key = "id", value = markerId)
                    put(key = "buildingSymbol", value = buildingSymbol)
                    floorId?.let { put(key = "floorId", value = it) }
                }
            return buildJsonObject {
                put(key = OPERATION_ID, value = operationId)
                put(key = TYPE, value = "SET_INSPECTION_MARKER")
                put(key = "marker", element = marker)
            }
        }

        fun JsonElement.results(): List<MarkerChangeResult> =
            requireNotNull(jsonObject[RESULTS]) { "brak pola $RESULTS" }.jsonArray.map { it.toResult() }

        fun JsonElement.toResult(): MarkerChangeResult {
            val outcome = text(OUTCOME)
            require(outcome == APPLIED || outcome in REFUSED) { "wynik operacji spoza kontraktu: $outcome" }
            return MarkerChangeResult(operationId = text(OPERATION_ID), isApplied = outcome == APPLIED)
        }

        fun JsonElement.text(name: String): String =
            requireNotNull(jsonObject[name]) { "brak pola $name w wyniku operacji" }.jsonPrimitive.content
    }
}
