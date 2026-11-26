/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.cover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Zasłony na zdjęciu: dotknięcie stawia albo zaznacza, rozmiar w granicach, usunięcie i położenie słowami.
 *
 * Wymagania: BR-15, NFR-18, NFR-53
 */
class CoversTest {
    @Test
    fun tapPlacesFixedSizeCover() {
        val placed = Covers().tap(x = 0.3f, y = 0.4f)
        assertEquals(listOf(Cover(x = 0.3f, y = 0.4f, size = Cover.START)), placed.items, "prostokąt stałej wielkości")
        assertEquals(0, placed.selected, "nowa zasłona zaznaczona")
    }

    @Test
    fun tapOnCoverSelectsIt() {
        val two = Covers().tap(x = 0.2f, y = 0.2f).tap(x = 0.8f, y = 0.8f)
        val selected = two.tap(x = 0.25f, y = 0.15f)
        assertEquals(2, selected.items.size, "dotknięcie zasłony nie stawia nowej")
        assertEquals(0, selected.selected, "zaznaczona pierwsza")
    }

    @Test
    fun resizesWithinLimits() {
        val one = Covers().tap(x = 0.5f, y = 0.5f)
        assertEquals(Cover.START + Cover.STEP, one.bigger().items.single().size, 0.0001f, "większy o krok")
        val smallest = generateSequence(one) { it.smaller() }.take(STEPS).last()
        assertEquals(Cover.MIN, smallest.items.single().size, 0.0001f, "nie mniejszy niż najmniejszy")
        val largest = generateSequence(one) { it.bigger() }.take(STEPS).last()
        assertEquals(Cover.MAX, largest.items.single().size, 0.0001f, "nie większy niż największy")
    }

    @Test
    fun removesSelected() {
        val two = Covers().tap(x = 0.2f, y = 0.2f).tap(x = 0.8f, y = 0.8f)
        val left = two.remove()
        assertEquals(listOf(Cover(x = 0.2f, y = 0.2f, size = Cover.START)), left.items, "usunięta zaznaczona")
        assertNull(left.selected, "bez zaznaczenia po usunięciu")
        assertEquals(left, left.bigger(), "bez zaznaczenia przyciski nic nie zmieniają")
    }

    @Test
    fun namesPlaceInWords() {
        assertEquals(CoverPlace.TOP_LEFT, Cover(x = 0.1f, y = 0.1f, size = Cover.START).place(), "u góry po lewej")
        assertEquals(CoverPlace.CENTER, Cover(x = 0.5f, y = 0.5f, size = Cover.START).place(), "pośrodku")
        val corner = Cover(x = 0.9f, y = 0.8f, size = Cover.START)
        assertEquals(CoverPlace.BOTTOM_RIGHT, corner.place(), "na dole po prawej")
    }

    private companion object {
        const val STEPS = 20
    }
}
