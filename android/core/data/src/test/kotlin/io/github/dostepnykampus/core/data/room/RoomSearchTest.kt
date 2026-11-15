/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.room

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.network.building.BuildingsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/** Wyszukiwanie sali jako odpowiedź serwera bez typów klienta HTTP. Wymagania: US18.1 */
class RoomSearchTest {
    @Test
    fun givesFoundRooms() {
        val reply = answered(code = 200, body = FOUND)

        assertInstanceOf(ServerReply.Ok::class.java, reply, "Wynik serwera nie dał odpowiedzi Ok")
        val rooms = (reply as ServerReply.Ok<List<FoundRoom>>).value
        assertEquals(listOf("A-1 329 K1.A1.P2"), rooms.map { "${it.building} ${it.room.number} ${it.floor.id}" })
    }

    @Test
    fun reportsServerFault() {
        assertEquals(ServerReply.Fault(SERVER_ERROR), answered(code = SERVER_ERROR, body = "{}"), "Błąd serwera")
    }

    @Test
    fun reportsOffline() {
        val reply =
            MockWebServer().use { server ->
                server.start()
                val url = server.url("/").toString()
                server.shutdown()
                runBlocking { search(url).search("A-1 329") }
            }

        assertEquals(ServerReply.Offline, reply, "Brak połączenia nie dał odpowiedzi Offline")
    }

    private fun answered(code: Int, body: String): ServerReply<List<FoundRoom>> =
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            runBlocking { search(server.url("/").toString()).search("A-1 329") }
        }

    private fun search(url: String): RoomSearch =
        roomSearchOf(BuildingsApi(http = OkHttpClient(), json = Json { ignoreUnknownKeys = true }, baseUrl = url))

    private companion object {
        const val SERVER_ERROR = 500
        const val FOUND =
            """{"source":"Politechnika Wrocławska","results":[{"buildingSymbol":"A-1",
                "floor":{"id":"K1.A1.P2","label":"2 piętro","order":2},
                "room":{"id":"7","number":"329","function":"Sala wykładowa","category":"LECTURE_HALL",
                "geometry":{"type":"MultiPolygon","coordinates":[]},
                "labelPoint":{"type":"Point","coordinates":[17.06,51.1]},"labelled":true}}]}"""
    }
}
