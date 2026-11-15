/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
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
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit

/**
 * Pomieszczenia piętra (`listFloorRooms`) na MockWebServer: odpowiedź bez zmian, pusty obrys, 404,
 * zerwane połączenie i zła treść.
 *
 * Wymagania: US16.1
 */
class FloorRoomsApiTest {
    @Test
    fun returnsRoomsOfFloor() {
        val expected =
            RoomList(
                source = "Politechnika Wrocławska",
                floorOutline = SQUARE_AREA,
                rooms =
                    listOf(
                        room(id = "1", number = "101", function = "Winda", category = RoomCategory.ELEVATOR)
                            .copy(liftId = "w1", liftFloors = listOf("K1.C13.P0", "K1.C13.P2")),
                        room(id = "2", number = null, function = "Korytarz", category = RoomCategory.CORRIDOR),
                    ),
            )
        val (result, path) = served(json(HTTP_OK, ROOMS)) { rooms(symbol = "C-13", floorId = "K1.C13.P0") }
        assertEquals(ApiResult.Success(expected), result, "pomieszczenia piętra i piętra windy bez zmian")
        assertEquals("/buildings/C-13/floors/K1.C13.P0/rooms", path, "adres pomieszczeń piętra")
    }

    @Test
    fun readsFloorWithoutRooms() {
        val body = """{"source":"Politechnika Wrocławska","floorOutline":$EMPTY,"rooms":[]}"""
        val result = served(json(HTTP_OK, body)) { rooms(symbol = "C-13", floorId = "P9") }.first
        val empty =
            RoomList(
                source = "Politechnika Wrocławska",
                floorOutline = GeoMultiPolygon(emptyList()),
                rooms = emptyList(),
            )
        assertEquals(ApiResult.Success(empty), result, "piętro bez pomieszczeń z pustym obrysem")
    }

    @Test
    fun mapsMissingFloorToNotFound() {
        val result = served(json(HTTP_NOT_FOUND, """{"status":404}""")) { rooms(symbol = "C-99", floorId = "P0") }
        assertEquals(ApiResult.NotFound, result.first, "404 jako brak piętra")
    }

    @Test
    fun reportsLostConnection() {
        val lost = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        val result = served(lost) { rooms(symbol = "C-13", floorId = "P0") }.first
        assertInstanceOf(ApiResult.NetworkError::class.java, result, "zerwane połączenie jako błąd sieci")
    }

    @Test
    fun rejectsUnknownCategory() {
        val body = ROOMS.replace(oldValue = "ELEVATOR", newValue = "GARAGE")
        val result = served(json(HTTP_OK, body)) { rooms(symbol = "C-13", floorId = "P0") }.first
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "nieznana grupa jako błąd serwera")
        assertInstanceOf(IllegalArgumentException::class.java, error.cause, "przyczyna nieznanej grupy")
    }

    private fun room(id: String, number: String?, function: String, category: RoomCategory): Room =
        Room(
            id = id,
            number = number,
            function = function,
            category = category,
            geometry = SQUARE_AREA,
            labelPoint = GeoPoint(longitude = 0.5, latitude = 0.25),
            isLabelled = number != null,
        )

    private fun <T> served(response: MockResponse, call: suspend BuildingsApi.() -> T): Pair<T, String?> =
        MockWebServer().use { server ->
            server.enqueue(response)
            server.start()
            val api = BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString())
            runBlocking { api.call() } to server.takeRequest(timeout = 1, unit = TimeUnit.SECONDS)?.path
        }

    private fun json(code: Int, body: String): MockResponse =
        MockResponse().setResponseCode(code).setHeader(name = "Content-Type", value = "application/json").setBody(body)

    private companion object {
        val SQUARE_AREA =
            GeoMultiPolygon(
                listOf(
                    GeoPolygon(
                        listOf(
                            listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0).map { (lon, lat) ->
                                GeoPoint(longitude = lon, latitude = lat)
                            },
                        ),
                    ),
                ),
            )
        const val SQUARE = """{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,0]]]]}"""
        const val EMPTY = """{"type":"MultiPolygon","coordinates":[]}"""
        const val POINT = """{"type":"Point","coordinates":[0.5,0.25]}"""
        const val ROOMS =
            """{"source":"Politechnika Wrocławska","floorOutline":$SQUARE,"rooms":[""" +
                """{"id":"1","number":"101","function":"Winda","category":"ELEVATOR",""" +
                """"geometry":$SQUARE,"labelPoint":$POINT,"labelled":true,""" +
                """"liftId":"w1","liftFloors":["K1.C13.P0","K1.C13.P2"]},""" +
                """{"id":"2","function":"Korytarz","category":"CORRIDOR",""" +
                """"geometry":$SQUARE,"labelPoint":$POINT,"labelled":false}]}"""
    }
}
