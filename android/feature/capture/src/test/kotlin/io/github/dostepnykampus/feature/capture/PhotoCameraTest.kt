/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.IntentCompat
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowActivity
import java.io.File

private const val CAMERA_QUALITY = 90

/** „Zrób zdjęcie” na C2: aparat zapisuje plik aplikacji, a zdjęcie wraca do formularza (US7.1). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCameraTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show() {
        val viewModel = ReportFormViewModel { _, _, _ -> }
        val route = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = null)
        compose.setContent {
            DostepnyKampusTheme { ReportFormScreen(route = route, onLeave = {}, onSave = {}, viewModel = viewModel) }
        }
    }

    private fun openCamera(): Pair<ShadowActivity.IntentForResult, File> {
        compose.onNodeWithText("Zrób zdjęcie").performScrollTo().performClick()
        val request = shadowOf(compose.activity).nextStartedActivityForResult
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, request.intent.action)
        val output = IntentCompat.getParcelableExtra(request.intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)
        val name = output?.lastPathSegment.orEmpty()
        return request to File(File(compose.activity.filesDir, PHOTO_DIR), name)
    }

    /**
     * Wiersz: C2, rezygnacja w aparacie → C2 bez zdjęcia, potem „Zrób zdjęcie” → aparat → C2 ze zdjęciem, US7.1.
     * Jeden test, bo FileProvider zapamiętuje katalog plików pierwszej aktywności na cały proces testów.
     */
    @Test
    fun us7_1_cancel_then_photo() {
        show()
        val (cancelled, empty) = openCamera()
        shadowOf(compose.activity).receiveResult(cancelled.intent, Activity.RESULT_CANCELED, null)
        compose.waitForIdle()
        assertFalse(empty.exists())

        val (request, file) = openCamera()
        val image = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, CAMERA_QUALITY, it) }
        shadowOf(compose.activity).receiveResult(request.intent, Activity.RESULT_OK, null)

        val preview = hasContentDescription("Zrobione zdjęcie 1 z 1")
        compose.waitUntil { compose.onAllNodes(preview).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Zrób nowe zdjęcie").performScrollTo()
    }
}
