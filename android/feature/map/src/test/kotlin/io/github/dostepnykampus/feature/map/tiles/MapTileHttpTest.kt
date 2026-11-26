/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.tiles

import io.github.dostepnykampus.feature.map.campus.CampusBounds
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
private const val TILE = "/20251119_001001_pt/14/8968/5477.pbf"

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
    fun sendsAppUserAgent() {
        server.enqueue(MockResponse().setBody("kafelek"))
        val tiles = MapTileHttp(http = app, userAgent = AGENT).calls
        tiles.newCall(Request.Builder().url(server.url("/0/0/0.png")).build()).execute().close()
        assertEquals(AGENT, server.takeRequest().getHeader("User-Agent"))
    }

    /** klient mapy dzieli pulę połączeń z klientem aplikacji, a kafelki nie mają jego limitu 3 s. */
    @Test
    fun sharesAppPool() {
        val tiles = MapTileHttp(http = app, userAgent = AGENT).client
        assertSame(app.connectionPool, tiles.connectionPool)
        assertEquals(OkHttpClient().callTimeoutMillis, tiles.callTimeoutMillis)
        assertEquals(OkHttpClient().connectTimeoutMillis, tiles.connectTimeoutMillis)
    }

    /**
     * Kafelek podkładu w całości poza prostokątami terenów nie idzie do serwera: odpowiedź przychodzi bez sieci.
     * Kafelek nad terenem idzie do serwera.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun skipsTileOutsideAreas() {
        server.enqueue(MockResponse().setBody("kafelek"))
        val http = MapTileHttp(http = app, userAgent = AGENT)
        http.basemapAreas = listOf(CampusBounds(west = 17.2, south = 51.0, east = 17.21, north = 51.01))
        val outside = http.calls.newCall(tileRequest()).execute()
        assertEquals(504, outside.code, "odpowiedź bez sieci")
        outside.close()
        http.basemapAreas = listOf(CampusBounds(west = 17.06, south = 51.10, east = 17.07, north = 51.11))
        val inside = http.calls.newCall(tileRequest()).execute()
        inside.close()
        assertEquals(1, server.requestCount, "tylko kafelek nad terenem do serwera")
    }

    private fun tileRequest() = Request.Builder().url(server.url(TILE)).build()
}
