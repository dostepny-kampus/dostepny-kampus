/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.FloorList
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP listy budynków, karty, pięter i podpowiedzi punktu (`GET /buildings`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Anulowanie korutyny wywołującej
 * każdą funkcję `suspend` przerywa jej żądanie HTTP, także w trakcie czytania odpowiedzi, i kończy się
 * [kotlinx.coroutines.CancellationException], nie [ApiResult.NetworkError] (NFR-55).
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu; nieznane pole jest pomijane, serwer może je dodać później
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
class BuildingsApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /** Lista budynków z konturami i atrybucją źródła. */
    suspend fun listBuildings(): ApiResult<BuildingList> =
        fetch(endpoint(listOf(BUILDINGS))) { json.decodeFromString<BuildingListDto>(it).toModel() }

    /**
     * Karta budynku [symbol]. Nieznany symbol daje [ApiResult.NotFound].
     *
     * @param symbol symbol budynku
     */
    suspend fun building(symbol: String): ApiResult<BuildingDetails> =
        fetch(endpoint(listOf(BUILDINGS, symbol))) { json.decodeFromString<BuildingDetailsDto>(it).toModel() }

    /**
     * Piętra budynku [symbol] od najniższego; budynek podstawowy ma pustą listę (US7.2, BR-G1).
     * Nieznany symbol daje [ApiResult.NotFound].
     *
     * @param symbol symbol budynku
     */
    suspend fun floors(symbol: String): ApiResult<FloorList> =
        fetch(endpoint(listOf(BUILDINGS, symbol, FLOORS))) { json.decodeFromString<FloorListDto>(it).toModel() }

    /**
     * Podpowiedź budynku dla punktu WGS84.
     *
     * @param latitude szerokość geograficzna w stopniach
     * @param longitude długość geograficzna w stopniach
     */
    suspend fun locate(latitude: Double, longitude: Double): ApiResult<BuildingAtPoint> =
        fetch(
            endpoint(
                segments = listOf(BUILDINGS, LOCATE),
                query = mapOf("lat" to latitude.toString(), "lon" to longitude.toString()),
            ),
        ) {
            json.decodeFromString<LocateResultDto>(it).toModel()
        }

    private suspend fun <T> fetch(target: HttpUrl, decode: (String) -> T): ApiResult<T> {
        val builder = Request.Builder().url(target)
        val request = builder.header(name = ACCEPT, value = JSON).build()
        return http.exchange(request) { code, body ->
            when (code) {
                HTTP_NOT_FOUND -> ApiResult.NotFound
                HTTP_OK -> decodeBody(code, body, decode)
                else -> ApiResult.ServerError(code)
            }
        }
    }

    private fun <T> decodeBody(code: Int, body: String, decode: (String) -> T): ApiResult<T> =
        try {
            ApiResult.Success(decode(body))
        } catch (error: IllegalArgumentException) {
            ApiResult.ServerError(code = code, cause = error)
        }

    private fun endpoint(segments: List<String>, query: Map<String, String> = emptyMap()): HttpUrl {
        val builder = base.newBuilder()
        segments.forEach { builder.addPathSegment(it) }
        query.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build()
    }

    private companion object {
        const val ACCEPT = "Accept"
        const val JSON = "application/json"
        const val BUILDINGS = "buildings"
        const val LOCATE = "locate"
        const val FLOORS = "floors"
    }
}

/** Wynik odczytu API. Wywołujący nie dostaje wyjątku sieci ani parsowania. */
sealed interface ApiResult<out T> {
    /**
     * Odpowiedź 200 zgodna z kontraktem.
     *
     * @param T typ odczytanych danych
     * @property value odczytane dane
     */
    data class Success<T>(val value: T) : ApiResult<T>

    /** Odpowiedź 404 w postaci `application/problem+json`. */
    data object NotFound : ApiResult<Nothing>

    /**
     * Połączenie nie doszło do skutku albo zostało przerwane.
     *
     * @property cause błąd wejścia-wyjścia
     */
    data class NetworkError(val cause: IOException) : ApiResult<Nothing>

    /**
     * Kod inny niż 200 i 404 albo treść, której nie da się odczytać jako kontrakt.
     *
     * @property code kod HTTP; przy złej treści udanej odpowiedzi to 200
     * @property cause błąd odczytu treści; `null`, gdy serwer zwrócił kod błędu
     */
    data class ServerError(val code: Int, val cause: IllegalArgumentException? = null) : ApiResult<Nothing>
}
