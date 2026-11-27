/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Zrzuty C6: jasny i ciemny motyw, czcionka 200 %, potwierdzenie obecnego oznaczenia i błąd walidacji.
 *
 * Wymagania: US17.2, BR-24, NFR-20, NFR-21
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MarkingScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(state: MarkingUiState) {
        val actions = MarkingActions(camera = PhotoCamera(onTake = {}), onChoose = {}, onSend = {}, onBack = {})
        compose.setContent { DostepnyKampusTheme { MarkingContent(state = state, actions = actions) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    /** C6 przed wyborem: obecne oznaczenie i dwa wybory. Wymagania: US17.2, US17.3 */
    @Test
    @Config(qualifiers = "w360dp-h1080dp-xhdpi")
    fun drawsLightEditing() {
        capture(EDITING)
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkEditing() {
        capture(EDITING)
    }

    /** Czcionka 200 %: treść przewija się, przycisk wysłania zostaje osiągalny. Wymagania: NFR-20 */
    @Test
    fun drawsEditingAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(EDITING)
    }

    /** Wybór obecnej wartości: napis o potwierdzeniu pod wybranym oznaczeniem. Wymagania: BR-24 */
    @Test
    fun drawsLightConfirming() {
        capture(EDITING.copy(chosen = EntranceMarking.INACCESSIBLE))
    }

    /** Wysłanie bez wyboru i zdjęcia: komunikaty walidacji przy obu polach. Wymagania: BR-7, BR-24 */
    @Test
    fun drawsLightMissing() {
        capture(EDITING.copy(isChoiceMissing = true, isPhotoMissing = true))
    }

    private companion object {
        val EDITING =
            MarkingUiState(
                entrance =
                    Entrance(
                        id = "n/1",
                        buildingSymbol = "A-1",
                        name = "Wejście Cesarskie",
                        isMain = false,
                        type = null,
                        location = GeoPoint(longitude = 17.06, latitude = 51.1),
                        marking = EntranceMarking.INACCESSIBLE,
                        assessment =
                            EntranceAssessment(date = LocalDate.of(2023, 12, 18), source = AssessmentSource.IMPORT),
                    ),
                buildingName = "Gmach Główny",
                stage = MarkingStage.EDITING,
            )
    }
}
