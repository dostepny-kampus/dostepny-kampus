/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Wysyła [request] bez blokowania wątku wywołującego i czyta całą odpowiedź, zanim wznowi korutynę.
 *
 * Anulowanie korutyny przerywa żądanie HTTP, także w trakcie czytania ciała, i kończy się
 * [kotlinx.coroutines.CancellationException], nie [ApiResult.NetworkError]. Odpowiedź jest zamykana
 * zawsze, także gdy przyszła już po anulowaniu. Wynik zostawia jedną linię logu ([logExchange]).
 *
 * Wymagania: NFR-55
 *
 * @param T typ odczytanych danych
 * @param request żądanie
 * @param read wynik z kodu HTTP i tekstu odpowiedzi
 * @return wynik [read] albo [ApiResult.NetworkError], gdy połączenie nie doszło do skutku lub zostało zerwane
 */
internal suspend fun <T> OkHttpClient.exchange(request: Request, read: (Int, String) -> ApiResult<T>): ApiResult<T> =
    exchangeBody(request) { code, body -> read(code, body.string()) }

/**
 * To samo co [exchange], ale [read] dostaje ciało odpowiedzi, np. do odczytu bajtów zdjęcia.
 *
 * @param T typ odczytanych danych
 * @param request żądanie
 * @param read wynik z kodu HTTP i ciała odpowiedzi
 * @return wynik [read] albo [ApiResult.NetworkError], gdy połączenie nie doszło do skutku lub zostało zerwane
 */
internal suspend fun <T> OkHttpClient.exchangeBody(
    request: Request,
    read: (Int, ResponseBody) -> ApiResult<T>,
): ApiResult<T> {
    val started = System.nanoTime()
    return awaitExchange(request, read).also { result ->
        logExchange(request, result, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
    }
}

private suspend fun <T> OkHttpClient.awaitExchange(
    request: Request,
    read: (Int, ResponseBody) -> ApiResult<T>,
): ApiResult<T> =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(ApiResult.NetworkError(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result =
                        try {
                            response.use { read(it.code, it.body) }
                        } catch (error: IOException) {
                            ApiResult.NetworkError(error)
                        }
                    continuation.resume(result)
                }
            },
        )
    }

/** Wynik odczytu API. Wywołujący nie dostaje wyjątku sieci ani parsowania. */
public sealed interface ApiResult<out T> {
    /**
     * Odpowiedź 200 zgodna z kontraktem.
     *
     * @param T typ odczytanych danych
     * @property value odczytane dane
     */
    public data class Success<T>(val value: T) : ApiResult<T>

    /** Odpowiedź 404 w postaci `application/problem+json`. */
    public data object NotFound : ApiResult<Nothing>

    /**
     * Połączenie nie doszło do skutku albo zostało przerwane.
     *
     * @property cause błąd wejścia-wyjścia
     */
    public data class NetworkError(val cause: IOException) : ApiResult<Nothing>

    /**
     * Kod inny niż 200 i 404 albo treść, której nie da się odczytać jako kontrakt.
     *
     * @property code kod HTTP; przy złej treści udanej odpowiedzi to 200
     * @property cause błąd odczytu treści; `null`, gdy serwer zwrócił kod błędu
     */
    public data class ServerError(val code: Int, val cause: IllegalArgumentException? = null) : ApiResult<Nothing>
}
