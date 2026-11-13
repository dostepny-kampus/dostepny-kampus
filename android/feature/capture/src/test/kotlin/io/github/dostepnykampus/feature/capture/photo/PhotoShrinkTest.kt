/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

private const val CAMERA_QUALITY = 95

/**
 * Plik z aparatu przekodowany przez plik tymczasowy: mniejszy JPEG, a pusty plik nie udaje zdjęcia.
 *
 * Wymagania: US7.1, NFR-34
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoShrinkTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun us7_1_camera_photo_shrunk() {
        val file = folder.newFile("aparat.jpg")
        val image = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, CAMERA_QUALITY, it) }

        assertTrue(shrinkPhoto(file))

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        assertEquals(2048, bounds.outWidth)
        assertEquals(1536, bounds.outHeight)
        assertEquals(listOf("aparat.jpg"), folder.root.list()?.toList())
    }

    @Test
    fun us7_1_failed_write_kept() {
        val file = folder.newFile("aparat.jpg").apply { writeText("oryginał") }

        val isReplaced =
            replaceFile(file) { stream ->
                stream.write(1)
                false
            }

        assertFalse(isReplaced)

        assertEquals("oryginał", file.readText())
        assertEquals(listOf("aparat.jpg"), folder.root.list()?.toList())
    }

    @Test
    fun us7_1_write_error_kept() {
        val file = folder.newFile("aparat.jpg").apply { writeText("oryginał") }

        assertFalse(replaceFile(file) { throw IOException("brak miejsca") })

        assertEquals("oryginał", file.readText())
        assertEquals(listOf("aparat.jpg"), folder.root.list()?.toList())
    }

    /** Orientacje EXIF z odbiciem i bez: po przekodowaniu ćwiartki leżą tak, jak widział je aparat. */
    @Test
    fun us7_1_exif_orientation() {
        val expected =
            mapOf(
                ExifInterface.ORIENTATION_NORMAL to listOf(RED, GREEN, BLUE, WHITE),
                ExifInterface.ORIENTATION_ROTATE_90 to listOf(BLUE, RED, WHITE, GREEN),
                ExifInterface.ORIENTATION_ROTATE_180 to listOf(WHITE, BLUE, GREEN, RED),
                ExifInterface.ORIENTATION_ROTATE_270 to listOf(GREEN, WHITE, RED, BLUE),
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL to listOf(GREEN, RED, WHITE, BLUE),
                ExifInterface.ORIENTATION_FLIP_VERTICAL to listOf(BLUE, WHITE, RED, GREEN),
                ExifInterface.ORIENTATION_TRANSPOSE to listOf(RED, BLUE, GREEN, WHITE),
                ExifInterface.ORIENTATION_TRANSVERSE to listOf(WHITE, GREEN, BLUE, RED),
            )
        expected.forEach { (orientation, quadrants) ->
            val file = quadrantPhoto(name = "aparat$orientation.jpg", orientation = orientation)

            assertTrue(shrinkPhoto(file))

            assertEquals("orientacja $orientation", quadrants, quadrantsOf(file))
        }
    }

    @Test
    fun us7_1_empty_file_not_photo() {
        assertFalse(shrinkPhoto(folder.newFile("pusty.jpg")))
    }

    private fun quadrantPhoto(name: String, orientation: Int): File {
        val image = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
        val half = SIDE / 2
        val canvas = Canvas(image)
        QUADRANTS.forEachIndexed { index, color ->
            val left = index % 2 * half
            val top = index / 2 * half
            canvas.drawRect(Rect(left, top, left + half, top + half), Paint().apply { this.color = color })
        }
        val file = folder.newFile(name)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, CAMERA_QUALITY, it) }
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file
    }

    private fun quadrantsOf(file: File): List<Int> {
        val image = BitmapFactory.decodeFile(file.path)
        val near = SIDE / 4
        val far = SIDE - near
        return listOf(near to near, far to near, near to far, far to far)
            .map { (x, y) -> pure(image.getPixel(x, y)) }
    }

    private fun pure(color: Int): Int =
        Color.rgb(
            if (Color.red(color) > HALF_CHANNEL) MAX_CHANNEL else 0,
            if (Color.green(color) > HALF_CHANNEL) MAX_CHANNEL else 0,
            if (Color.blue(color) > HALF_CHANNEL) MAX_CHANNEL else 0,
        )

    private companion object {
        const val SIDE = 64
        const val HALF_CHANNEL = 127
        const val MAX_CHANNEL = 255
        val RED = Color.rgb(MAX_CHANNEL, 0, 0)
        val GREEN = Color.rgb(0, MAX_CHANNEL, 0)
        val BLUE = Color.rgb(0, 0, MAX_CHANNEL)
        val WHITE = Color.rgb(MAX_CHANNEL, MAX_CHANNEL, MAX_CHANNEL)

        /** Ćwiartki zdjęcia z aparatu: lewa górna, prawa górna, lewa dolna, prawa dolna. */
        val QUADRANTS = listOf(RED, GREEN, BLUE, WHITE)
    }
}
