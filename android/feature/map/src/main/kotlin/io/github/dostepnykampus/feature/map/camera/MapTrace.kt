/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import android.os.Build
import android.os.Trace
import kotlin.math.roundToLong

/**
 * Ślad mapy i karty budynku dla Perfetto: sekcja na czas każdego ruchu kamery i wczytania stylu, liczniki widoku
 * w klatkach i nazwane sekcje pracy w wątku. Bez włączonego śledzenia (Android 10+) nic nie robi. Nazwy są stałe,
 * bez danych osobowych.
 *
 * Wymagania: NFR-57
 */
internal object MapTrace {
    private const val ZOOM_COUNTER = "Kamera: powiększenie ×100"
    private const val LATITUDE_COUNTER = "Kamera: szerokość ×1e6"
    private const val LONGITUDE_COUNTER = "Kamera: długość ×1e6"
    private const val STYLE_SECTION = "Mapa: wczytanie stylu"
    const val AREA_SECTION = "Mapa: zmiana terenu"
    const val SELECTION_SECTION = "Mapa: zaznaczenie w stylu"
    const val CARD_STATE_SECTION = "Karta: stan"
    const val CARD_FLOORS_SECTION = "Karta: fakty o piętrach"
    private const val ZOOM_SCALE = 100.0
    private const val DEGREE_SCALE = 1_000_000.0

    /** Początek ruchu kamery [motion] o numerze [move]; koniec daje [endMove] z tym samym numerem. */
    fun startMove(motion: CameraMotion, move: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.beginAsyncSection(moveSection(motion), move)
        }
    }

    /** Koniec albo przerwanie ruchu kamery [motion] o numerze [move]. */
    fun endMove(motion: CameraMotion, move: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.endAsyncSection(moveSection(motion), move)
        }
    }

    /** Widok w klatce: powiększenie ×100 i środek w milionowych częściach stopnia. */
    fun frame(zoom: Double, latitude: Double, longitude: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.setCounter(ZOOM_COUNTER, (zoom * ZOOM_SCALE).roundToLong())
            Trace.setCounter(LATITUDE_COUNTER, (latitude * DEGREE_SCALE).roundToLong())
            Trace.setCounter(LONGITUDE_COUNTER, (longitude * DEGREE_SCALE).roundToLong())
        }
    }

    /** Początek wczytania stylu o numerze [load]; koniec daje [endStyle]. */
    fun startStyle(load: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.beginAsyncSection(STYLE_SECTION, load)
        }
    }

    /** Styl o numerze [load] wczytany. */
    fun endStyle(load: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()) {
            Trace.endAsyncSection(STYLE_SECTION, load)
        }
    }

    /** Synchroniczna sekcja zmiany terenu wokół [block]. */
    inline fun <T> areaChange(block: () -> T): T = section(name = AREA_SECTION, block = block)

    /**
     * Synchroniczna sekcja [name] wokół [block], zamknięta także po wyjątku; [block] nie może zmienić wątku. Bez
     * włączonego śledzenia tylko wykonuje [block].
     */
    inline fun <T> section(name: String, block: () -> T): T {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !Trace.isEnabled()) return block()
        Trace.beginSection(name)
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }

    private fun moveSection(motion: CameraMotion): String =
        when (motion) {
            CameraMotion.NONE -> "Kamera: bez ruchu"
            CameraMotion.EASE -> "Kamera: przesunięcie"
            CameraMotion.FLIGHT -> "Kamera: przelot"
        }
}
