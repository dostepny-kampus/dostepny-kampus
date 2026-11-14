/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.inspection.InspectionMarkersApi
import io.github.dostepnykampus.core.network.inspection.MarkerOperation
import io.github.dostepnykampus.core.network.inspection.MarkerOutcome
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** Wysyłka zmian znaczników piętra obejrzanego przez `POST /sync/push` według `api/openapi.yaml`. */
class InspectionMarkersApiTest {
    private val server = MockWebServer()
    private val json = NetworkModule.json()
    private val spec = OpenApiSpec(Path.of(requireNotNull(System.getProperty("openapi.file"))))

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    /** Znacznik piętra, znacznik budynku podstawowego i cofnięcie w jednej paczce zgodnej ze schematem. */
    @Test
    fun us15_3_push_matches_contract() {
        reply(code = 200, body = """{"results":[]}""")
        push(listOf(SET, SET_BUILDING, REVOKE))
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals(listOf("POST", "/sync/push"), listOf(request.method, request.path), "metoda i ścieżka")
        assertEquals(emptyList<String>(), spec.violations(pointer = PUSH_BODY, json = body), "zgodność ze schematem")
        val expected =
            """{"operations":[""" +
                """{"clientOperationId":"$OP_1","type":"SET_INSPECTION_MARKER",""" +
                """"marker":{"id":"$MARKER_1","buildingSymbol":"A-1","floorId":"$FLOOR"}},""" +
                """{"clientOperationId":"$OP_2","type":"SET_INSPECTION_MARKER",""" +
                """"marker":{"id":"$MARKER_2","buildingSymbol":"B-4"}},""" +
                """{"clientOperationId":"$OP_3","type":"REVOKE_INSPECTION_MARKER","markerId":"$MARKER_1"}]}"""
        assertEquals(json.parseToJsonElement(expected), json.parseToJsonElement(body), "treść paczki")
    }

    /** Wynik każdej operacji według jej identyfikatora: zapisana albo odrzucona z powodem. */
    @Test
    fun us15_3_outcomes_by_operation() {
        val rejected = """{"clientOperationId":"$OP_3","outcome":"REJECTED","problem":$PROBLEM}"""
        reply(code = 200, body = """{"results":[{"clientOperationId":"$OP_1","outcome":"APPLIED"},$rejected]}""")
        val expected = mapOf(OP_1 to MarkerOutcome.APPLIED, OP_3 to MarkerOutcome.REJECTED)
        assertEquals(ApiResult.Success(expected), push(listOf(SET, REVOKE)), "wyniki operacji")
    }

    /** Wynik spoza kontraktu to błąd treści z przyczyną, bez wyjątku. */
    @Test
    fun us15_3_unknown_outcome_error() {
        reply(code = 200, body = """{"results":[{"clientOperationId":"$OP_1","outcome":"POSTPONED"}]}""")
        val error = assertInstanceOf(ApiResult.ServerError::class.java, push(listOf(SET)), "błąd treści")
        assertEquals(200, error.code, "kod odpowiedzi")
        assertNotNull(error.cause, "przyczyna błędu treści")
    }

    private fun reply(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun push(operations: List<MarkerOperation>): ApiResult<Map<String, MarkerOutcome>> =
        runBlocking { InspectionMarkersApi(OkHttpClient(), json, server.url("/").toString()).push(operations) }

    private companion object {
        const val PUSH_BODY = "/paths/~1sync~1push/post/requestBody/content/application~1json/schema"
        const val OP_1 = "1d0f9a4e-3b2c-4a5d-8e6f-7a8b9c0d1e2f"
        const val OP_2 = "2e1a0b5f-4c3d-4b6e-9f7a-8b9c0d1e2f3a"
        const val OP_3 = "3f2b1c6a-5d4e-4c7f-8a8b-9c0d1e2f3a4b"
        const val MARKER_1 = "4a3c2d7b-6e5f-4d8a-9b9c-0d1e2f3a4b5c"
        const val MARKER_2 = "5b4d3e8c-7f6a-4e9b-8c0d-1e2f3a4b5c6d"
        const val FLOOR = "A-1-3"
        const val PROBLEM = """{"type":"about:blank","title":"Brak roli","status":403}"""
        val SET = MarkerOperation.Set(operationId = OP_1, markerId = MARKER_1, buildingSymbol = "A-1", floorId = FLOOR)
        val SET_BUILDING =
            MarkerOperation.Set(operationId = OP_2, markerId = MARKER_2, buildingSymbol = "B-4", floorId = null)
        val REVOKE = MarkerOperation.Revoke(operationId = OP_3, markerId = MARKER_1)
    }
}
