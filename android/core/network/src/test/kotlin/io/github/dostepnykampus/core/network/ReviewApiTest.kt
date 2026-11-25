/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PRECISE
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PROBLEM
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.SERVER_DATA
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.rejected
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.review.ReviewApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Trasy sprawdzania zgłoszeń na MockWebServer: kolejka, wzięcie, decyzja i podobne zgłoszenia, każda odpowiedź
 * z kontraktu `api/openapi.yaml`.
 *
 * Wymagania: US10.1, US10.2, US10.3, US10.4, US10.7, US10.8, BR-3, BR-11, BR-13
 */
class ReviewApiTest {
    private val server = MockWebServer()
    private val json = NetworkModule.json()
    private val sent = rejected.copy(status = SubmissionStatus.SENT, rejectionReason = null)
    private val inReview = sent.copy(status = SubmissionStatus.IN_REVIEW)

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun readsQueue() {
        enqueue(code = 200, body = "[${body(SENT_STATUS)},${body(IN_REVIEW_STATUS)}]")
        assertEquals(ApiResult.Success(listOf(sent, inReview)), blocking { queue() }, "kolejka z serwera")
        assertEquals(listOf("GET", "/review/queue"), route(server.takeRequest()), "metoda i ścieżka kolejki")
    }

    @Test
    fun readsSubmission() {
        enqueue(code = 200, body = body(IN_REVIEW_STATUS))
        assertEquals(ApiResult.Success(inReview), blocking { submission(ID) }, "zgłoszenie do sprawdzenia")
        assertEquals(listOf("GET", "/submissions/$ID"), route(server.takeRequest()), "metoda i ścieżka zgłoszenia")
    }

    @Test
    fun claimsSubmission() {
        enqueue(code = 200, body = body(IN_REVIEW_STATUS))
        assertEquals(ApiResult.Success(ReviewClaim.Claimed(inReview)), blocking { claim(ID) }, "wzięte zgłoszenie")
        assertEquals(listOf("POST", "/submissions/$ID/claim"), route(server.takeRequest()), "metoda i ścieżka")
    }

    @Test
    fun claimTakenByOther() {
        enqueue(code = 409, body = """{"title":"Zajęte","status":409}""")
        assertEquals(ApiResult.Success(ReviewClaim.Taken), blocking { claim(ID) }, "409 przy wzięciu to zajęte")
    }

    @Test
    fun sendsAccept() {
        enqueue(code = 200, body = body(ACCEPTED_STATUS))
        val accept = ReviewDecision.AcceptQuick(version = 2, barrierType = "TB-3", severity = 2)
        val result = blocking { decide(ID, accept) }
        val decided = assertInstanceOf(ApiResult.Success::class.java, result, "przyjęcie zapisane").value
        assertInstanceOf(ReviewOutcome.Decided::class.java, decided, "decyzja zapadła")
        val request = server.takeRequest()
        assertEquals(listOf("POST", "/submissions/$ID/review"), route(request), "metoda i ścieżka decyzji")
        assertDecision("""{"decision":"ACCEPT","version":2,"barrierType":"TB-3","severity":2}""", request)
    }

    @Test
    fun reportsChangedVersion() {
        enqueue(code = 409, body = """{$PROBLEM,"serverSubmission":${body(IN_REVIEW_STATUS)}}""")
        val result = blocking { decide(ID, ReviewDecision.Accept(version = 1)) }
        assertEquals(ApiResult.Success(ReviewOutcome.Changed(inReview)), result, "409 z wersją z serwera")
    }

    @Test
    fun changedWithoutServerIsError() {
        enqueue(code = 409, body = "{$PROBLEM}")
        assertContentError(409, blocking { decide(ID, ReviewDecision.Accept(version = 1)) })
    }

    @Test
    fun readsSimilar() {
        val similar = body(SENT_STATUS).removeSuffix("}") + ""","distanceMeters":4.5}"""
        enqueue(code = 200, body = "[$similar]")
        val expected = listOf(SimilarSubmission(submission = sent, distanceMeters = 4.5))
        assertEquals(ApiResult.Success(expected), blocking { similar(ID) }, "podobne zgłoszenia z odległością")
        assertEquals(listOf("GET", "/submissions/$ID/similar"), route(server.takeRequest()), "metoda i ścieżka")
    }

    @Test
    fun similarWithoutDistance() {
        enqueue(code = 200, body = "[${body(SENT_STATUS)}]")
        assertContentError(200, blocking { similar(ID) })
    }

    @Test
    fun mapsErrorCodes() {
        listOf(401, 403).forEach { code ->
            assertFault(code) { queue() }
            assertFault(code) { claim(ID) }
            assertFault(code) { decide(ID, REJECT) }
            assertFault(code) { similar(ID) }
        }
        assertFault(400) { decide(ID, REJECT) }
        assertFault(413) { decide(ID, REJECT) }
        assertFault(429) { claim(ID) }
        assertFault(429) { decide(ID, REJECT) }
    }

    @Test
    fun mapsMissingSubmission() {
        assertMissing { submission(ID) }
        assertMissing { claim(ID) }
        assertMissing { decide(ID, REJECT) }
        assertMissing { similar(ID) }
    }

    private fun assertFault(code: Int, call: suspend ReviewApi.() -> ApiResult<*>) {
        enqueue(code = code, body = """{"title":"Błąd","status":$code}""")
        assertEquals(ApiResult.ServerError(code), blocking(call), "kod $code jako błąd serwera")
    }

    private fun assertMissing(call: suspend ReviewApi.() -> ApiResult<*>) {
        enqueue(code = 404, body = """{"title":"Brak","status":404}""")
        assertEquals(ApiResult.NotFound, blocking(call), "404 to brak zgłoszenia")
    }

    private fun assertDecision(expected: String, request: RecordedRequest) {
        val sentBody = request.body.readUtf8()
        assertEquals(json.parseToJsonElement(expected), json.parseToJsonElement(sentBody), "treść decyzji")
    }

    private fun assertContentError(code: Int, result: ApiResult<*>) {
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "błąd treści odpowiedzi")
        assertEquals(code, error.code, "kod odpowiedzi błędnej treści")
        assertNotNull(error.cause, "błąd treści bez przyczyny")
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun <T> blocking(call: suspend ReviewApi.() -> T): T =
        runBlocking { ReviewApi(OkHttpClient(), json, server.url("/").toString()).call() }

    private fun route(request: RecordedRequest): List<String?> = listOf(request.method, request.path)

    private companion object {
        const val SENT_STATUS = """"status":"SENT","""
        const val IN_REVIEW_STATUS = """"status":"IN_REVIEW","""
        const val ACCEPTED_STATUS = """"status":"ACCEPTED","verifiedAt":"2026-11-04T09:30:00Z","""
        val REJECT = ReviewDecision.Reject(version = 1, reason = "Nieaktualne")

        fun body(status: String): String = "{$SERVER_DATA$status$PRECISE}"
    }
}
