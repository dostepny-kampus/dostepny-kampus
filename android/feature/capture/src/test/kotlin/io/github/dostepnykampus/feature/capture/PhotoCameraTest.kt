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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowActivity
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext

private const val CAMERA_QUALITY = 90

/** „Zrób zdjęcie” na C2: aparat zapisuje plik aplikacji, a zdjęcie wraca do formularza. Wymagania: US7.1 */
@RunWith(RobolectricTestRunner::class)
@Config(
    qualifiers = "w360dp-h800dp",
    instrumentedPackages = ["androidx.core.content"],
    shadows = [PhotoCameraTest.FileProviderPathShadow::class],
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCameraTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var photos = 0
    private var path = ""
    private var generation by mutableIntStateOf(0)

    /** FileProvider zakłada separator Androida, a Robolectric używa ścieżek systemu hosta. */
    @Implements(className = "androidx.core.content.FileProvider\$SimplePathStrategy", isInAndroidSdk = false)
    class FileProviderPathShadow {
        @Implementation
        fun belongsToRoot(filePath: String, rootPath: String): Boolean {
            val file = Path.of(filePath)
            val root = Path.of(rootPath)
            return file != root && file.startsWith(root)
        }
    }

    /** FileProvider trzyma katalogi pierwszej aktywności w statycznej pamięci, a każdy test ma nowy katalog plików. */
    @Before
    fun forgetFileProviderRoots() {
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    private fun show() {
        val viewModel = ReportFormViewModel { _, _, _ -> }
        val route = ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = null)
        val transcoder = PhotoTranscoder(EmptyCoroutineContext)
        compose.setContent {
            DostepnyKampusTheme {
                CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
                    ReportFormScreen(
                        route = route,
                        onLeave = {},
                        onSave = {},
                        viewModel = viewModel,
                        photoTranscoder = transcoder,
                    )
                }
            }
        }
    }

    private fun showCamera(transcoder: PhotoTranscoder) {
        compose.setContent {
            DostepnyKampusTheme {
                CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
                    key(generation) { CameraHost(transcoder) }
                }
            }
        }
    }

    @Composable
    private fun CameraHost(transcoder: PhotoTranscoder) {
        val camera =
            rememberPhotoCamera(
                onPhoto = { photo ->
                    photos++
                    path = photo.path
                },
                transcoder = transcoder,
            )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            PhotoSection(photos = PhotoPaths(listOf(path).filter { it.isNotEmpty() }), camera = camera)
        }
    }

    private fun writePhoto(file: File) {
        val image = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, CAMERA_QUALITY, it) }
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
     * C2 wraca z aparatu bez zdjęcia po anulowaniu i ze zdjęciem po zatwierdzeniu.
     * Obie próby używają jednej aktywności, bo FileProvider zapamiętuje katalog plików.
     *
     * Wymagania: US7.1
     */
    @Test
    fun us7_1_cancel_then_photo() {
        val strategy = Class.forName("androidx.core.content.FileProvider\$SimplePathStrategy")
        val method = strategy.getDeclaredMethod("belongsToRoot", String::class.java, String::class.java)
        assertEquals(Boolean::class.javaPrimitiveType, method.returnType)
        show()
        val (cancelled, empty) = openCamera()
        shadowOf(compose.activity).receiveResult(cancelled.intent, Activity.RESULT_CANCELED, null)
        compose.waitForIdle()
        assertFalse(empty.exists())

        val (request, file) = openCamera()
        writePhoto(file)
        shadowOf(compose.activity).receiveResult(request.intent, Activity.RESULT_OK, null)

        compose.waitForIdle()
        compose.onNode(hasContentDescription("Zrobione zdjęcie 1 z 1")).assertExists()
        compose.onNodeWithText("Zrób nowe zdjęcie").performScrollTo()
    }

    /** Błąd przekodowania: komunikat, plik usunięty, aplikacja działa, a „Zrób zdjęcie” otwiera aparat ponownie. */
    @Test
    fun nfr50_photo_error_retry() {
        val broken = PhotoTranscoder(EmptyCoroutineContext) { throw IOException(BROKEN) }
        showCamera(broken)
        val (request, file) = openCamera()
        file.writeText(BROKEN)
        shadowOf(compose.activity).receiveResult(request.intent, Activity.RESULT_OK, null)

        compose.waitForIdle()
        compose.onNodeWithText(ERROR).assertIsDisplayed()
        assertFalse(file.exists())
        assertEquals(0, photos)

        val (retry, next) = openCamera()
        compose.onNodeWithText(ERROR).assertDoesNotExist()
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, retry.intent.action)
        assertNotEquals(file, next)
    }

    /**
     * Kompozycja odtworzona w trakcie przekodowania, jak po obrocie: zdjęcie dociera, bez plików-sierot.
     *
     * Wymagania: NFR-22
     */
    @Test
    fun nfr22_photo_kept_on_rotation() {
        val work = StandardTestDispatcher()
        showCamera(PhotoTranscoder(work))
        val (request, file) = openCamera()
        writePhoto(file)
        shadowOf(compose.activity).receiveResult(request.intent, Activity.RESULT_OK, null)
        compose.waitForIdle()

        generation++
        compose.waitForIdle()
        work.scheduler.advanceUntilIdle()

        compose.waitForIdle()
        assertEquals(1, photos)
        assertEquals(file.path, path)
        assertEquals(listOf(file.name), file.parentFile?.list()?.toList())
    }

    private companion object {
        const val BROKEN = "uszkodzony"
        const val ERROR = "Nie zapisaliśmy zdjęcia. Zrób je jeszcze raz."
    }
}
