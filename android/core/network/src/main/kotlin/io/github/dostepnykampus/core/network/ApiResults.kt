/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Wynik odczytu treści odpowiedzi udanej: [decode] albo [ApiResult.ServerError] z kodem [code] i przyczyną,
 * gdy treść nie pasuje do kontraktu.
 *
 * @param T typ odczytanych danych
 * @param code kod HTTP odpowiedzi
 * @param decode odczyt treści; zła treść to [IllegalArgumentException]
 */
internal fun <T> decodedResult(code: Int, decode: () -> T): ApiResult<T> =
    try {
        ApiResult.Success(decode())
    } catch (error: IllegalArgumentException) {
        ApiResult.ServerError(code = code, cause = error)
    }

/** Wynik odpowiedzi z kodem [code], którego klient nie oczekuje: 404 to [ApiResult.NotFound], reszta błąd serwera. */
internal fun unexpectedResult(code: Int): ApiResult<Nothing> =
    if (code == HTTP_NOT_FOUND) ApiResult.NotFound else ApiResult.ServerError(code)
