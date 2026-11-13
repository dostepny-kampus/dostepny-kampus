/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit

/**
 * Klient HTTP zgłoszeń: wysłanie, edycja z kontrolą wersji, zdjęcia i zgłoszenia przyjęte (`/submissions`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Odpowiedź z kodem spoza kontraktu,
 * np. 400 przy złamanej regule zgłoszenia, to [ApiResult.ServerError] z tym kodem. Anulowanie korutyny
 * wywołującej każdą funkcję `suspend`, także wysyłkę zdjęcia, przerywa jej żądanie HTTP, również w trakcie
 * czytania odpowiedzi, i kończy się [kotlinx.coroutines.CancellationException], nie [ApiResult.NetworkError]
 * (NFR-55).
 *
 * @param http klient OkHttp; wysyłka zdjęcia używa jego kopii z dłuższym limitem czasu
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
class SubmissionsApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()
    private val photoHttp by lazy { http.newBuilder().callTimeout(PHOTO_TIMEOUT_SECONDS, TimeUnit.SECONDS).build() }

    /**
     * Wysyła nowe zgłoszenie. Ponowne wysłanie tego samego [id] zwraca zgłoszenie zapisane wcześniej (BR-9).
     *
     * @param id identyfikator UUID nadany w telefonie
     * @param content treść zgłoszenia
     */
    suspend fun create(id: String, content: SubmissionContent): ApiResult<Submission> =
        http.exchange(request(path(), JSON.body(json.encodeNewSubmission(id, content)), POST)) { code, body ->
            if (code == HTTP_OK || code == HTTP_CREATED) success(code) { json.decodeSubmission(body) } else other(code)
        }

    /**
     * Wysyła edycję zaczętą od wersji [version]. Inna wersja na serwerze daje [SubmissionEdit.Conflict]
     * z wersją z serwera; nic nie zapisuje się samo (NFR-9, BR-10).
     *
     * @param id identyfikator zgłoszenia
     * @param version wersja, od której zaczęto edycję w telefonie
     * @param content treść po edycji
     */
    suspend fun update(id: String, version: Int, content: SubmissionContent): ApiResult<SubmissionEdit> =
        http.exchange(request(path(id), JSON.body(json.encodeSubmissionUpdate(version, content)), PUT)) { code, body ->
            when (code) {
                HTTP_OK -> success(code) { SubmissionEdit.Saved(json.decodeSubmission(body)) }
                HTTP_CONFLICT -> success(code) { SubmissionEdit.Conflict(json.decodeConflict(body)) }
                else -> other(code)
            }
        }

    /**
     * Wysyła zdjęcie z listy `photoIds` zgłoszenia; ponowne wysłanie je zastępuje (BR-9).
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia nadany w telefonie
     * @param jpeg plik JPEG już obrócony, bo serwer usuwa EXIF z orientacją (NFR-26)
     */
    suspend fun putPhoto(submissionId: String, photoId: String, jpeg: ByteArray): ApiResult<Unit> =
        photoHttp.exchange(request(path(submissionId, PHOTOS, photoId), JPEG.body(jpeg), PUT)) { code, _ ->
            if (code == HTTP_NO_CONTENT) ApiResult.Success(Unit) else other(code)
        }

    /**
     * Zgłoszenia przyjęte do mapy i karty budynku (`status=ACCEPTED`, US1.3, US2.2, BR-6).
     *
     * @param buildingSymbol tylko zgłoszenia z tego budynku; `null` dla wszystkich
     */
    suspend fun accepted(buildingSymbol: String? = null): ApiResult<List<Submission>> {
        val builder = path().newBuilder().addQueryParameter(name = "status", value = "ACCEPTED")
        buildingSymbol?.let { builder.addQueryParameter(name = "buildingSymbol", value = it) }
        return http.exchange(request(builder.build(), body = null, method = GET)) { code, body ->
            if (code == HTTP_OK) {
                success(code) { json.decodeSubmissions(body) }
            } else {
                other(code)
            }
        }
    }

    /**
     * Własne zgłoszenia Zbierającego w każdym statusie, odrzucone z powodem (`mine=true`, US8.1, US8.2).
     * Bez tokenu z rolą Zbierającego serwer odpowiada 401 albo 403.
     */
    suspend fun mine(): ApiResult<List<Submission>> {
        val target = path().newBuilder().addQueryParameter(name = "mine", value = "true").build()
        return http.exchange(request(target, body = null, method = GET)) { code, body ->
            if (code == HTTP_OK) success(code) { json.decodeSubmissions(body) } else other(code)
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

/** Wynik edycji zgłoszenia, które serwer przyjął do sprawdzenia wersji (NFR-9). */
sealed interface SubmissionEdit {
    /**
     * Serwer zapisał edycję jako kolejną wersję.
     *
     * @property submission zgłoszenie po edycji
     */
    data class Saved(val submission: Submission) : SubmissionEdit

    /**
     * Serwer ma inną wersję albo zgłoszenie nie pozwala na zmianę; edycja czeka w telefonie na wybór (US9.3).
     *
     * @property server zgłoszenie w wersji z serwera
     */
    data class Conflict(val server: Submission) : SubmissionEdit
}
