/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.Floor
import io.github.dostepnykampus.core.model.FloorList
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

/** Odczyty budynków i pięter na MockWebServer: sukces, 404, zły JSON, zerwane połączenie i błąd serwera. */
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
    fun us1_list() {
        enqueue(HTTP_OK, LIST)
        val list = json.decodeFromString<BuildingListDto>(LIST).toModel()
        assertEquals(ApiResult.Success(list), blocking { listBuildings() })
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/buildings", recorded.path)
        assertEquals(JSON, recorded.getHeader("Accept"))
    }

    @Test
    fun us2_building() {
        enqueue(HTTP_OK, DETAILS)
        val card = json.decodeFromString<BuildingDetailsDto>(DETAILS).toModel()
        assertEquals(ApiResult.Success(card), blocking { building("C-13") })
        assertEquals("/buildings/C-13", server.takeRequest().path)
    }

    @Test
    fun us6_2_locate() {
        enqueue(HTTP_OK, """{"buildingSymbol":"C-13","insideOutline":true}""")
        val inside = blocking { locate(latitude = 51.0, longitude = 17.0) }
        val expected = json.decodeFromString<LocateResultDto>("""{"buildingSymbol":"C-13","insideOutline":true}""")
        assertEquals(ApiResult.Success(expected.toModel()), inside)
        assertEquals("/buildings/locate?lat=51.0&lon=17.0", server.takeRequest().path)
        enqueue(HTTP_OK, """{"insideOutline":false}""")
        val outside = json.decodeFromString<LocateResultDto>("""{"insideOutline":false}""").toModel()
        assertEquals(ApiResult.Success(outside), blocking { locate(latitude = 51.0, longitude = 17.0) })
    }

    @Test
    fun us7_2_floors() {
        enqueue(HTTP_OK, FLOORS)
        val floors = blocking { floors("C-13") }
        val lowest =
            listOf("c13-p" to "Piwnica", "c13-0" to "Parter", "c13-1" to "1 piętro")
                .mapIndexed { index, (id, label) -> Floor(id = id, label = label, order = index - 1) }
        assertEquals(ApiResult.Success(FloorList(source = "Politechnika Wrocławska", floors = lowest)), floors)
        assertEquals("/buildings/C-13/floors", server.takeRequest().path)
        enqueue(code = HTTP_NOT_FOUND, body = """{"status":404}""", contentType = PROBLEM)
        assertEquals(ApiResult.NotFound, blocking { floors("C-99") })
    }

    @Test
    fun not_found() {
        enqueue(
            code = HTTP_NOT_FOUND,
            body = """{"type":"about:blank","title":"Not Found","status":404}""",
            contentType = PROBLEM,
        )
        assertEquals(ApiResult.NotFound, blocking { building("C-99") })
    }

    @Test
    fun malformed_json() {
        enqueue(HTTP_OK, "{")
        val error = assertInstanceOf(ApiResult.ServerError::class.java, blocking { listBuildings() })
        assertEquals(HTTP_OK, error.code)
        assertInstanceOf(IllegalArgumentException::class.java, error.cause)
    }

    @Test
    fun connection_dropped() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertInstanceOf(ApiResult.NetworkError::class.java, blocking { listBuildings() })
    }

    @Test
    fun server_error() {
        enqueue(HTTP_INTERNAL_ERROR, """{"status":500}""")
        val failed = blocking { locate(latitude = 51.0, longitude = 17.0) }
        val error = assertInstanceOf(ApiResult.ServerError::class.java, failed)
        assertEquals(HTTP_INTERNAL_ERROR, error.code)
        assertNull(error.cause)
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
        const val OUTLINE =
            """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""
        const val LIST =
            """{"source":"© OpenStreetMap contributors","buildings":[""" +
                """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"DETAILED","outline":$OUTLINE}]}"""
        const val FLOORS =
            """{"source":"Politechnika Wrocławska","floors":[{"id":"c13-1","label":"1 piętro","order":1},""" +
                """{"id":"c13-p","label":"Piwnica","order":-1},{"id":"c13-0","label":"Parter","order":0}]}"""
        const val DETAILS =
            """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"BASIC","outline":$OUTLINE,""" +
                """"entrances":[{"id":"c13-main","buildingSymbol":"C-13","name":"Wejście główne","main":true,""" +
                """"location":{"type":"Point","coordinates":[17,51]},"marking":"ACCESSIBLE",""" +
                """"markingDate":"2026-10-01","markingSource":"IMPORT"}]}"""
    }
}
