/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PHOTO
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** Odczyt zdjęć z kontraktu: bajty JPEG, właściwy zasób i błędy jako wyniki (US2.2, #1138). */
class SubmissionPhotoReadTest {
    private val server = MockWebServer()
    private val http = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
    private val jpeg = byteArrayOf(-1, -40, 0, -128, 65, -1, -39)

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    /** Nawet bajty spoza UTF-8 wracają bez zmiany, a żądanie wskazuje zdjęcie z listy zgłoszenia. */
    @Test
    fun us2_2_reads_jpeg_bytes() {
        Buffer().write(jpeg).use { body ->
            server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody(body))
        }
        val result = read()
        assertArrayEquals(jpeg, (result as ApiResult.Success).value)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/submissions/$ID/photos/$PHOTO", request.path)
        assertEquals("image/jpeg", request.getHeader("Accept"))
    }

    /** 404 nie zwraca treści błędu jako zdjęcia. */
    @Test
    fun us2_2_photo_not_found() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("nie ma zdjęcia"))
        assertEquals(ApiResult.NotFound, read())
    }

    /** Odmowa dostępu i awaria magazynu zachowują kod HTTP. */
    @ParameterizedTest
    @ValueSource(ints = [401, 403, 500])
    fun us2_2_photo_server_error(code: Int) {
        server.enqueue(MockResponse().setResponseCode(code).setBody("błąd"))
        assertEquals(ApiResult.ServerError(code), read())
    }

    /** Zerwane połączenie zostawia decyzję o ponowieniu wywołującemu. */
    @Test
    fun us2_2_photo_network_error() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertInstanceOf(ApiResult.NetworkError::class.java, read())
    }

    private fun read(): ApiResult<ByteArray> =
        runBlocking {
            SubmissionsApi(http, NetworkModule.json(), server.url("/").toString()).getPhoto(ID, PHOTO)
        }
}
