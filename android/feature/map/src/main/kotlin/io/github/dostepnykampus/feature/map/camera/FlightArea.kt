/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.core.model.CampusBounds
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
 * Teren pokazany w stylu przy zmianie terenu. Do końca ruchu kamery, który po niej następuje, podkład jest rysowany
 * od mniejszego z powiększeń podkładu starego i nowego terenu, a w przelocie przy każdym powiększeniu. Kafelki
 * podkładu wolno wtedy pobierać w prostokątach obu terenów. Koniec przelotu, inny ruch kamery i spoczynek mapy, także
 * po przelocie przerwanym gestem, pokazują sam najnowszy teren.
 *
 * Wymagania: US1.8, NFR-39, NFR-56
 *
 * @param first teren na starcie mapy
 * @param onTileAreas prostokąty terenów, w których wolno pobierać kafelki podkładu, po każdej ich zmianie
 */
internal class FlightArea(first: StyleArea, private val onTileAreas: (List<CampusBounds>) -> Unit = {}) {
    /** Teren pokazany w stylu; zmiana rekomponuje widok mapy. */
    var shown: StyleArea by mutableStateOf(first)
        private set

    /**
     * Czy przy ostatniej zmianie [shown] podkład zostaje na ekranie: kamera doszła do widoku bez skoku, a podkład
     * starego i nowego terenu jest rysowany przy bieżącym powiększeniu. `false`, gdy podkład nowego terenu ma czekać
     * ukryty na wczytanie kafelków.
     */
    var isBasemapKept: Boolean = false
        private set
    private var latest = first
    private var origin: StyleArea? = null
    private var isFlying = false
    private var isJump = false
    private var zoomNow = 0.0

    init {
        tiles()
    }

    /** Nowy teren [next]; poza przelotem w stylu od razu, z podkładem starego terenu do końca następnego ruchu. */
    fun area(next: StyleArea) {
        latest = next
        if (isFlying) {
            tiles()
        } else {
            origin = origin ?: shown
            show(StyleArea(area = next.area, basemap = lower(old = origin?.basemap, new = next.basemap)))
        }
    }

    /** Start ruchu kamery [motion]: przelot rysuje podkład przy każdym powiększeniu, inny ruch kończy zmianę. */
    fun start(motion: CameraMotion) {
        isJump = motion == CameraMotion.NONE
        if (motion != CameraMotion.FLIGHT) {
            settle()
        } else if (!isFlying) {
            isFlying = true
            origin = origin ?: shown
            show(shown.copy(basemap = shown.basemap?.let { Basemap(minZoom = 0.0) }))
        }
    }

    /** Powiększenie mapy [zoom] w trakcie ruchu kamery. */
    fun zoom(zoom: Double) {
        zoomNow = zoom
    }

    /** Spoczynek mapy, także po przelocie: sam najnowszy teren w stylu. */
    fun endFlight() {
        settle()
        isJump = false
    }

    private fun settle() {
        isFlying = false
        origin = null
        show(latest)
    }

    private fun show(next: StyleArea) {
        if (next != shown) {
            isBasemapKept = !isJump && zoomNow >= maxOf(a = shown.hiddenBelow, b = next.hiddenBelow)
            shown = next
        }
        tiles()
    }

    private fun tiles() {
        onTileAreas(listOfNotNull(origin, latest).mapNotNull { it.area.bounds }.distinct())
    }

    private fun lower(old: Basemap?, new: Basemap?): Basemap? =
        if (old == null || new == null) new else Basemap(minZoom = minOf(a = old.minZoom, b = new.minZoom))
}
