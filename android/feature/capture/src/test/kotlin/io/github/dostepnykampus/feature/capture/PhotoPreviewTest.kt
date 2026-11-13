/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Podgląd zdjęcia w C2 czyta zmniejszony obraz, a nie pełny JPEG z aparatu. Wymagania: US7.1 */
class PhotoPreviewTest {
    @Test
    fun `małe zdjęcie bez zmniejszania`() {
        assertEquals(1, decodeSample(longerSide = 1439, limit = 720))
    }

    @Test
    fun `duże zdjęcie zmniejszone`() {
        assertEquals(2, decodeSample(longerSide = 1440, limit = 720))
        assertEquals(4, decodeSample(longerSide = 4000, limit = 720))
    }
}
