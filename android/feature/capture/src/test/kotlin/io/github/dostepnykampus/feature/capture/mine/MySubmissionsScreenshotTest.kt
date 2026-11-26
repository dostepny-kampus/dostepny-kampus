/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Zrzuty C3 z kartami zgłoszeń w telefonie, wysłanego i odrzuconego: jasny i ciemny motyw oraz czcionka 200 %.
 *
 * Wymagania: US8.1, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class MySubmissionsScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture() {
        val actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = {}, onSync = {})
        compose.setContent {
            DostepnyKampusTheme {
                MySubmissionsContent(state = MySubmissionsUiState.Ready(MySubmissions(ITEMS)), actions = actions)
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun drawsLightList() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkList() {
        capture()
    }

    /** Czcionka 200 %: czynność karty zostaje szeroka jak napis. Wymagania: NFR-20 */
    @Test
    fun drawsListAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private companion object {
        val ITEMS =
            listOf(
                item(id = "a", state = SendState.LOCAL),
                item(id = "b", state = SendState.SENT).copy(serverStatus = SubmissionStatus.IN_REVIEW),
                item(id = "c", state = SendState.REFUSED).copy(refusal = RefusalReason.NOT_ACCEPTED),
            )

        fun item(id: String, state: SendState) =
            OwnSubmissionItem(
                id = id,
                savedAt = Instant.parse("2026-11-09T12:00:00Z"),
                type = BarrierType.STAIRS_AT_ENTRANCE,
                buildingSymbol = "A-1",
                floorNumber = 2,
                state = state,
                refusal = null,
            )
    }
}
