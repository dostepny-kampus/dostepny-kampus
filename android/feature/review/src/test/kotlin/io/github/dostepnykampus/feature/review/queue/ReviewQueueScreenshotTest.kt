/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty R1 „Zgłoszenia do sprawdzenia” z „Wziętymi przeze mnie” i kolejką, jasny, ciemny i przy czcionce 200 %.
 *
 * Wymagania: US10.4, US10.7, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ReviewQueueScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsLightQueue() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkQueue() {
        capture()
    }

    @Test
    fun drawsQueueAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private fun capture() {
        val state =
            ReviewQueueUiState.Ready(
                waiting = listOf(queued(id = "a", day = 27), queued(id = "b", day = 28)),
                mine = listOf(queued(id = "c", day = 26).copy(status = SubmissionStatus.IN_REVIEW)),
            )
        compose.setContent {
            DostepnyKampusTheme {
                ReviewQueueContent(
                    state = state,
                    actions = ReviewQueueActions(onBack = {}, onOpen = {}),
                    onClaim = {},
                    onRetry = {},
                )
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }
}
