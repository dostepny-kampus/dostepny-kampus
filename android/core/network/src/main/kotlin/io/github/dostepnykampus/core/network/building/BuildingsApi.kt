/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.building

import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.path.AccessiblePathList
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.exchange
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

/**
 * Klient HTTP listy budynków, karty, pięter, wyszukiwania sali, podpowiedzi punktu i ścieżek dostosowanych
 * (`GET /buildings`, `/rooms`, `/accessible-paths`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Anulowanie korutyny wywołującej
 * każdą funkcję `suspend` przerywa jej żądanie HTTP, także w trakcie czytania odpowiedzi, i kończy się
 * [kotlinx.coroutines.CancellationException], nie [ApiResult.NetworkError].
 *
 * Wymagania: NFR-55
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu; nieznane pole jest pomijane, serwer może je dodać później
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class BuildingsApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val base = baseUrl.toHttpUrl()

    /** Lista budynków z konturami i atrybucją źródła. */
    public suspend fun listBuildings(): ApiResult<BuildingList> =
        fetch(endpoint(listOf(BUILDINGS))) { json.decodeBuildingList(it) }

    /**
     * Karta budynku [symbol]. Nieznany symbol daje [ApiResult.NotFound].
     *
     * @param symbol symbol budynku
     */
    public suspend fun building(symbol: String): ApiResult<BuildingDetails> =
        fetch(endpoint(listOf(BUILDINGS, symbol))) { json.decodeBuildingDetails(it) }

    /**
     * Piętra budynku [symbol] od najniższego; budynek podstawowy ma pustą listę.
     * Nieznany symbol daje [ApiResult.NotFound].
     *
     * Wymagania: US7.2, BR-G1
     *
     * @param symbol symbol budynku
     */
    public suspend fun floors(symbol: String): ApiResult<FloorList> =
        fetch(endpoint(listOf(BUILDINGS, symbol, FLOORS))) { json.decodeFloorList(it) }

    /**
     * Pomieszczenia piętra [floorId] budynku [symbol] (`listFloorRooms`); nieznany budynek albo piętro daje
     * [ApiResult.NotFound]. Wymagania: US16.1
     */
    public suspend fun rooms(symbol: String, floorId: String): ApiResult<RoomList> =
        fetch(endpoint(listOf(BUILDINGS, symbol, FLOORS, floorId, ROOMS))) { json.decodeRoomList(it) }

    /**
     * Sale pasujące do tekstu [query], np. „A-1 329” albo „329” (`searchRooms`): najpierw dokładne dopasowania
     * numeru; pusta lista, gdy nic nie pasuje. Wymagania: US18.1
     */
    public suspend fun searchRooms(query: String): ApiResult<List<FoundRoom>> =
        fetch(endpoint(segments = listOf(ROOMS), query = mapOf("q" to query))) { json.decodeRoomSearch(it) }

    /**
     * Podpowiedź budynku dla punktu WGS84.
     *
     * @param latitude szerokość geograficzna w stopniach
     * @param longitude długość geograficzna w stopniach
     */
    public suspend fun locate(latitude: Double, longitude: Double): ApiResult<BuildingAtPoint> =
        fetch(
            endpoint(
                segments = listOf(BUILDINGS, LOCATE),
                query = mapOf("lat" to latitude.toString(), "lon" to longitude.toString()),
            ),
        ) {
            json.decodeLocateResult(it)
        }

    /** Odcinki ścieżek dostosowanych z mapy kampusu PWr (`listAccessiblePaths`). Wymagania: US1.4 */
    public suspend fun accessiblePaths(): ApiResult<AccessiblePathList> =
        fetch(endpoint(listOf(ACCESSIBLE_PATHS))) { json.decodeAccessiblePaths(it) }

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
        const val ROOMS = "rooms"
        const val ACCESSIBLE_PATHS = "accessible-paths"
    }
}
