/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Zdjęcia zmiany „bariera usunięta”: obok siebie, a przy dużej czcionce jedno pod drugim. Wymagania: US10.8, NFR-20 */
@RunWith(RobolectricTestRunner::class)
class RemovedComparisonTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun photosSideBySide() {
        show(fontScale = 1f)
        assertTrue("nowe zdjęcie na prawo od poprzedniego", newCaption().left > previousCaption().left)
    }

    @Test
    fun photosStackedAtLargeFont() {
        show(fontScale = 2f)
        assertTrue("nowe zdjęcie pod poprzednim", newCaption().top > previousCaption().bottom)
    }

    private fun previousCaption(): DpRect = bounds("Zdjęcie z poprzedniego zgłoszenia")

    private fun newCaption(): DpRect = bounds("Nowe zdjęcie miejsca")

    private fun bounds(text: String): DpRect = compose.onNodeWithText(text).getUnclippedBoundsInRoot()

    private fun show(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DostepnyKampusTheme {
                    RemovedComparison(
                        previous = PreviousSubmission(status = SubmissionStatus.ACCEPTED, photoUrl = "p/p-p"),
                        newPhoto = "a/p-a",
                        type = null,
                    )
                }
            }
        }
    }
}
