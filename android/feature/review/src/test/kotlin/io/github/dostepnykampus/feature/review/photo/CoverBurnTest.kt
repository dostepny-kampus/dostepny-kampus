/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/** Zasłony wypalone w pikselach zdjęcia JPEG; reszta zdjęcia bez zmian. Wymagania: BR-15, NFR-25 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverBurnTest {
    @Test
    fun blackensCoveredPixelsOnly() {
        val covered = burnCovers(whiteJpeg(), listOf(Cover(x = 0.5f, y = 0.5f, size = Cover.START)))
        val image = BitmapFactory.decodeByteArray(covered, 0, covered.size)
        assertEquals("ten sam rozmiar zdjęcia", SIDE, image.width)
        assertTrue("środek zasłonięty", brightness(image.getPixel(SIDE / 2, SIDE / 2)) < DARK)
        assertTrue("róg bez zmian", brightness(image.getPixel(2, 2)) > LIGHT)
    }

    private fun whiteJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
    }

    private fun brightness(pixel: Int): Int = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

    private companion object {
        const val SIDE = 100
        const val QUALITY = 95
        const val DARK = 40
        const val LIGHT = 215
    }
}
