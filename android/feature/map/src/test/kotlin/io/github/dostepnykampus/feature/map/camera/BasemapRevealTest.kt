/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Odsłonięcie podkładu po wczytaniu kafelków: klatki z kafelkami w drodze i klatki poniżej powiększenia podkładu
 * go nie odsłaniają.
 *
 * Wymagania: US1.8, NFR-56
 */
class BasemapRevealTest {
    private val reveal = BasemapReveal()

    /** Wiersz: V1, przelot na inny teren, kafelki podkładu się wczytują → podkład przenika raz, po wczytaniu. */
    @Test
    fun revealsOnceWhenLoaded() {
        reveal.hide()
        val frames =
            listOf(
                reveal.frame(isFull = true, zoom = 12.0, minZoom = 14.0),
                reveal.frame(isFull = false, zoom = 15.0, minZoom = 14.0),
                reveal.frame(isFull = true, zoom = 16.0, minZoom = 14.0),
                reveal.frame(isFull = true, zoom = 17.0, minZoom = 14.0),
            )
        val expected = listOf(false, false, true, false)
        assertEquals(expected, frames, "odsłonięcie tylko w pierwszej pełnej klatce nad powiększeniem podkładu")
    }

    /** Wiersz: V1, podkład nie był ukryty → żadna klatka go nie odsłania drugi raz. */
    @Test
    fun keepsShownBasemap() {
        assertEquals(false, reveal.frame(isFull = true, zoom = 16.0, minZoom = 14.0), "bez ukrycia nie ma odsłonięcia")
    }
}
