/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Postęp i cofnięcie znacznika jako odpowiedź serwera bez typów klienta HTTP.
 *
 * Wymagania: US11.1, US11.3, BR-21
 */
class InspectionProgressTest {
    @Test
    fun us11_1_progress_is_ok() {
        val marker =
            InspectionMarker(
                id = MARKER,
                buildingSymbol = "B-4",
                place = MarkerPlace.WholeBuilding,
                createdAt = Instant.parse("2026-11-10T09:30:00Z"),
                isRevoked = false,
                isMine = false,
            )
        val expected =
            BuildingProgress(
                buildingSymbol = "B-4",
                coverage = InspectionCoverage.Whole(isInspected = false),
                pendingSubmissions = 0,
                acceptedSubmissions = 1,
                entrancesTotal = 2,
                entrancesCheckedOnSite = 0,
                markers = listOf(ProgressMarker(marker = marker, authorPseudonym = "Autor 14")),
            )
        assertEquals(ServerReply.Ok(listOf(expected)), answered(200, PROGRESS) { it.buildings() }, "postęp z serwera")
    }

    @Test
    fun us11_1_forbidden_is_fault() {
        assertEquals(ServerReply.Fault(FORBIDDEN), answered(FORBIDDEN, "{}") { it.buildings() }, "403 przy odczycie")
    }

    @Test
    fun us11_3_revoke_is_ok() {
        assertEquals(ServerReply.Ok(Unit), answered(NO_CONTENT, "") { it.revoke(MARKER) }, "204 przy cofnięciu")
    }

    @Test
    fun us11_3_unknown_marker_missing() {
        assertEquals(ServerReply.Missing, answered(NOT_FOUND, "{}") { it.revoke(MARKER) }, "nieznany znacznik")
    }

    @Test
    fun us11_3_revoke_forbidden_fault() {
        val reply = answered(FORBIDDEN, "{}") { it.revoke(MARKER) }
        assertEquals(ServerReply.Fault(FORBIDDEN), reply, "403 przy cofnięciu")
    }

    @Test
    fun us11_3_offline_revoke() {
        val offline =
            MockWebServer().use { server ->
                server.start()
                val url = server.url("/").toString()
                server.shutdown()
                runBlocking { source(url).revoke(MARKER) }
            }
        assertEquals(ServerReply.Offline, offline, "brak połączenia")
    }

    private fun <T> answered(code: Int, body: String, call: suspend (InspectionProgress) -> T): T =
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            runBlocking { call(source(server.url("/").toString())) }
        }

    private fun source(url: String): InspectionProgress =
        inspectionProgressOf(InspectionProgressApi(OkHttpClient(), Json { ignoreUnknownKeys = true }, url))

    private companion object {
        const val MARKER = "3c2b1a09-8f7e-4d6c-b5a4-918273645546"
        const val PROGRESS =
            """[{"buildingSymbol":"B-4","detailLevel":"BASIC","inspected":false,"pendingSubmissions":0,
                "acceptedSubmissions":1,"entrancesTotal":2,"entrancesCheckedOnSite":0,
                "markers":[{"id":"$MARKER","buildingSymbol":"B-4",
                "createdAt":"2026-11-10T09:30:00Z","revoked":false,"mine":false,"authorPseudonym":"Autor 14"}]}]"""
        const val NO_CONTENT = 204
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
    }
}
