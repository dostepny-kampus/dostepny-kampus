/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.network.building.BuildingsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit

/** Klient wyszukiwania sali `GET /rooms?q=`: budynek, piętro i sala z wyniku. Wymagania: US18.1 */
class RoomSearchApiTest {
    @Test
    fun returnsFoundRoomWithFloor() {
        val (result, path) = served(json(FOUND)) { searchRooms("A-1 329") }

        val expected =
            FoundRoom(
                building = "A-1",
                floor = Floor(id = "K1.A1.P2", label = "2 piętro", order = 2),
                room =
                    Room(
                        id = "7",
                        number = "329",
                        function = "Sala wykładowa",
                        category = RoomCategory.LECTURE_HALL,
                        geometry = GeoMultiPolygon(emptyList()),
                        labelPoint = GeoPoint(longitude = 17.06, latitude = 51.1),
                        isLabelled = true,
                    ),
            )
        assertEquals(ApiResult.Success(listOf(expected)), result, "Wynik nie ma budynku, piętra albo sali")
        assertEquals("/rooms?q=A-1%20329", path, "Zapytanie nie idzie w parametrze q")
    }

    @Test
    fun returnsEmptyListWhenNothingMatches() {
        val (result, _) = served(json("""{"source":"Politechnika Wrocławska","results":[]}""")) { searchRooms("999") }

        assertEquals(ApiResult.Success(emptyList<FoundRoom>()), result, "Brak sali nie dał pustej listy")
    }

    @Test
    fun reportsLostConnection() {
        val response = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        val (result, _) = served(response) { searchRooms("329") }

        assertInstanceOf(ApiResult.NetworkError::class.java, result, "Zerwane połączenie nie dało błędu sieci")
    }

    private fun <T> served(response: MockResponse, call: suspend BuildingsApi.() -> T): Pair<T, String?> =
        MockWebServer().use { server ->
            server.enqueue(response)
            server.start()
            val api = BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString())
            runBlocking { api.call() } to server.takeRequest(timeout = 1, unit = TimeUnit.SECONDS)?.path
        }

    private fun json(body: String): MockResponse =
        MockResponse()
            .setResponseCode(HTTP_OK)
            .setHeader(name = "Content-Type", value = "application/json")
            .setBody(body)

    private companion object {
        const val FOUND =
            """{"source":"Politechnika Wrocławska","results":[{"buildingSymbol":"A-1",""" +
                """"floor":{"id":"K1.A1.P2","label":"2 piętro","order":2},""" +
                """"room":{"id":"7","number":"329","function":"Sala wykładowa","category":"LECTURE_HALL",""" +
                """"geometry":{"type":"MultiPolygon","coordinates":[]},""" +
                """"labelPoint":{"type":"Point","coordinates":[17.06,51.1]},"labelled":true}}]}"""
    }
}
