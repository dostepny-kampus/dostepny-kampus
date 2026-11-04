/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.EntranceAssessment
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Dzień i źródło oznaczenia wejścia z karty budynku `GET /buildings/{symbol}` (US17.3). */
class EntranceAssessmentTest {
    private val server = MockWebServer()

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    // Wiersz: karta budynku z serwera → każde wejście ma w modelu oznaczenie z dniem i źródłem: import albo
    //   obchód, US17.3, BR-24.
    @Test
    fun us17_3_assessment_kept() {
        val details =
            success(card(date = "2026-10-01", source = "IMPORT", second = "2026-11-03", secondSource = "WALKTHROUGH"))
        assertEquals(
            listOf(
                EntranceAssessment(date = LocalDate.of(2026, 10, 1), source = AssessmentSource.IMPORT),
                EntranceAssessment(date = LocalDate.of(2026, 11, 3), source = AssessmentSource.WALKTHROUGH),
            ),
            details.entrances.map { it.assessment },
        )
    }

    // Wiersz: dzień, którego nie ma w kalendarzu, albo nieznane źródło oznaczenia → błąd treści, nie wejście,
    //   US17.3.
    @Test
    fun us17_3_bad_assessment() {
        val bodies = mapOf("dzień 2026-02-30" to card(date = "2026-02-30"), "źródło SURVEY" to card(source = "SURVEY"))
        bodies.forEach { (input, body) -> assertInstanceOf(ApiResult.ServerError::class.java, fetch(body), input) }
    }

    private fun success(body: String): BuildingDetails {
        val result = fetch(body)
        return if (result is ApiResult.Success) result.value else fail("oczekiwano karty budynku, było $result")
    }

    private fun fetch(body: String): ApiResult<BuildingDetails> {
        server.enqueue(MockResponse().setHeader(name = "Content-Type", value = "application/json").setBody(body))
        val api = BuildingsApi(http = OkHttpClient(), json = Json, baseUrl = server.url("/").toString())
        return runBlocking { api.building("C-13") }
    }

    private fun card(
        date: String = "2026-10-01",
        source: String = "IMPORT",
        second: String = date,
        secondSource: String = source,
    ): String =
        """{"symbol":"C-13","name":"Budynek C-13","detailLevel":"BASIC","outline":$OUTLINE,"entrances":[""" +
            entrance(id = "c13-main", date = date, source = source) +
            "," +
            entrance(id = "c13-side", date = second, source = secondSource) +
            "]}"

    private fun entrance(id: String, date: String, source: String): String =
        """{"id":"$id","buildingSymbol":"C-13","main":true,"location":{"type":"Point","coordinates":[17,51]},""" +
            """"marking":"ACCESSIBLE","markingDate":"$date","markingSource":"$source"}"""

    private companion object {
        const val OUTLINE = """{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,52],[17,51]]]}"""
    }
}
