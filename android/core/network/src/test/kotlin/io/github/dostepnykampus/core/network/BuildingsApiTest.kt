/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.building.decodeBuildingDetails
import io.github.dostepnykampus.core.network.building.decodeBuildingList
import io.github.dostepnykampus.core.network.building.decodeLocateResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

/**
 * Odczyty budynków i pięter na MockWebServer: sukces, 404, zły JSON, kontur niebędący wielokątem,
 * zerwane połączenie i błąd serwera.
 *
 * Wymagania: US1, US2, US6.2, US7.2
 */
class BuildingsApiTest {
    private val server = MockWebServer()
    private val json = Json

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun fetchesList() {
        enqueue(HTTP_OK, BUILDING_LIST_JSON)
        val list = json.decodeBuildingList(BUILDING_LIST_JSON)
        assertEquals(ApiResult.Success(list), blocking { listBuildings() }, "lista budynków z serwera")
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method, "metoda żądania listy")
        assertEquals("/buildings", recorded.path, "adres listy")
        assertEquals(JSON, recorded.getHeader("Accept"), "nagłówek Accept")
    }

    @Test
    fun fetchesBuilding() {
        enqueue(HTTP_OK, BUILDING_DETAILS_JSON)
        val card = json.decodeBuildingDetails(BUILDING_DETAILS_JSON)
        assertEquals(ApiResult.Success(card), blocking { building("C-13") }, "karta budynku z serwera")
        assertEquals("/buildings/C-13", server.takeRequest().path, "adres karty")
    }

    @Test
    fun fetchesLocateResult() {
        enqueue(HTTP_OK, """{"buildingSymbol":"C-13","insideOutline":true}""")
        val inside = blocking { locate(latitude = 51.0, longitude = 17.0) }
        val expected = json.decodeLocateResult("""{"buildingSymbol":"C-13","insideOutline":true}""")
        assertEquals(ApiResult.Success(expected), inside, "budynek w punkcie")
        assertEquals("/buildings/locate?lat=51.0&lon=17.0", server.takeRequest().path, "adres wyszukania budynku")
        enqueue(HTTP_OK, """{"insideOutline":false}""")
        val outside = json.decodeLocateResult("""{"insideOutline":false}""")
        assertEquals(
            ApiResult.Success(outside),
            blocking { locate(latitude = 51.0, longitude = 17.0) },
            "brak budynku w punkcie",
        )
    }

    @Test
    fun fetchesFloors() {
        enqueue(HTTP_OK, FLOORS)
        val floors = blocking { floors("C-13") }
        val lowest =
            listOf("c13-p" to "Piwnica", "c13-0" to "Parter", "c13-1" to "1 piętro")
                .mapIndexed { index, (id, label) -> Floor(id = id, label = label, order = index - 1) }
        assertEquals(
            ApiResult.Success(FloorList(source = "Politechnika Wrocławska", floors = lowest)),
            floors,
            "lista pięter z serwera",
        )
        assertEquals("/buildings/C-13/floors", server.takeRequest().path, "adres listy pięter")
        enqueue(code = HTTP_NOT_FOUND, body = """{"status":404}""", contentType = PROBLEM)
        assertEquals(ApiResult.NotFound, blocking { floors("C-99") }, "brak budynku dla pięter")
    }

    @Test
    fun reportsNotFound() {
        enqueue(
            code = HTTP_NOT_FOUND,
            body = """{"type":"about:blank","title":"Not Found","status":404}""",
            contentType = PROBLEM,
        )
        assertEquals(ApiResult.NotFound, blocking { building("C-99") }, "brak budynku na serwerze")
    }

    @Test
    fun reportsMalformedJson() {
        enqueue(HTTP_OK, "{")
        val failed = blocking { listBuildings() }
        val error = assertInstanceOf(ApiResult.ServerError::class.java, failed, "błąd serwera dla złego JSON")
        assertEquals(HTTP_OK, error.code, "kod błędu złego JSON")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna złego JSON")
    }

    @Test
    fun rejectsOutlineNotPolygon() {
        val line = """{"type":"LineString","coordinates":[[17,51],[18,51]]}"""
        enqueue(HTTP_OK, BUILDING_LIST_JSON.replace(oldValue = BUILDING_OUTLINE_JSON, newValue = line))
        val failed = blocking { listBuildings() }
        val error = assertInstanceOf(ApiResult.ServerError::class.java, failed, "błąd serwera dla obrysu")
        assertEquals(HTTP_OK, error.code, "kod błędu obrysu")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna błędu obrysu")
    }

    @Test
    fun reportsDroppedConnection() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertInstanceOf(
            ApiResult.NetworkError::class.java,
            blocking { listBuildings() },
            "błąd sieci po zerwaniu połączenia",
        )
    }

    @Test
    fun reportsServerError() {
        enqueue(HTTP_INTERNAL_ERROR, """{"status":500}""")
        val failed = blocking { locate(latitude = 51.0, longitude = 17.0) }
        val error = assertInstanceOf(ApiResult.ServerError::class.java, failed, "błąd serwera dla kodu 500")
        assertEquals(HTTP_INTERNAL_ERROR, error.code, "kod odpowiedzi serwera")
        assertNull(error.cause, "brak przyczyny błędu serwera")
    }

    private fun api(): BuildingsApi =
        BuildingsApi(http = OkHttpClient(), json = json, baseUrl = server.url("/").toString())

    private fun <T> blocking(call: suspend BuildingsApi.() -> T): T = runBlocking { api().call() }

    private fun enqueue(code: Int, body: String, contentType: String = JSON) {
        val response =
            MockResponse()
                .setResponseCode(code)
                .setHeader(name = "Content-Type", value = contentType)
                .setBody(body)
        server.enqueue(response)
    }

    private companion object {
        const val JSON = "application/json"
        const val PROBLEM = "application/problem+json"
        const val FLOORS =
            """{"source":"Politechnika Wrocławska","floors":[{"id":"c13-1","label":"1 piętro","order":1},""" +
                """{"id":"c13-p","label":"Piwnica","order":-1},{"id":"c13-0","label":"Parter","order":0}]}"""
    }
}
