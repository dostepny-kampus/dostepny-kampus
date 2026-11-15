/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.exchange
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP postępu inwentaryzacji Koordynatora: odczyt postępu budynków (`GET /progress`) i cofnięcie znacznika
 * (`DELETE /inspection-markers/{markerId}`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Kod inny niż oczekiwany i 404, także
 * 403 bez roli Koordynatora, to [ApiResult.ServerError] z tym kodem. Anulowanie korutyny przerywa żądanie HTTP.
 *
 * Wymagania: US11.1, US11.3, BR-21
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class InspectionProgressApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /** Postęp wszystkich budynków z aktywnymi znacznikami do cofnięcia. */
    public suspend fun progress(): ApiResult<List<BuildingProgress>> =
        http.exchange(request(base.newBuilder().addPathSegment(PROGRESS).build(), delete = false)) { code, body ->
            if (code == HTTP_OK) decodedResult(code) { json.decodeProgress(body) } else unexpectedResult(code)
        }

    /**
     * Cofa znacznik dowolnego Zbierającego. Znacznik, którego serwer nie zna, daje [ApiResult.NotFound].
     *
     * @param markerId identyfikator UUID cofanego znacznika
     */
    public suspend fun revoke(markerId: String): ApiResult<Unit> {
        val target = base.newBuilder().addPathSegment(MARKERS).addPathSegment(markerId).build()
        return http.exchange(request(target, delete = true)) { code, _ ->
            if (code == HTTP_NO_CONTENT) ApiResult.Success(Unit) else unexpectedResult(code)
        }
    }

    private fun request(target: HttpUrl, delete: Boolean): Request {
        val builder = Request.Builder().url(target).header(name = ACCEPT, value = ACCEPTED_TYPES)
        return if (delete) builder.delete().build() else builder.get().build()
    }

    private companion object {
        const val PROGRESS = "progress"
        const val MARKERS = "inspection-markers"
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
    }
}
