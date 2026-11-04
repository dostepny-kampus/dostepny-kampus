/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.core.network.BuildingsApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Wejścia karty z grafu aplikacji pytają serwer o kartę budynku (US17.3). */
class CardEntrancesTest {
    private val server = MockWebServer()

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    // Wiersz: V2 budynku, który zna serwer → wejścia karty w aplikacji pochodzą z `GET /buildings/{symbol}`
    //   z dniem i źródłem oznaczenia, US17.3.
    @Test
    fun us17_3_app_asks_server() {
        server.enqueue(MockResponse().setHeader(name = "Content-Type", value = "application/json").setBody(CARD))
        val api = BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString())
        val entrances = runBlocking { DataModule.cardEntrances(api).entrances("A-1") }
        assertEquals(
            listOf(EntranceAssessment(date = LocalDate.of(2026, 10, 1), source = AssessmentSource.WALKTHROUGH)),
            entrances.map { it.assessment },
        )
    }

    private companion object {
        const val CARD =
            """{"symbol":"A-1","name":"Gmach","detailLevel":"BASIC",""" +
                """"outline":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]},"entrances":[""" +
                """{"id":"a1-main","buildingSymbol":"A-1","name":"Wejście główne","main":true,""" +
                """"location":{"type":"Point","coordinates":[0.5,0]},"marking":"ACCESSIBLE",""" +
                """"markingDate":"2026-10-01","markingSource":"WALKTHROUGH"}]}"""
    }
}
