/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.review

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.review.ReviewApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Sprawdzanie zgłoszeń jako odpowiedź serwera bez typów klienta HTTP: podział kolejki, zajęte zgłoszenie,
 * nowa wersja przy decyzji, brak sieci, brak zgłoszenia i brak uprawnień.
 *
 * Wymagania: US10.1, US10.4, US10.7, US10.8, BR-11
 */
class ReviewsTest {
    @Test
    fun us10QueueSplitsMine() {
        val reply = answered(OK, "[${body(SENT)},${body(IN_REVIEW)}]") { it.queue() }
        val expected = ReviewQueue(waiting = listOf(submission(SENT)), mine = listOf(submission(IN_REVIEW)))
        assertEquals(ServerReply.Ok(expected), reply, "kolejka podzielona na do wzięcia i wzięte przeze mnie")
    }

    @Test
    fun us10ClaimTaken() {
        val reply = answered(CONFLICT, """{"title":"Zajęte","status":409}""") { it.claim(ID) }
        assertEquals(ServerReply.Ok(ReviewClaim.Taken), reply, "zgłoszenie wziął inny Weryfikator")
    }

    @Test
    fun us10ClaimOk() {
        val reply = answered(OK, body(IN_REVIEW)) { it.claim(ID) }
        assertEquals(ServerReply.Ok(ReviewClaim.Claimed(submission(IN_REVIEW))), reply, "zgłoszenie wzięte")
    }

    @Test
    fun us10DecisionChanged() {
        val conflict = """{"title":"Konflikt","status":409,"serverSubmission":${body(IN_REVIEW)}}"""
        val reply = answered(CONFLICT, conflict) { it.decide(ID, ReviewDecision.Accept(version = 1)) }
        assertEquals(ServerReply.Ok(ReviewOutcome.Changed(submission(IN_REVIEW))), reply, "autor zmienił zgłoszenie")
    }

    @Test
    fun us10SimilarOk() {
        val similar = body(SENT).removeSuffix("}") + ""","distanceMeters":5.0}"""
        val reply = answered(OK, "[$similar]") { it.similar(ID) }
        val expected = listOf(SimilarSubmission(submission = submission(SENT), distanceMeters = 5.0))
        assertEquals(ServerReply.Ok(expected), reply, "podobne zgłoszenie w 5 m")
    }

    @Test
    fun us10SubmissionAndPhoto() {
        val reply = answered(OK, body(IN_REVIEW)) { it.submission(ID) }
        assertEquals(ServerReply.Ok(submission(IN_REVIEW)), reply, "zgłoszenie do sprawdzenia")
        val url = source("http://serwer/").photoUrl(ID, PHOTO)
        assertEquals("http://serwer/$ID/$PHOTO", url, "adres zdjęcia z adresu klienta zgłoszeń")
    }

    @Test
    fun us10MissingAndForbidden() {
        assertEquals(ServerReply.Missing, answered(NOT_FOUND, "{}") { it.similar(ID) }, "nieznane zgłoszenie")
        assertEquals(ServerReply.Fault(FORBIDDEN), answered(FORBIDDEN, "{}") { it.queue() }, "403 bez roli")
    }

    @Test
    fun us10OfflineDecision() {
        val offline =
            MockWebServer().use { server ->
                server.start()
                val url = server.url("/").toString()
                server.shutdown()
                runBlocking { source(url).decide(ID, ReviewDecision.Reject(version = 1, reason = "Nieaktualne")) }
            }
        assertEquals(ServerReply.Offline, offline, "brak połączenia przy decyzji")
    }

    private fun <T> answered(code: Int, body: String, call: suspend (Reviews) -> T): T =
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            server.start()
            runBlocking { call(source(server.url("/").toString())) }
        }

    private fun source(url: String): Reviews =
        reviewsOf(ReviewApi(OkHttpClient(), Json { ignoreUnknownKeys = true }, url)) { id, photo -> "$url$id/$photo" }

    private companion object {
        const val ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        const val PHOTO = "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        const val SENT = "SENT"
        const val IN_REVIEW = "IN_REVIEW"
        const val OK = 200
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val CONFLICT = 409

        fun body(status: String): String =
            """{"id":"$ID","version":1,"status":"$status","createdAt":"2026-11-03T18:00:00Z","kind":"QUICK",""" +
                """"location":{"type":"Point","coordinates":[17.06,51.1]},"barrierGroup":"OTHER",""" +
                """"photoIds":["$PHOTO"]}"""

        fun submission(status: String): Submission =
            Submission(
                id = ID,
                version = 1,
                status = SubmissionStatus.valueOf(status),
                createdAt = Instant.parse("2026-11-03T18:00:00Z"),
                content =
                    SubmissionContent(
                        kind = SubmissionKind.QUICK,
                        location = GeoPoint(longitude = 17.06, latitude = 51.1),
                        photoIds = listOf(PHOTO),
                        barrierGroup = BarrierGroup.OTHER,
                    ),
            )
    }
}
