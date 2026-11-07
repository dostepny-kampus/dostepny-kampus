/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Podgląd zdjęcia w C2 czyta zmniejszony obraz, a nie pełny JPEG z aparatu (US7.1). */
class PhotoPreviewTest {
    @Test
    fun `małe zdjęcie bez zmniejszania`() {
        assertEquals(1, sampleSize(1439))
    }

    @Test
    fun `duże zdjęcie zmniejszone`() {
        assertEquals(2, sampleSize(1440))
        assertEquals(4, sampleSize(4000))
    }
}
