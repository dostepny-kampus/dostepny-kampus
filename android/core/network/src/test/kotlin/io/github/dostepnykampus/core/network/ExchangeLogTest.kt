/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Linia logu wymiany ma metodę, ścieżkę bez identyfikatorów, wynik i czas, bez zapytania.
 *
 * Wymagania: NFR-57
 */
class ExchangeLogTest {
    private fun get(url: String): Request = Request.Builder().url(url).build()

    @Test
    fun hidesIdentifiersAndQueryInPath() {
        val request = get("http://serwer/submissions/6f1c2a9e-1b3d-4c5e-8f70-123456789abc?mine=true")

        val line = exchangeLine(request, ApiResult.Success(Unit), millis = 42)

        assertEquals(
            "GET /submissions/{id} -> ok, 42 ms",
            line,
            "Linia ma {id} zamiast identyfikatora i nie ma zapytania",
        )
    }

    @Test
    fun namesNetworkErrorClass() {
        val failure: IOException = SocketTimeoutException("tajny adres 10.0.0.7")

        val line = exchangeLine(get("http://serwer/buildings"), ApiResult.NetworkError(failure), millis = 3000)

        assertEquals(
            "GET /buildings -> błąd sieci SocketTimeoutException, 3000 ms",
            line,
            "Linia podaje klasę błędu sieci bez jego komunikatu",
        )
    }

    @Test
    fun namesServerErrorCode() {
        val line = exchangeLine(get("http://serwer/buildings/C-13"), ApiResult.ServerError(code = 503), millis = 7)

        assertEquals("GET /buildings/{id} -> błąd serwera 503, 7 ms", line, "Linia podaje kod błędu serwera")
    }

    @Test
    fun namesNotFound() {
        val line = exchangeLine(get("http://serwer/buildings/floors"), ApiResult.NotFound, millis = 1)

        assertEquals("GET /buildings/floors -> 404, 1 ms", line, "Linia podaje 404 dla braku zasobu")
    }
}
