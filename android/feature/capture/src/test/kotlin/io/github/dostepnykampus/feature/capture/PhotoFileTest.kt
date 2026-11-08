/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.media.ExifInterface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Zdjęcie z aparatu po przekodowaniu: wymiary do 2048 px i obrót według EXIF (US7.1, NFR-34). */
class PhotoFileTest {
    @Test
    fun `duże zdjęcie zmniejszone`() {
        assertEquals(2048 to 1536, scaledSize(width = 4000, height = 3000))
        assertEquals(1536 to 2048, scaledSize(width = 3000, height = 4000))
    }

    @Test
    fun `małe zdjęcie bez powiększania`() {
        assertEquals(1600 to 1200, scaledSize(width = 1600, height = 1200))
    }

    @Test
    fun `obrót z orientacji EXIF`() {
        assertEquals(90f, rotationDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180f, rotationDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270f, rotationDegrees(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0f, rotationDegrees(ExifInterface.ORIENTATION_NORMAL))
    }

    @Test
    fun `odczyt co potęgę dwójki`() {
        assertEquals(1, decodeSample(longerSide = 4095))
        assertEquals(2, decodeSample(longerSide = 4096))
        assertEquals(4, decodeSample(longerSide = 8192))
    }
}
