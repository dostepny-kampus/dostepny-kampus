/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.style.Basemap

/**
 * Teren w stylu mapy: prostokąt i granica terenu [area] (bez zaznaczenia) oraz podkład [basemap].
 *
 * Wymagania: BR-G5, NFR-39
 */
internal data class StyleArea(val area: MapSelection, val basemap: Basemap?) {
    /** Powiększenie, poniżej którego podkład nie jest rysowany; bez podkładu 0, czyli zawsze rysowany. */
    val hiddenBelow: Double get() = basemap?.minZoom ?: 0.0
}

/** Teren z zaznaczenia [selection] (prostokąt i granica, bez zaznaczonych obiektów) z podkładem [basemap]. */
internal fun styleAreaOf(selection: MapSelection, basemap: Basemap?): StyleArea =
    StyleArea(area = MapSelection(bounds = selection.bounds, boundary = selection.boundary), basemap = basemap)

/**
 * Teren pokazany w stylu na czas przelotu na inny teren. Bez przelotu nowy teren idzie do stylu od razu. W przelocie
 * zostaje teren startowy z jego podkładem, aż mapa oddali się poniżej powiększenia, przy którym podkład żadnego
 * z dwóch terenów nie jest rysowany; zamiana jest wtedy niewidoczna, a przy zejściu do celu wraca już podkład nowego
 * terenu. Koniec przelotu, także przerwanego gestem albo innym ruchem, pokazuje najnowszy teren.
 *
 * Wymagania: US1.8, NFR-39
 */
internal class FlightArea(first: StyleArea) {
    /** Teren pokazany w stylu; zmiana rekomponuje widok mapy. */
    var shown: StyleArea by mutableStateOf(first)
        private set
    private var latest = first
    private var isFlying = false

    /** Nowy teren [next]; w trakcie przelotu czeka na oddalenie mapy albo koniec przelotu. */
    fun area(next: StyleArea) {
        latest = next
        if (!isFlying) shown = next
    }

    /** Start przelotu: teren startowy zostaje w stylu. */
    fun startFlight() {
        isFlying = true
    }

    /** Powiększenie mapy [zoom] w trakcie ruchu kamery. */
    fun zoom(zoom: Double) {
        if (isFlying && zoom < minOf(a = shown.hiddenBelow, b = latest.hiddenBelow)) shown = latest
    }

    /** Koniec przelotu: najnowszy teren w stylu. */
    fun endFlight() {
        if (isFlying) {
            isFlying = false
            shown = latest
        }
    }
}
