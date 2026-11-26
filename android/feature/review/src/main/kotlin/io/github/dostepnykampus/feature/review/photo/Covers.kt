/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import kotlin.math.abs

/**
 * Prostokąt zasłony we współrzędnych względnych zdjęcia: 0 to lewa albo górna krawędź, 1 prawa albo dolna.
 *
 * Wymagania: BR-15, NFR-25
 *
 * @property x środek w poziomie
 * @property y środek w pionie
 * @property size bok jako część szerokości i wysokości zdjęcia
 */
data class Cover(val x: Float, val y: Float, val size: Float) {
    /** Wielkości zasłony. */
    companion object {
        /** Bok nowej zasłony. */
        const val START: Float = 0.2f

        /** Zmiana boku przy „Większy” i „Mniejszy”. */
        const val STEP: Float = 0.05f

        /** Najmniejszy bok. */
        const val MIN: Float = 0.1f

        /** Największy bok. */
        const val MAX: Float = 0.6f
    }
}

private const val THIRDS = 3

/** Czy punkt ([px], [py]) leży w zasłonie. */
fun Cover.contains(px: Float, py: Float): Boolean = abs(px - x) <= size / 2 && abs(py - y) <= size / 2

/** Położenie zasłony na zdjęciu w siatce trzy na trzy. Wymagania: NFR-18 */
fun Cover.place(): CoverPlace = CoverPlace.entries[third(y) * THIRDS + third(x)]

private fun third(value: Float): Int = (value * THIRDS).toInt().coerceIn(minimumValue = 0, maximumValue = THIRDS - 1)

/** Położenie zasłony słowami dla czytnika ekranu, wierszami od góry. Wymagania: NFR-18 */
enum class CoverPlace {
    /** U góry po lewej. */
    TOP_LEFT,

    /** U góry pośrodku. */
    TOP,

    /** U góry po prawej. */
    TOP_RIGHT,

    /** Pośrodku po lewej. */
    LEFT,

    /** Pośrodku. */
    CENTER,

    /** Pośrodku po prawej. */
    RIGHT,

    /** Na dole po lewej. */
    BOTTOM_LEFT,

    /** Na dole pośrodku. */
    BOTTOM,

    /** Na dole po prawej. */
    BOTTOM_RIGHT,
}

/**
 * Zasłony jednego zdjęcia i zaznaczona zasłona.
 *
 * Wymagania: BR-15, NFR-53
 *
 * @property items zasłony w kolejności postawienia
 * @property selected indeks zaznaczonej zasłony; `null` bez zaznaczenia
 */
data class Covers(val items: List<Cover> = emptyList(), val selected: Int? = null)

/** Dotknięcie zdjęcia: zaznacza zasłonę w tym miejscu albo stawia nową i ją zaznacza. */
fun Covers.tap(x: Float, y: Float): Covers {
    val hit = items.indexOfLast { it.contains(px = x, py = y) }
    return if (hit >= 0) {
        copy(selected = hit)
    } else {
        val placed = Cover(x = x.coerceIn(range = 0f..1f), y = y.coerceIn(range = 0f..1f), size = Cover.START)
        Covers(items = items + placed, selected = items.size)
    }
}

/** „Większy” dla zaznaczonej zasłony, najwyżej do [Cover.MAX]. */
fun Covers.bigger(): Covers = resized(Cover.STEP)

/** „Mniejszy” dla zaznaczonej zasłony, najmniej do [Cover.MIN]. */
fun Covers.smaller(): Covers = resized(-Cover.STEP)

/** „Usuń zasłonę”: usuwa zaznaczoną zasłonę; bez zaznaczenia nic nie zmienia. */
fun Covers.remove(): Covers {
    val index = selected ?: return this
    return Covers(items = items.filterIndexed { at, _ -> at != index }, selected = null)
}

private fun Covers.resized(step: Float): Covers {
    val index = selected ?: return this
    val changed = items[index].let { it.copy(size = (it.size + step).coerceIn(range = Cover.MIN..Cover.MAX)) }
    return copy(items = items.mapIndexed { at, cover -> if (at == index) changed else cover })
}
