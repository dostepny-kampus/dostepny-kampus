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
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Wysyła [request] bez blokowania wątku wywołującego i czyta całą odpowiedź, zanim wznowi korutynę.
 *
 * Anulowanie korutyny przerywa żądanie HTTP, także w trakcie czytania ciała, i kończy się
 * [kotlinx.coroutines.CancellationException], nie [ApiResult.NetworkError] (NFR-55). Odpowiedź jest zamykana
 * zawsze, także gdy przyszła już po anulowaniu.
 *
 * @param T typ odczytanych danych
 * @param request żądanie
 * @param read wynik z kodu HTTP i tekstu odpowiedzi
 * @return wynik [read] albo [ApiResult.NetworkError], gdy połączenie nie doszło do skutku lub zostało zerwane
 */
internal suspend fun <T> OkHttpClient.exchange(request: Request, read: (Int, String) -> ApiResult<T>): ApiResult<T> =
    exchangeResponse(request) { read(it.code, it.body.string()) }

/**
 * Odczytuje odpowiedź binarną bez zamiany na tekst; anulowanie i zamknięcie odpowiedzi jak w [exchange].
 *
 * @param T typ odczytanych danych
 * @param request żądanie pliku
 * @param read wynik z kodu HTTP i niezmienionych bajtów odpowiedzi
 */
internal suspend fun <T> OkHttpClient.exchangeBytes(
    request: Request,
    read: (Int, ByteArray) -> ApiResult<T>,
): ApiResult<T> =
    exchangeResponse(request) { read(it.code, it.body.bytes()) }

private suspend fun <T> OkHttpClient.exchangeResponse(
    request: Request,
    read: (Response) -> ApiResult<T>,
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
                            response.use(read)
                        } catch (error: IOException) {
                            ApiResult.NetworkError(error)
                        }
                    continuation.resume(result)
                }
            },
        )
    }
