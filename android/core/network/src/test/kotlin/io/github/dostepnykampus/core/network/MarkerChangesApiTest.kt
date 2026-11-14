/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.inspection.MarkerChange
import io.github.dostepnykampus.core.network.inspection.MarkerChangeResult
import io.github.dostepnykampus.core.network.inspection.MarkerChangesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

/** Wysyłka zmian znaczników przez `POST /sync/push`: ciało według `SyncOperation` z kontraktu i odczyt wyników. */
class MarkerChangesApiTest {
    @Test
    fun us15_3_contract_operations() {
        val sent = served(code = 200, body = """{"results":[]}""") { api -> api.push(CHANGES) }.second
        val spec = OpenApiSpec(Path.of(requireNotNull(System.getProperty("openapi.file"))))
        val expected =
            """
                |{"operations":[
                |{"clientOperationId":"$FIRST","type":"SET_INSPECTION_MARKER",
                |"marker":{"id":"$MARKER","buildingSymbol":"A-1","floorId":"A-1-2"}},
                |{"clientOperationId":"$SECOND","type":"SET_INSPECTION_MARKER",
                |"marker":{"id":"$OTHER","buildingSymbol":"A-6"}},
                |{"clientOperationId":"$THIRD","type":"REVOKE_INSPECTION_MARKER","markerId":"$MARKER"}
                |]}
            """.trimMargin()
        val body = JSON.parseToJsonElement(sent.body.readUtf8())
        assertEquals(JSON.parseToJsonElement(expected), body, "treść paczki zmian")
        assertEquals(listOf("POST", "/sync/push"), listOf(sent.method, sent.path), "metoda i ścieżka żądania")
        assertEquals(
            emptyList<String>(),
            body.jsonObject
                .getValue("operations")
                .jsonArray
                .flatMap { spec.violations(pointer = OPERATION, json = it.toString()) },
            "operacja znacznika niezgodna ze schematem SyncOperation",
        )
    }

    @Test
    fun us15_3_result_per_operation() {
        val body =
            """
                |{"results":[
                |{"clientOperationId":"$FIRST","outcome":"APPLIED"},
                |{"clientOperationId":"$THIRD","outcome":"REJECTED",
                |"problem":{"type":"about:blank","title":"Brak roli","status":403}}
                |]}
            """.trimMargin()
        val expected =
            listOf(
                MarkerChangeResult(operationId = FIRST, isApplied = true),
                MarkerChangeResult(operationId = THIRD, isApplied = false),
            )
        val result = served(code = 200, body = body) { api -> api.push(CHANGES) }.first
        assertEquals(ApiResult.Success(expected), result, "wynik zapisanej i odrzuconej operacji")
    }

    @Test
    fun empty_list_sends_nothing() {
        val (result, requests) =
            MockWebServer().use { server ->
                server.start()
                runBlocking { api(server).push(emptyList()) } to server.requestCount
            }
        assertEquals(ApiResult.Success(emptyList<MarkerChangeResult>()), result, "wynik pustej paczki")
        assertEquals(0, requests, "pusta paczka nie wysyła żądania")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 500])
    fun error_code_is_server_error(code: Int) {
        val result = served(code = code, body = """{"type":"about:blank"}""") { api -> api.push(CHANGES) }.first
        assertEquals(ApiResult.ServerError(code), result, "kod $code jako błąd serwera")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "to nie JSON",
            """{"wyniki":[]}""",
            """{"results":[{"outcome":"APPLIED"}]}""",
            """{"results":[{"clientOperationId":"a","outcome":"NIEZNANY"}]}""",
        ],
    )
    fun bad_body_is_server_error(body: String) {
        val result = served(code = 200, body = body) { api -> api.push(CHANGES) }.first
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "zła treść jako błąd serwera")
        assertEquals(200, error.code, "kod odpowiedzi ze złą treścią")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna złej treści")
    }

    private fun <T> served(code: Int, body: String, call: suspend (MarkerChangesApi) -> T): Pair<T, RecordedRequest> {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            return runBlocking { call(api(server)) } to server.takeRequest()
        }
    }

    private fun api(server: MockWebServer): MarkerChangesApi =
        MarkerChangesApi(http = OkHttpClient(), json = JSON, baseUrl = server.url("/").toString())

    private companion object {
        const val FIRST = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        const val SECOND = "7a1e2f3c-9b8d-4c6e-a5f4-3d2c1b0a9e8f"
        const val THIRD = "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        const val MARKER = "3c2b1a09-8f7e-4d6c-b5a4-918273645546"
        const val OPERATION = "/components/schemas/SyncOperation"
        const val OTHER = "9e8d7c6b-5a49-4382-a716-0f1e2d3c4b5a"
        val JSON = NetworkModule.json()
        val CHANGES =
            listOf(
                MarkerChange.Mark(operationId = FIRST, markerId = MARKER, buildingSymbol = "A-1", floorId = "A-1-2"),
                MarkerChange.Mark(operationId = SECOND, markerId = OTHER, buildingSymbol = "A-6", floorId = null),
                MarkerChange.Revoke(operationId = THIRD, markerId = MARKER),
            )
    }
}
