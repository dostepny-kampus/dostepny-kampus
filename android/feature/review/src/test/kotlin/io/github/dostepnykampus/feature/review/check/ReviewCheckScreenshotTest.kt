/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.review.queue.queued
import io.github.dostepnykampus.feature.review.queue.quick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import io.github.dostepnykampus.core.designsystem.R as DesignR

/**
 * Zrzuty R2 „Sprawdzenie zgłoszenia” z danymi zgłoszenia, podobnymi i decyzją, jasny, ciemny i przy czcionce 200 %,
 * oraz zgłoszenie szybkie z wyborem typu i uciążliwości po próbie przyjęcia bez nich.
 *
 * Wymagania: US10, US10.1, US10.2, US10.3, NFR-20
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

    @Test
    @Config(qualifiers = "w360dp-h1600dp-xhdpi")
    fun drawsQuickWithoutType() {
        capture(quickWithoutType())
    }

    @Test
    @Config(qualifiers = "w360dp-h3200dp-xhdpi")
    fun drawsQuickWithoutTypeAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(quickWithoutType())
    }

    @Test
    fun drawsCoverActionBelowPhoto() {
        val photo = File.createTempFile("r2-photo", ".png", compose.activity.cacheDir)
        val bitmap = Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GRAY)
        try {
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            capture(ready().copy(photoUrls = listOf(Uri.fromFile(photo).toString())))
        } finally {
            bitmap.recycle()
            photo.delete()
        }
    }

    private fun quickWithoutType(): CheckUiState.Ready =
        ready(submission = quick(id = "a")).copy(notice = CheckNotice.TYPE_MISSING)

    private fun ready(submission: Submission = queued(id = "a", day = 27)): CheckUiState.Ready {
        val near = queued(id = "b", day = 20).copy(status = SubmissionStatus.ACCEPTED)
        return CheckUiState.Ready(
            submission = submission,
            photoUrls = emptyList(),
            similar = listOf(SimilarSubmission(submission = near, distanceMeters = 4.0)),
        )
    }

    private fun capture(state: CheckUiState = ready()) {
        compose.setContent {
            DostepnyKampusTheme {
                ReviewCheckContent(
                    state = state,
                    actions = ReviewCheckActions(onBack = {}, onDecided = {}, onCover = { _, _ -> }),
                    decision = DecisionActions(onAccept = {}, onReject = {}),
                    onRetry = {},
                )
            }
        }
        compose.waitForIdle()
        if (state is CheckUiState.Ready && state.photoUrls.isNotEmpty()) {
            val loading = compose.activity.getString(DesignR.string.zdjecie_wczytujemy)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(loading).fetchSemanticsNodes().isEmpty()
            }
        }
        compose.onRoot().captureRoboImage()
    }
}
