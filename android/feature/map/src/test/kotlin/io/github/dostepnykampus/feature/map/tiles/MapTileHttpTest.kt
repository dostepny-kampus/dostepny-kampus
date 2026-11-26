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
private const val OTHER_TILE = "/20251119_001001_pt/14/8975/5484.pbf"
private const val OUTSIDE_TILE = "/20251119_001001_pt/14/8969/5477.pbf"

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
     * Żądania z dwóch widocznych map korzystają z obu terenów. Ukrycie mapy usuwa tylko jej teren.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun keepsTilesOfVisibleMaps() {
        repeat(7) { server.enqueue(MockResponse().setBody("kafelek")) }
        val http = MapTileHttp(http = app, userAgent = AGENT)
        val first = http.newView()
        val second = http.newView()
        first.update(listOf(CampusBounds(west = 17.06, south = 51.10, east = 17.07, north = 51.11)))
        second.update(listOf(CampusBounds(west = 17.2, south = 51.0, east = 17.21, north = 51.01)))
        first.setVisible(true)
        second.setVisible(true)

        assertEquals(200, code(http, TILE), "teren pierwszej mapy")
        assertEquals(200, code(http, OTHER_TILE), "teren drugiej mapy")
        assertEquals(504, code(http, OUTSIDE_TILE), "kafelek poza obiema mapami")

        second.setVisible(false)
        assertEquals(504, code(http, OTHER_TILE), "teren drugiej mapy po zamknięciu")
        assertEquals(200, code(http, TILE), "pierwsza mapa nadal widoczna")
        first.setVisible(false)
        assertEquals(504, code(http, TILE), "bez widocznych map")
        second.setVisible(true)
        assertEquals(200, code(http, OTHER_TILE), "teren po ponownym pokazaniu mapy")
        assertEquals(4, server.requestCount, "tylko kafelki widocznych terenów trafiły do serwera")
    }

    private fun code(http: MapTileHttp, path: String): Int {
        val request = Request.Builder().url(server.url(path)).build()
        return http.calls.newCall(request).execute().use { it.code }
    }
}
