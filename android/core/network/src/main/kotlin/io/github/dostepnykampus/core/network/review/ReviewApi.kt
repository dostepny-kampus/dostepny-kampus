/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.review

import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.exchange
import io.github.dostepnykampus.core.network.submission.decodeConflict
import io.github.dostepnykampus.core.network.submission.decodeSubmission
import io.github.dostepnykampus.core.network.submission.decodeSubmissions
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP sprawdzania zgłoszeń przez Weryfikatora: kolejka, wzięcie, decyzja i podobne zgłoszenia.
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. 404 to [ApiResult.NotFound], a inny kod
 * spoza opisanych przy funkcji, np. 401 i 403, to [ApiResult.ServerError] z tym kodem. Anulowanie korutyny
 * przerywa żądanie HTTP.
 *
 * Wymagania: US10, NFR-55
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class ReviewApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /**
     * Kolejka Weryfikatora od najstarszego: zgłoszenia wysłane innych osób i zgłoszenia w weryfikacji wzięte przez
     * niego (`GET /review/queue`).
     *
     * Wymagania: US10.4, US10.5, US10.7
     */
    public suspend fun queue(): ApiResult<List<Submission>> {
        val target =
            base
                .newBuilder()
                .addPathSegment(REVIEW)
                .addPathSegment(QUEUE)
                .build()
        return http.exchange(request(target, body = null)) { code, body ->
            if (code == HTTP_OK) decodedResult(code) { json.decodeSubmissions(body) } else unexpectedResult(code)
        }
    }

    /**
     * Bierze zgłoszenie do sprawdzenia. 409 to [ReviewClaim.Taken].
     *
     * Wymagania: US10.4, BR-11
     *
     * @param submissionId identyfikator zgłoszenia
     */
    public suspend fun claim(submissionId: String): ApiResult<ReviewClaim> {
        val request = request(submission(id = submissionId, action = CLAIM), body = EMPTY.toRequestBody())
        return http.exchange(request) { code, body ->
            when (code) {
                HTTP_OK -> decodedResult(code) { ReviewClaim.Claimed(json.decodeSubmission(body)) }
                HTTP_CONFLICT -> ApiResult.Success(ReviewClaim.Taken)
                else -> unexpectedResult(code)
            }
        }
    }

    /**
     * Zapisuje decyzję o zgłoszeniu wziętym przez tego Weryfikatora. 409 z wersją z serwera to
     * [ReviewOutcome.Changed]; 400 przy złamanej regule decyzji to [ApiResult.ServerError].
     *
     * Wymagania: US10.2, US10.3, US10.8, BR-3, BR-16
     *
     * @param submissionId identyfikator zgłoszenia
     * @param decision przyjęcie albo odrzucenie z powodem
     */
    public suspend fun decide(submissionId: String, decision: ReviewDecision): ApiResult<ReviewOutcome> {
        val payload = json.encodeDecision(decision).toRequestBody(JSON)
        val request = request(submission(id = submissionId, action = DECISION), body = payload)
        return http.exchange(request) { code, body ->
            when (code) {
                HTTP_OK -> decodedResult(code) { ReviewOutcome.Decided(json.decodeSubmission(body)) }
                HTTP_CONFLICT -> decodedResult(code) { ReviewOutcome.Changed(json.decodeConflict(body)) }
                else -> unexpectedResult(code)
            }
        }
    }

    /**
     * Zgłoszenia wysłane, w weryfikacji i przyjęte do 10 m od sprawdzanego, od najbliższego.
     *
     * Wymagania: US10.1, BR-13
     *
     * @param submissionId identyfikator sprawdzanego zgłoszenia
     */
    public suspend fun similar(submissionId: String): ApiResult<List<SimilarSubmission>> =
        http.exchange(request(submission(id = submissionId, action = SIMILAR), body = null)) { code, body ->
            if (code == HTTP_OK) decodedResult(code) { json.decodeSimilar(body) } else unexpectedResult(code)
        }

    private fun request(target: HttpUrl, body: RequestBody?): Request =
        Request
            .Builder()
            .url(target)
            .header(name = ACCEPT, value = ACCEPTED_TYPES)
            .method(if (body == null) GET else POST, body)
            .build()

    private fun submission(id: String, action: String): HttpUrl =
        base
            .newBuilder()
            .addPathSegment(SUBMISSIONS)
            .addPathSegment(id)
            .addPathSegment(action)
            .build()

    private companion object {
        const val REVIEW = "review"
        const val QUEUE = "queue"
        const val SUBMISSIONS = "submissions"
        const val CLAIM = "claim"
        const val DECISION = "review"
        const val SIMILAR = "similar"
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
        const val GET = "GET"
        const val POST = "POST"
        const val EMPTY = ""
        val JSON = "application/json".toMediaType()
    }
}
