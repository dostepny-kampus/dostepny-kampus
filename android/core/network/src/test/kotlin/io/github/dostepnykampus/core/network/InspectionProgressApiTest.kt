/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Postęp inwentaryzacji (`GET /progress`) i cofnięcie znacznika (`DELETE /inspection-markers/{markerId}`).
 *
 * Wymagania: US11.1, US11.3, BR-21
 */
class InspectionProgressApiTest {
    @Test
    fun us11_1_progress_is_get() {
        val (result, sent) = served(code = 200, body = PROGRESS) { it.progress() }
        val expected =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 3),
                pendingSubmissions = 2,
                acceptedSubmissions = 7,
                entrancesTotal = 4,
                entrancesCheckedOnSite = 1,
                markers = emptyList(),
            )
        assertEquals(ApiResult.Success(listOf(expected)), result, "postęp budynku szczegółowego")
        assertEquals(listOf("GET", "/progress"), listOf(sent.method, sent.path), "metoda i ścieżka żądania")
    }

    @Test
    fun us11_1_basic_building() {
        val body =
            """[{"buildingSymbol":"B-4","detailLevel":"BASIC","inspected":true,"pendingSubmissions":0,
                "acceptedSubmissions":1,"entrancesTotal":2,"entrancesCheckedOnSite":2,"markers":[]}]"""
        val expected =
            BuildingProgress("B-4", InspectionCoverage.Whole(isInspected = true), 0, 1, 2, 2, emptyList())
        val result = served(code = 200, body = body) { it.progress() }.first
        assertEquals(ApiResult.Success(listOf(expected)), result, "postęp budynku podstawowego")
    }

    @Test
    fun us11_3_revoke_is_delete() {
        val (result, sent) = served(code = 204, body = "") { it.revoke(MARKER) }
        assertEquals(ApiResult.Success(Unit), result, "wynik cofnięcia")
        val line = listOf(sent.method, sent.path)
        assertEquals(listOf("DELETE", "/inspection-markers/$MARKER"), line, "metoda i ścieżka żądania")
    }

    @Test
    fun us11_3_unknown_marker_is_not_found() {
        val result = served(code = 404, body = """{"type":"about:blank"}""") { it.revoke(MARKER) }.first
        assertEquals(ApiResult.NotFound, result, "znacznika nie ma na serwerze")
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403, 500])
    fun us11_3_refusal_is_server_error(code: Int) {
        val result = served(code = code, body = """{"type":"about:blank"}""") { it.revoke(MARKER) }.first
        assertEquals(ApiResult.ServerError(code), result, "kod $code przy cofnięciu")
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 500])
    fun us11_1_refusal_is_server_error(code: Int) {
        val result = served(code = code, body = """{"type":"about:blank"}""") { it.progress() }.first
        assertEquals(ApiResult.ServerError(code), result, "kod $code przy odczycie postępu")
    }

    @Test
    fun us11_1_bad_body_is_server_error() {
        val result = served(code = 200, body = "to nie JSON") { it.progress() }.first
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "zła treść jako błąd serwera")
        assertEquals(200, error.code, "kod odpowiedzi ze złą treścią")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna złej treści")
    }

    private fun <T> served(
        code: Int,
        body: String,
        call: suspend (InspectionProgressApi) -> T,
    ): Pair<T, RecordedRequest> {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            val api = InspectionProgressApi(OkHttpClient(), NetworkModule.json(), server.url("/").toString())
            return runBlocking { call(api) } to server.takeRequest()
        }
    }

    private companion object {
        const val MARKER = "3c2b1a09-8f7e-4d6c-b5a4-918273645546"
        const val PROGRESS =
            """[{"buildingSymbol":"A-1","detailLevel":"DETAILED","floorsTotal":5,"floorsInspected":3,
                "pendingSubmissions":2,"acceptedSubmissions":7,"entrancesTotal":4,"entrancesCheckedOnSite":1,
                "markers":[]}]"""
    }
}
