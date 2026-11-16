/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

/**
 * Granice kamery na czas ruchu mapy do budynku albo terenu (przesunięcia i przelotu). Start ruchu zdejmuje
 * granice: każda zmiana granic w MapLibre zatrzymuje trwający ruch, a granice środka zmieniają się
 * z powiększeniem w każdej klatce. Przelot na inny teren zaczyna się też poza prostokątem nowego terenu. Cel
 * ruchu leży już w granicach, a granice zmienione w trakcie ruchu czekają na jego koniec. Bez ruchu granice idą
 * do mapy od razu przez [apply] (`null` zdejmuje granice).
 *
 * Wymagania: US1.8, US2.8, BR-G5
 */
internal class MoveLimits(private val apply: (MapLimits?) -> Unit) {
    private var latest: MapLimits? = null
    private var isMoving = false

    /** Nowe granice terenu [next]; w trakcie ruchu czekają na jego koniec. */
    fun limit(next: MapLimits?) {
        latest = next
        if (!isMoving) apply(next)
    }

    /** Start ruchu: granice zdjęte do jego końca. */
    fun startMove() {
        isMoving = true
        apply(null)
    }

    /** Koniec ruchu, także przerwanego gestem albo innym ruchem: ostatnie granice wracają. */
    fun endMove() {
        if (isMoving) {
            isMoving = false
            apply(latest)
        }
    }
}
