/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.review.queue.queued
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty R2 „Sprawdzenie zgłoszenia” z danymi zgłoszenia i podobnymi zgłoszeniami, jasny, ciemny i przy czcionce
 * 200 %.
 *
 * Wymagania: US10.1, US10.2, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ReviewCheckScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsLightCheck() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkCheck() {
        capture()
    }

    @Test
    fun drawsCheckAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private fun capture() {
        val near = queued(id = "b", day = 20).copy(status = SubmissionStatus.ACCEPTED)
        val state =
            CheckUiState.Ready(
                submission = queued(id = "a", day = 27),
                photoUrls = emptyList(),
                similar = listOf(SimilarSubmission(submission = near, distanceMeters = 4.0)),
            )
        compose.setContent {
            DostepnyKampusTheme {
                ReviewCheckContent(
                    state = state,
                    actions = ReviewCheckActions(onBack = {}, onDecided = {}),
                    decision = DecisionActions(onAccept = {}, onReject = {}),
                    onRetry = {},
                )
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }
}
