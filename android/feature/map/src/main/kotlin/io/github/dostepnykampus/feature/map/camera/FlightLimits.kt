/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

/**
 * Granice kamery na czas przelotu na inny teren: start przelotu zdejmuje granice, bo punkt startu leży poza
 * prostokątem nowego terenu, a przelot oddala mapę poniżej jego najmniejszego powiększenia. Granice zmienione
 * w trakcie przelotu czekają na jego koniec. Bez przelotu granice idą do mapy od razu przez [apply]
 * (`null` zdejmuje granice).
 *
 * Wymagania: US1.8, BR-G5
 */
internal class FlightLimits(private val apply: (MapLimits?) -> Unit) {
    private var latest: MapLimits? = null
    private var isFlying = false

    /** Nowe granice terenu [next]; w trakcie przelotu czekają na jego koniec. */
    fun limit(next: MapLimits?) {
        latest = next
        if (!isFlying) apply(next)
    }

    /** Start przelotu: granice zdjęte do jego końca. */
    fun startFlight() {
        isFlying = true
        apply(null)
    }

    /** Koniec przelotu, także przerwanego gestem albo innym ruchem: ostatnie granice wracają. */
    fun endFlight() {
        if (isFlying) {
            isFlying = false
            apply(latest)
        }
    }
}
