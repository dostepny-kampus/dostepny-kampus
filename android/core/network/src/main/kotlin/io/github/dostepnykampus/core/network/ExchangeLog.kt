/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import okhttp3.Request
import timber.log.Timber

/**
 * Zapisuje jedną linię logu wymiany z serwerem: sukces na DEBUG, brak zasobu na INFO, błąd sieci i błąd serwera
 * na WARN. Linia nie niesie zapytania, nagłówków, treści ani wartości identyfikatorów ze ścieżki.
 *
 * Wymagania: NFR-57
 *
 * @param request wysłane żądanie
 * @param result wynik wymiany
 * @param millis czas wymiany w milisekundach
 */
internal fun logExchange(request: Request, result: ApiResult<*>, millis: Long) {
    val line = exchangeLine(request, result, millis)
    when (result) {
        is ApiResult.Success -> Timber.d(line)
        ApiResult.NotFound -> Timber.i(line)
        is ApiResult.NetworkError, is ApiResult.ServerError -> Timber.w(line)
    }
}

/**
 * Treść linii logu wymiany: metoda, ścieżka z `{id}` w miejscu segmentów z cyfrą, wynik i czas.
 *
 * Wymagania: NFR-57
 *
 * @param request wysłane żądanie
 * @param result wynik wymiany
 * @param millis czas wymiany w milisekundach
 */
internal fun exchangeLine(request: Request, result: ApiResult<*>, millis: Long): String {
    val path =
        request.url.pathSegments
            .filter { segment -> segment.isNotEmpty() }
            .joinToString(separator = "/", prefix = "/") { segment ->
                if (segment.any(Char::isDigit)) "{id}" else segment
            }
    val outcome =
        when (result) {
            is ApiResult.Success -> "ok"
            ApiResult.NotFound -> "404"
            is ApiResult.NetworkError -> "błąd sieci ${result.cause.javaClass.simpleName}"
            is ApiResult.ServerError -> "błąd serwera ${result.code}"
        }
    return "${request.method} $path -> $outcome, $millis ms"
}
