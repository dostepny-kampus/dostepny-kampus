/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.sync

import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.exchange
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP pobrania zmian z serwera (`GET /sync/pull`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Kod inny niż 200, także 400 i 401,
 * to [ApiResult.ServerError] z tym kodem. Anulowanie korutyny przerywa żądanie HTTP.
 *
 * Wymagania: US15.1, US15.2
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class SyncPullApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /**
     * Zmiany od poprzedniego pobrania.
     *
     * @param since `cursor` poprzedniego pobrania; `null` pobiera wszystko
     */
    public suspend fun pull(since: String?): ApiResult<SyncPull> {
        val builder = base.newBuilder().addPathSegment(SYNC).addPathSegment(PULL)
        if (since != null) builder.addQueryParameter(name = SINCE, value = since)
        val target = builder.build()
        val request =
            Request
                .Builder()
                .url(target)
                .header(name = ACCEPT, value = ACCEPTED_TYPES)
                .get()
                .build()
        return http.exchange(request) { code, body ->
            if (code == HTTP_OK) decodedResult(code) { json.decodeSyncPull(body) } else unexpectedResult(code)
        }
    }

    private companion object {
        const val SYNC = "sync"
        const val PULL = "pull"
        const val SINCE = "since"
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
    }
}
