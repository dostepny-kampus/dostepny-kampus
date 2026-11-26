/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

/**
 * Chwila odsłonięcia podkładu nowego terenu. Po wczytaniu stylu albo zmianie terenu podkład jest ukryty, a odsłania
 * go pierwsza klatka narysowana w pełni, czyli z wczytanymi kafelkami w widoku, przy powiększeniu, od którego
 * podkład jest rysowany. Dzięki temu podkład przenika w całości, zamiast wskakiwać kawałkami.
 *
 * Wymagania: US1.8, NFR-56
 */
internal class BasemapReveal {
    private var isHidden = false

    /** Podkład nowego stylu albo terenu ukryty do wczytania kafelków. */
    fun hide() {
        isHidden = true
    }

    /**
     * Klatka mapy przy powiększeniu [zoom]; [isFull], gdy kafelki w widoku są wczytane, a [minZoom] to powiększenie,
     * od którego podkład jest rysowany. `true` raz, w klatce, w której ukryty podkład ma zacząć przenikać.
     */
    fun frame(isFull: Boolean, zoom: Double, minZoom: Double): Boolean {
        val isDue = isHidden && isFull && zoom >= minZoom
        if (isDue) isHidden = false
        return isDue
    }
}
