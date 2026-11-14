/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.OkHttpClient
import java.time.Duration
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

private val TILE_CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)

/** Nagłówek User-Agent kafelków mapy OSM, dostarczany przez aplikację do grafu Hilt. Wymagania: NFR-39 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MapUserAgent

/**
 * Klient HTTP kafelków mapy z grafu Hilt, więc nagłówek nie zależy od kolejności startu. Powstaje z klienta
 * aplikacji [http] przez `newBuilder()` i dzieli z nim pulę połączeń. Każde żądanie ma nagłówek [userAgent].
 * Kafelki nie mają limitu całego wywołania klienta aplikacji, tylko domyślne 10 s na połączenie, jak
 * wcześniej. Token konta idzie tylko do serwera aplikacji, nie do serwera kafelków.
 *
 * Wymagania: NFR-39
 *
 * @param http klient aplikacji
 * @param userAgent nagłówek User-Agent aplikacji
 */
@Singleton
class MapTileHttp
    @Inject
    constructor(
        http: OkHttpClient,
        @MapUserAgent userAgent: String,
    ) {
        /** Klient kafelków z pulą połączeń klienta aplikacji i domyślnymi limitami czasu OkHttp. */
        internal val client: OkHttpClient =
            http
                .newBuilder()
                .connectTimeout(TILE_CONNECT_TIMEOUT)
                .callTimeout(Duration.ZERO)
                .build()

        /**
         * Wywołania dla MapLibre z nagłówkiem User-Agent; bez regionów offline, kafelki na bieżący widok.
         *
         * Wymagania: NFR-39
         */
        val calls: Call.Factory =
            Call.Factory { request ->
                client.newCall(request.newBuilder().header(name = "User-Agent", value = userAgent).build())
            }
    }

/** Dostęp widoku mapy do [MapTileHttp]: widok jest obiektem, więc nie ma wstrzykiwania przez konstruktor. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapTileHttpEntryPoint {
    /** Klient kafelków mapy z grafu. */
    fun mapTileHttp(): MapTileHttp
}
