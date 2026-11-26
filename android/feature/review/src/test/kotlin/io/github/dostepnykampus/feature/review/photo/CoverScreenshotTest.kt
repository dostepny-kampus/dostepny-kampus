/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty ekranu zasłaniania z dwiema zasłonami, druga zaznaczona, jasny, ciemny i przy czcionce 200 %.
 *
 * Wymagania: BR-15, NFR-20, NFR-21, NFR-53
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class CoverScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsLightCover() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkCover() {
        capture()
    }

    @Test
    fun drawsCoverAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    private fun capture() {
        val covers = Covers().tap(x = 0.3f, y = 0.3f).tap(x = 0.7f, y = 0.6f)
        val state = CoverUiState.Ready(photo = CoverPhoto(grayJpeg()), covers = covers)
        compose.setContent { DostepnyKampusTheme { CoverContent(state = state, actions = coverActions()) } }
        compose.waitUntilAtLeastOneExists(hasContentDescription("Zdjęcie do zasłonięcia"), timeoutMillis = WAIT)
        compose.onRoot().captureRoboImage()
    }

    private companion object {
        const val WAIT = 5_000L
    }
}
