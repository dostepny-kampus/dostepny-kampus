/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

/** Katalog zdjęć zgłoszeń w pamięci aplikacji; kopia zapasowa aplikacji jest wyłączona. */
internal const val PHOTO_DIR = "zdjecia"

// Dłuższy bok zdjęcia po zmniejszeniu: czytelna bariera, a plik daleko poniżej 5 MB (NFR-34).
private const val PHOTO_PIXELS = 2048
private const val JPEG_QUALITY = 85
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f

/**
 * Wymiary po zmniejszeniu tak, by dłuższy bok miał najwyżej [limit] pikseli; mniejszego zdjęcia nie powiększa.
 *
 * @return szerokość i wysokość z zachowaniem proporcji
 */
internal fun scaledSize(width: Int, height: Int, limit: Int = PHOTO_PIXELS): Pair<Int, Int> {
    val longer = maxOf(a = width, b = height)
    if (longer <= limit) return width to height
    val scale = limit.toDouble() / longer
    return maxOf(a = 1, b = (width * scale).toInt()) to maxOf(a = 1, b = (height * scale).toInt())
}

/** Obrót w stopniach z orientacji EXIF aparatu, żeby zdjęcie po przekodowaniu nie leżało bokiem. */
internal fun rotationDegrees(orientation: Int): Float =
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
        ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
        ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
        else -> 0f
    }

/** Największa potęga dwójki, po której dłuższy bok zostaje co najmniej [limit] pikseli. */
internal fun decodeSample(longerSide: Int, limit: Int = PHOTO_PIXELS): Int {
    var sample = 1
    while (longerSide / (sample * 2) >= limit) sample *= 2
    return sample
}

/**
 * Przekodowuje zdjęcie z aparatu w miejscu: obraca według EXIF, zmniejsza do [PHOTO_PIXELS] i zapisuje jako JPEG
 * bez metadanych, więc w pliku nie zostaje położenie z aparatu ani model telefonu (NFR-34).
 *
 * @return `false`, gdy pliku nie da się odczytać jako obrazu, np. aparat nic nie zapisał
 */
internal fun shrinkPhoto(file: File): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val sample = decodeSample(longerSide = maxOf(a = bounds.outWidth, b = bounds.outHeight))
    val decoded =
        BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return false
    val orientation = ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
    val (width, height) = scaledSize(width = decoded.width, height = decoded.height)
    val matrix =
        Matrix().apply {
            postScale(width.toFloat() / decoded.width, height.toFloat() / decoded.height)
            postRotate(rotationDegrees(orientation))
        }
    val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    return true
}
