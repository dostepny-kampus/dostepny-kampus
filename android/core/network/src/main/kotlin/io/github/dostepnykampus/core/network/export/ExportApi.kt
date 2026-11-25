/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.export

import io.github.dostepnykampus.core.model.export.ExportFormat
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.exchange
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection.HTTP_OK

/**
 * Plik eksportu z serwera.
 *
 * @property text treść pliku w UTF-8
 * @property isEmpty czy plik nie ma żadnej bariery
 */
public data class ExportFile(val text: String, val isEmpty: Boolean)

/**
 * Klient HTTP eksportu Koordynatora: `GET /export.geojson` i `GET /export.csv`.
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek; 403 bez roli Koordynatora to
 * [ApiResult.ServerError] z tym kodem. Anulowanie korutyny przerywa żądanie HTTP.
 *
 * TODO(#3062): serwer nie ma jeszcze `GET /export.geojson`.
 *
 * Wymagania: US13.1, US13.2, US13.3
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera
 */
public class ExportApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /** Plik eksportu w formacie [format]. */
    public suspend fun download(format: ExportFormat): ApiResult<ExportFile> {
        val request =
            Request
                .Builder()
                .url(base.newBuilder().addPathSegment("export.${format.extension}").build())
                .header(name = "Accept", value = "${format.mediaType}, application/problem+json")
                .build()
        return http.exchange(request) { code, body ->
            if (code == HTTP_OK) decodedResult(code) { fileOf(format, body) } else unexpectedResult(code)
        }
    }

    private fun fileOf(format: ExportFormat, body: String): ExportFile =
        ExportFile(text = body, isEmpty = isEmpty(format, body))

    private fun isEmpty(format: ExportFormat, body: String): Boolean =
        when (format) {
            ExportFormat.GEOJSON -> features(body).isEmpty()
            ExportFormat.CSV -> body.lineSequence().drop(1).all { line -> line.isBlank() }
        }

    private fun features(body: String): JsonArray =
        requireNotNull(json.decodeContract(JsonObject.serializer(), body)["features"]).jsonArray
}
