/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.capture.photo.LocalPhotoDecodeContext
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.report.form.PlanFloor
import io.github.dostepnykampus.feature.capture.report.form.PlanFloors
import io.github.dostepnykampus.feature.capture.report.form.ReportFloor
import io.github.dostepnykampus.feature.capture.report.form.ReportForm
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError
import io.github.dostepnykampus.feature.capture.report.form.ReportStage
import io.github.dostepnykampus.feature.capture.report.form.ReportUiState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** Stany formularza C2 z makiet: nowe zgłoszenie, edycja i błędy. Wymagania: US7.1, US8.3, NFR-7, NFR-20 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1500dp-xhdpi")
class ReportScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun drawsLightNew() {
        capture(ReportUiState(form = NEW))
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkNew() {
        capture(ReportUiState(form = NEW))
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xhdpi", fontScale = 2f)
    fun drawsNewAtFont200() {
        capture(ReportUiState(form = NEW))
    }

    @Test
    fun drawsValidationError() {
        val form = NEW.copy(floor = ReportFloor.Planned("a1-0"), severity = Severity.BLOCKING)
        capture(ReportUiState(form = form, errors = listOf(ReportFormError.MISSING_BARRIER_TYPE)))
    }

    @Test
    @Config(qualifiers = "w360dp-h1950dp-xhdpi")
    fun drawsEditedValues() {
        capture(editedState())
    }

    @Test
    @Config(qualifiers = "w360dp-h1950dp-xhdpi")
    fun drawsSaveFailure() {
        capture(editedState().copy(stage = ReportStage.SAVE_FAILED))
    }

    private fun capture(state: ReportUiState) {
        val actions =
            ReportActions(
                onEdit = {},
                onChangePlace = {},
                camera = PhotoCamera(onTake = {}),
                onSave = {},
                onBack = {},
                onConfirmExit = {},
                onStay = {},
            )
        compose.setContent {
            DostepnyKampusTheme {
                CompositionLocalProvider(LocalPhotoDecodeContext provides EmptyCoroutineContext) {
                    ReportContent(state = state, actions = actions)
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    private fun editedState(): ReportUiState {
        val file = folder.newFile("bariera.png")
        Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val photo = PendingPhotoDraft(photoId = "p1", path = file.path)
        val form =
            ReportForm(
                place = PLACE.copy(buildingSymbol = "C-13", buildingName = "Centrum Studenckie"),
                floor = ReportFloor.Numbered(number = 2, isMezzanine = true),
                barrierType = BarrierType.OTHER_BARRIER,
                severity = Severity.ONLY_WITH_HELP,
                photoIds = listOf(photo.photoId),
                description = "Przeszkoda przy drzwiach na korytarzu.",
                editedSubmission = EditedSubmission(id = "s1", baseVersion = 1),
            )
        return ReportUiState(form = form, photos = listOf(photo))
    }

    private companion object {
        val PLACE = A1.copy(buildingName = "Gmach Główny")
        val PLAN =
            PlanFloors(
                items = listOf(PlanFloor(id = "a1-0", label = "Parter"), PlanFloor(id = "a1-1", label = "1 piętro")),
                source = "Politechnika Wrocławska",
            )
        val NEW = ReportForm(place = PLACE.copy(floors = PLAN))
    }
}
