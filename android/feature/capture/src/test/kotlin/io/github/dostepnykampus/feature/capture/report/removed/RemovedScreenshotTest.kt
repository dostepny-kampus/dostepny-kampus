/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Zrzuty C7 ze zgłoszeniem przyjętym: jasny i ciemny motyw, czcionka 200 % oraz błąd walidacji bez zdjęcia.
 *
 * Wymagania: US8.4, BR-19, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class RemovedScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(state: RemovedUiState) {
        val actions = RemovedActions(camera = PhotoCamera(onTake = {}), onSend = {}, onBack = {})
        compose.setContent { DostepnyKampusTheme { RemovedContent(state = state, actions = actions) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    /** C7 przed wysłaniem: dane zgłoszenia przyjętego i miejsce na nowe zdjęcie. Wymagania: US8.4 */
    @Test
    fun drawsLightEditing() {
        capture(RemovedUiState(previous = LIFT, stage = RemovedStage.EDITING))
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkEditing() {
        capture(RemovedUiState(previous = LIFT, stage = RemovedStage.EDITING))
    }

    /** Czcionka 200 %: treść przewija się, przycisk wysłania zostaje osiągalny. Wymagania: NFR-20 */
    @Test
    fun drawsEditingAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(RemovedUiState(previous = LIFT, stage = RemovedStage.EDITING))
    }

    /** Wysłanie bez zdjęcia: komunikat walidacji przy sekcji zdjęcia. Wymagania: BR-19 */
    @Test
    fun drawsLightPhotoMissing() {
        capture(RemovedUiState(previous = LIFT, isPhotoMissing = true, stage = RemovedStage.EDITING))
    }

    private companion object {
        val LIFT =
            AcceptedBarrier(
                id = "tb9",
                location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                severity = Severity.BLOCKING,
                type = BarrierType.BROKEN_LIFT,
                buildingSymbol = "A-1",
                verifiedAt = Instant.parse("2026-10-07T10:00:00Z"),
                floor = SubmissionFloor(number = 1),
            )
    }
}
