/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Krok 1 V4: „Zrób zdjęcie” jako jedyna czynność główna, a po powrocie z kroku 2 „Dalej” nad „Zrób nowe zdjęcie”.
 *
 * Wymagania: US4.1, US4.8, NFR-52, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickPhotoButtonsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    private val shown = mutableStateOf(ReportRoute())

    private val actions = QuickReportActions(onNext = {}, onSent = {}, onBack = {}, onBackToMap = {})

    @Before
    fun grant() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** Wiersz: krok 1 przed zdjęciem, „Zrób zdjęcie” przypięte pod „Ważne”, bez nagłówka. Wymagania: US4.1 */
    @Test
    fun pinsTakeBelowNotice() {
        show(ReportRoute(step = ReportStep.PHOTO))
        val notice = bounds("Nie fotografuj twarzy ani tablic rejestracyjnych.")
        val take = bounds("Zrób zdjęcie")
        assertTrue("„Zrób zdjęcie” pod komunikatem", take.top >= notice.bottom)
        compose.onNodeWithText("Zdjęcie").assertDoesNotExist()
    }

    /** Wiersz: krok 1 po powrocie z kroku 2, „Dalej” nad „Zrób nowe zdjęcie”. Wymagania: US4.8 */
    @Test
    fun putsNextAboveRetake() {
        show(withPhoto())
        assertTrue("„Dalej” nad „Zrób nowe zdjęcie”", bounds("Dalej").bottom <= bounds("Zrób nowe zdjęcie").top)
    }

    /** Wiersz: czcionka 200 %, przypięte „Dalej”, „Zrób nowe zdjęcie” w treści. Wymagania: NFR-52, NFR-20 */
    @Test
    fun keepsNextPinnedAtLargeFont() {
        RuntimeEnvironment.setFontScale(2f)
        show(withPhoto())
        assertTrue("„Zrób nowe zdjęcie” w treści nad „Dalej”", bounds("Zrób nowe zdjęcie").top < bounds("Dalej").top)
    }

    private fun withPhoto(): ReportRoute {
        val photo = folder.newFile("zdjecie-1.jpg")
        photo.outputStream().use { out ->
            Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        }
        return ReportRoute(step = ReportStep.PHOTO, photoId = "zdjecie-1", photoPath = photo.path)
    }

    private fun show(route: ReportRoute) {
        shown.value = route
        val send = quickSendModel(positions = PositionSource { flowOf() }, saver = { _, _, _ -> })
        compose.showQuickReport(route = shown, actions = actions, send = send)
    }

    private fun bounds(text: String): DpRect =
        compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private companion object {
        const val QUALITY = 90
    }
}
