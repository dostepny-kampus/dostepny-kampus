/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.core.model.path.AccessiblePathList
import io.github.dostepnykampus.core.network.building.BuildingsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.HttpURLConnection.HTTP_OK

/**
 * Odczyt ścieżek dostosowanych (`GET /accessible-paths`) na MockWebServer: odcinki ze źródłem i zła linia.
 *
 * Wymagania: US1.4
 */
class AccessiblePathsApiTest {
    private val server = MockWebServer()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun fetchesPaths() {
        enqueue(list(line("[[17.06,51.107],[17.061,51.108]]")))
        val expected =
            AccessiblePathList(
                source = "Politechnika Wrocławska",
                paths =
                    listOf(
                        AccessiblePath(
                            id = "p-1",
                            line =
                                listOf(
                                    GeoPoint(longitude = 17.06, latitude = 51.107),
                                    GeoPoint(longitude = 17.061, latitude = 51.108),
                                ),
                        ),
                    ),
            )
        assertEquals(ApiResult.Success(expected), blocking(), "odcinki ścieżek z serwera")
        val recorded = server.takeRequest()
        assertEquals("/accessible-paths", recorded.path, "adres listy ścieżek")
        assertEquals("application/json", recorded.getHeader("Accept"), "nagłówek Accept")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"type":"Point","coordinates":[[17.06,51.107],[17.061,51.108]]}""",
            """{"type":"LineString","coordinates":[[17.06,51.107]]}""",
            """{"type":"LineString","coordinates":[[17.06],[17.061,51.108]]}""",
        ],
    )
    fun rejectsBrokenLine(line: String) {
        enqueue(list(line))
        val error = assertInstanceOf(ApiResult.ServerError::class.java, blocking(), "błąd serwera dla złej linii")
        assertEquals(HTTP_OK, error.code, "kod błędu złej linii")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna błędu złej linii")
    }

    private fun blocking(): ApiResult<AccessiblePathList> =
        runBlocking {
            BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString()).accessiblePaths()
        }

    private fun enqueue(body: String) {
        val response =
            MockResponse()
                .setResponseCode(HTTP_OK)
                .setHeader(name = "Content-Type", value = "application/json")
                .setBody(body)
        server.enqueue(response)
    }

    private fun line(coordinates: String): String = """{"type":"LineString","coordinates":$coordinates}"""

    private fun list(line: String): String =
        """{"source":"Politechnika Wrocławska","paths":[{"id":"p-1","line":$line}]}"""
}
