/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Katalog zdjęć zgłoszeń w pamięci aplikacji; kopia zapasowa aplikacji jest wyłączona. */
internal const val PHOTO_DIR = "zdjecia"

private const val PHOTO_PIXELS = 2048
private const val JPEG_QUALITY = 85
private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val THREE_QUARTER_TURN = 270f
private const val TEMP_SUFFIX = ".tmp"

/**
 * Wymiary po zmniejszeniu tak, by dłuższy bok miał najwyżej [limit] pikseli; mniejszego zdjęcia nie powiększa.
 * Domyślny [limit] to dłuższy bok po zmniejszeniu: czytelna bariera, a plik daleko poniżej 5 MB (NFR-34).
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
 * Przekodowuje zdjęcie z aparatu: obraca według EXIF, zmniejsza do [PHOTO_PIXELS] i zapisuje jako JPEG
 * bez metadanych, więc w pliku nie zostaje położenie z aparatu ani model telefonu (NFR-34).
 * Nowy plik zastępuje oryginał dopiero po udanym zapisie, przez [replaceFile].
 *
 * @return `false`, gdy pliku nie da się odczytać jako obrazu, np. aparat nic nie zapisał, albo zapis się nie udał
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
    return replaceFile(file) { result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
}

/**
 * Zapisuje nową treść do pliku tymczasowego obok [file] i dopiero po udanym zapisie podmienia nim oryginał.
 * Nieudany zapis zostawia oryginał bez zmian i usuwa plik tymczasowy.
 *
 * @param file plik podmieniany nową treścią
 * @param write zapis treści; `false` oznacza nieudany zapis
 * @return `true`, gdy [file] ma już nową treść
 */
internal fun replaceFile(file: File, write: (OutputStream) -> Boolean): Boolean {
    val temp = File(file.parentFile, file.name + TEMP_SUFFIX)
    val isReplaced =
        try {
            temp.outputStream().use(write) && moveOver(source = temp, target = file)
        } catch (_: IOException) {
            false
        }
    if (!isReplaced) temp.delete()
    return isReplaced
}

private fun moveOver(source: File, target: File): Boolean {
    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    return true
}
