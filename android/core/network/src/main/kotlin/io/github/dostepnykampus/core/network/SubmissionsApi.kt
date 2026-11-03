/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Klient HTTP zgłoszeń: wysłanie, zdjęcia i zgłoszenia przyjęte (`/submissions`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Odpowiedź z kodem spoza kontraktu,
 * np. 400 przy złamanej regule zgłoszenia, to [ApiResult.ServerError] z tym kodem.
 *
 * @param http klient OkHttp; wysyłka zdjęcia używa jego kopii z dłuższym limitem czasu
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 * @param io kontekst blokującego wywołania; domyślnie dyspozytor wejścia-wyjścia
 */
class SubmissionsApi(
    private val http: OkHttpClient,
    private val json: Json,
    baseUrl: String,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    private val base = baseUrl.toHttpUrl()
    private val photoHttp by lazy { http.newBuilder().callTimeout(PHOTO_TIMEOUT_SECONDS, TimeUnit.SECONDS).build() }

    /**
     * Wysyła nowe zgłoszenie. Ponowne wysłanie tego samego [id] zwraca zgłoszenie zapisane wcześniej (BR-9).
     *
     * @param id identyfikator UUID nadany w telefonie
     * @param content treść zgłoszenia
     */
    suspend fun create(id: String, content: SubmissionContent): ApiResult<Submission> =
        call(http, request(path(), JSON.body(json.encodeNewSubmission(id, content)), POST)) { code, body ->
            if (code == HTTP_OK || code == HTTP_CREATED) success(code) { submission(body) } else other(code)
        }

    /**
     * Wysyła zdjęcie z listy `photoIds` zgłoszenia; ponowne wysłanie je zastępuje (BR-9).
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia nadany w telefonie
     * @param jpeg plik JPEG już obrócony, bo serwer usuwa EXIF z orientacją (NFR-26)
     */
    suspend fun putPhoto(submissionId: String, photoId: String, jpeg: ByteArray): ApiResult<Unit> =
        call(photoHttp, request(path(submissionId, PHOTOS, photoId), JPEG.body(jpeg), PUT)) { code, _ ->
            if (code == HTTP_NO_CONTENT) ApiResult.Success(Unit) else other(code)
        }

    /**
     * Zgłoszenia przyjęte do mapy i karty budynku (US1.3, US2.2, BR-6).
     *
     * @param buildingSymbol tylko zgłoszenia z tego budynku; `null` dla wszystkich
     */
    suspend fun accepted(buildingSymbol: String? = null): ApiResult<List<Submission>> {
        val builder = path().newBuilder()
        buildingSymbol?.let { builder.addQueryParameter(name = "buildingSymbol", value = it) }
        return call(http, request(builder.build(), body = null, method = GET)) { code, body ->
            if (code == HTTP_OK) {
                success(code) { json.parseToJsonElement(body).jsonArray.map { json.decodeSubmission(it.jsonObject) } }
            } else {
                other(code)
            }
        }
    }

    private fun submission(body: String): Submission = json.decodeSubmission(json.parseToJsonElement(body).jsonObject)

    private suspend fun <T> call(
        client: OkHttpClient,
        request: Request,
        read: (Int, String) -> ApiResult<T>,
    ): ApiResult<T> =
        withContext(io) {
            try {
                client.newCall(request).execute().use { read(it.code, it.body?.string().orEmpty()) }
            } catch (error: IOException) {
                ApiResult.NetworkError(error)
            }
        }

    private fun request(target: HttpUrl, body: RequestBody?, method: String): Request {
        val builder = Request.Builder().url(target).header(name = ACCEPT, value = ACCEPTED_TYPES)
        return builder.method(method, body).build()
    }

    private fun path(vararg segments: String): HttpUrl {
        val builder = base.newBuilder().addPathSegment(SUBMISSIONS)
        segments.forEach { builder.addPathSegment(it) }
        return builder.build()
    }

    private companion object {
        const val PHOTO_TIMEOUT_SECONDS = 60L
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
        const val SUBMISSIONS = "submissions"
        const val PHOTOS = "photos"
        const val GET = "GET"
        const val POST = "POST"
        const val PUT = "PUT"
        val JSON = "application/json".toMediaType()
        val JPEG = "image/jpeg".toMediaType()

        fun MediaType.body(text: String): RequestBody = text.toRequestBody(this)

        fun MediaType.body(bytes: ByteArray): RequestBody = bytes.toRequestBody(this)

        fun <T> success(code: Int, decode: () -> T): ApiResult<T> =
            try {
                ApiResult.Success(decode())
            } catch (error: IllegalArgumentException) {
                ApiResult.ServerError(code = code, cause = error)
            }

        fun other(code: Int): ApiResult<Nothing> =
            if (code == HTTP_NOT_FOUND) ApiResult.NotFound else ApiResult.ServerError(code)
    }
}
