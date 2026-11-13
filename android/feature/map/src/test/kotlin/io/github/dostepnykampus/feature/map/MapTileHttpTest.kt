/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

private const val AGENT = "DostepnyKampus/1.0 (io.example.test)"

/** Klient kafelków mapy z grafu Hilt na serwerze testowym, nie na serwerze OSM. Wymagania: NFR-39 */
class MapTileHttpTest {
    private val server = MockWebServer()
    private val app = OkHttpClient.Builder().callTimeout(3, TimeUnit.SECONDS).build()

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    /**
     * Żądanie kafelka ma nagłówek User-Agent aplikacji. Klient powstaje z wartości podanej przez graf, bez
     * zmiennej globalnej, więc wynik nie zależy od tego, czy mapa powstała przed aktywnością.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun nfr39_tile_has_app_user_agent() {
        server.enqueue(MockResponse().setBody("kafelek"))
        val tiles = MapTileHttp(http = app, userAgent = AGENT).calls
        tiles.newCall(Request.Builder().url(server.url("/0/0/0.png")).build()).execute().close()
        assertEquals(AGENT, server.takeRequest().getHeader("User-Agent"))
    }

    /** klient mapy dzieli pulę połączeń z klientem aplikacji, a kafelki nie mają jego limitu 3 s. */
    @Test
    fun nfr39_tiles_share_app_pool() {
        val tiles = MapTileHttp(http = app, userAgent = AGENT).client
        assertSame(app.connectionPool, tiles.connectionPool)
        assertEquals(OkHttpClient().callTimeoutMillis, tiles.callTimeoutMillis)
        assertEquals(OkHttpClient().connectTimeoutMillis, tiles.connectTimeoutMillis)
    }
}
