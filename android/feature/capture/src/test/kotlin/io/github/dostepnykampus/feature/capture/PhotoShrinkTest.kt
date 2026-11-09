/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val CAMERA_QUALITY = 95

/** Plik z aparatu przekodowany w miejscu: mniejszy JPEG, a pusty plik nie udaje zdjęcia (US7.1, NFR-34). */
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
    }

    @Test
    fun us7_1_empty_file_not_photo() {
        assertFalse(shrinkPhoto(folder.newFile("pusty.jpg")))
    }
}
