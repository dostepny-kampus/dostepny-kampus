/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.export.ExportFormat
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.export.ExportApi
import io.github.dostepnykampus.core.network.export.ExportFile
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Eksport bieżących zgłoszeń przyjętych (`GET /export.csv`, `GET /export.geojson`).
 *
 * Wymagania: US13.1, US13.2, US13.3
 */
class ExportApiTest {
    @Test
    fun readsCsvWithRows() {
        val (result, sent) = served(code = 200, body = CSV_ROW, format = ExportFormat.CSV)
        assertEquals(ApiResult.Success(ExportFile(CSV_ROW, isEmpty = false)), result, "plik CSV z barierą")
        assertEquals(listOf("GET", "/export.csv"), listOf(sent.method, sent.path), "metoda i ścieżka żądania")
    }

    @Test
    fun csvHeaderOnlyIsEmpty() {
        val result = served(code = 200, body = CSV_HEADER, format = ExportFormat.CSV).first
        assertEquals(ApiResult.Success(ExportFile(CSV_HEADER, isEmpty = true)), result, "CSV z samym nagłówkiem")
    }

    @Test
    fun readsEmptyGeoJson() {
        val (result, sent) = served(code = 200, body = EMPTY_GEOJSON, format = ExportFormat.GEOJSON)
        assertEquals(ApiResult.Success(ExportFile(EMPTY_GEOJSON, isEmpty = true)), result, "GeoJSON bez barier")
        assertEquals("/export.geojson", sent.path, "ścieżka żądania GeoJSON")
    }

    @Test
    fun forbiddenIsServerError() {
        val result = served(code = 403, body = """{"type":"about:blank"}""", format = ExportFormat.CSV).first
        assertEquals(ApiResult.ServerError(403), result, "403 bez roli Koordynatora")
    }

    private fun served(code: Int, body: String, format: ExportFormat): Pair<ApiResult<ExportFile>, RecordedRequest> {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            val api = ExportApi(OkHttpClient(), NetworkModule.json(), server.url("/").toString())
            return runBlocking { api.download(format) } to server.takeRequest()
        }
    }

    private companion object {
        const val CSV_HEADER =
            "submissionId,x,y,barrierType,barrierGroup,severity,buildingSymbol,floor,description,verifiedAt\n"
        const val CSV_ROW = CSV_HEADER + "TB-1,360000.5,5660000.25,STAIRS,ENTRANCE,3,A-1,0,,2026-11-20T10:00:00Z\n"
        const val EMPTY_GEOJSON = """{"type":"FeatureCollection","features":[]}"""
    }
}
