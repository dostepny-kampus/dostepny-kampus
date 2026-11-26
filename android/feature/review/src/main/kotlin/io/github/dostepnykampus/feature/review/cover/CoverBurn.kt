/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.cover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ByteArrayOutputStream

private const val JPEG_QUALITY = 90

/**
 * Zdjęcie JPEG z zasłonami wypalonymi w pikselach jako czarne prostokąty; reszta zdjęcia bez zmian.
 *
 * Wymagania: BR-15, NFR-25
 *
 * @param jpeg bajty zdjęcia
 * @param covers zasłony we współrzędnych względnych zdjęcia
 * @return bajty nowego zdjęcia JPEG
 * @throws IllegalArgumentException gdy bajty nie są obrazem
 */
fun burnCovers(jpeg: ByteArray, covers: List<Cover>): ByteArray {
    val options = BitmapFactory.Options().apply { inMutable = true }
    val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
    val image = requireNotNull(decoded) { "Zdjęcie nie jest obrazem" }
    val canvas = Canvas(image)
    val paint = Paint().apply { color = Color.BLACK }
    covers.forEach { cover ->
        val half = cover.size / 2
        canvas.drawRect(
            (cover.x - half) * image.width,
            (cover.y - half) * image.height,
            (cover.x + half) * image.width,
            (cover.y + half) * image.height,
            paint,
        )
    }
    return ByteArrayOutputStream().use { out ->
        image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }
}
