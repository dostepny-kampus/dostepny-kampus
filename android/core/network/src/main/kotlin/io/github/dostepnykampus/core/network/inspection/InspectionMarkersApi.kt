/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.exchange
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK

/**
 * Zmiana znacznika piętra obejrzanego zapisana w telefonie, wysyłana jako operacja `POST /sync/push`.
 *
 * Wymagania: US15.3, BR-9
 */
public sealed interface MarkerOperation {
    /** Identyfikator UUID operacji nadany w telefonie; powtórzony nie zapisuje zmiany drugi raz. */
    public val operationId: String

    /**
     * Postawienie znacznika.
     *
     * @property operationId identyfikator operacji
     * @property markerId identyfikator UUID znacznika
     * @property buildingSymbol budynek znacznika
     * @property floorId piętro budynku szczegółowego; `null` to cały budynek podstawowy
     */
    public data class Set(
        override val operationId: String,
        val markerId: String,
        val buildingSymbol: String,
        val floorId: String?,
    ) : MarkerOperation

    /**
     * Cofnięcie znacznika.
     *
     * @property operationId identyfikator operacji
     * @property markerId identyfikator cofanego znacznika
     */
    public data class Revoke(override val operationId: String, val markerId: String) : MarkerOperation
}

/** Wynik jednej operacji ze zbioru `SyncOperationResult`. Wymagania: US15.3, BR-9 */
public enum class MarkerOutcome {
    /** Serwer zapisał zmianę albo zna już tę operację. */
    APPLIED,

    /** Konflikt wersji; dotyczy zgłoszeń, przy znaczniku nie powinien wystąpić. */
    CONFLICT,

    /** Serwer odrzucił zmianę, np. bez roli Zbierającego. */
    REJECTED,
}

/**
 * Klient HTTP zmian znaczników piętra obejrzanego zapisanych w telefonie (`POST /sync/push`).
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek. Anulowanie korutyny przerywa
 * żądanie i kończy się [kotlinx.coroutines.CancellationException].
 * TODO(#1846): podłączyć do wysyłki kolejki znaczników po powrocie sieci.
 *
 * Wymagania: US15.3, BR-9, BR-20
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera, np. `http://10.0.2.2:8080/` z emulatora
 */
public class InspectionMarkersApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val target = requireNotNull(baseUrl.toHttpUrl().resolve(PUSH_PATH))

    /**
     * Wysyła zmiany w kolejności zapisu w telefonie, najwyżej 100 naraz.
     *
     * @param operations zmiany od najstarszej
     * @return wynik każdej operacji według jej [MarkerOperation.operationId]
     */
    public suspend fun push(operations: List<MarkerOperation>): ApiResult<Map<String, MarkerOutcome>> {
        val body = json.encodeMarkerPush(operations).toRequestBody(JSON)
        val builder = Request.Builder().url(target).header(name = ACCEPT, value = ACCEPTED_TYPES)
        val request = builder.post(body).build()
        return http.exchange(request) { code, text ->
            when (code) {
                HTTP_OK -> outcomes(code, text)
                HTTP_NOT_FOUND -> ApiResult.NotFound
                else -> ApiResult.ServerError(code)
            }
        }
    }

    private fun outcomes(code: Int, text: String): ApiResult<Map<String, MarkerOutcome>> =
        try {
            ApiResult.Success(json.decodeMarkerOutcomes(text))
        } catch (error: IllegalArgumentException) {
            ApiResult.ServerError(code = code, cause = error)
        }

    private companion object {
        const val PUSH_PATH = "sync/push"
        const val ACCEPT = "Accept"
        const val ACCEPTED_TYPES = "application/json, application/problem+json"
        val JSON = "application/json".toMediaType()
    }
}
