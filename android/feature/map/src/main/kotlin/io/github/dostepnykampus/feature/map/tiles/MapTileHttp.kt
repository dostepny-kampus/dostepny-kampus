/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.tiles

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
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
 * wcześniej. Token konta idzie tylko do serwera aplikacji, nie do serwera kafelków. Kafelek wektorowy w całości
 * poza prostokątami wszystkich widocznych widoków jest odczytywany tylko z pamięci podręcznej. Gdy nie ma
 * kopii, OkHttp odpowiada bez sieci (504).
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
        private val activeAreas = AtomicReference<Map<ViewAreas, List<CampusBounds>>>(emptyMap())

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
                val sent = request.newBuilder().header(name = "User-Agent", value = userAgent)
                val isBlocked = isOutside(url = request.url.toString(), areas = activeAreas.get().values.flatten())
                if (isBlocked) sent.cacheControl(CacheControl.FORCE_CACHE)
                client.newCall(sent.build())
            }

        /** Tworzy rejestrację prostokątów jednego widoku mapy, początkowo ukrytego. */
        internal fun newView(): ViewAreas = ViewAreas()

        /**
         * Granice jednego widoku mapy, aktualizowane na wątku interfejsu. Klient HTTP widzi tylko granice
         * widocznych widoków i odczytuje jeden stan wszystkich rejestracji dla każdego żądania.
         */
        internal inner class ViewAreas {
            private var bounds: List<CampusBounds> = emptyList()
            private var isShown = false

            /** Zapisuje bieżące granice widoku, także gdy widok jest ukryty. */
            fun update(areas: List<CampusBounds>) {
                if (bounds == areas) return
                bounds = areas.toList()
                if (isShown) publish()
            }

            /** Pokazuje bieżące granice klientowi HTTP albo wycofuje udział tego widoku. */
            fun setVisible(isVisible: Boolean) {
                if (isShown == isVisible) return
                isShown = isVisible
                publish()
            }

            private fun publish() {
                val shown = bounds.takeIf { isShown && it.isNotEmpty() }
                activeAreas.updateAndGet { active ->
                    if (shown == null) active - this else active + (this to shown)
                }
            }
        }
    }

/** Dostęp widoku mapy do [MapTileHttp]: widok jest obiektem, więc nie ma wstrzykiwania przez konstruktor. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MapTileHttpEntryPoint {
    /** Klient kafelków mapy z grafu. */
    fun mapTileHttp(): MapTileHttp
}
