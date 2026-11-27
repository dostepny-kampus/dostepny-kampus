/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
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
 * Zrzuty L1 według makiet `m-l1-konto` i `m-l1-konto-brak-sieci`.
 *
 * Wymagania: US14.2, UC6, NFR-52
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class SignInScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(
        state: SignInUiState,
        scrollToEnd: Boolean = false,
    ) {
        compose.setContent {
            DostepnyKampusTheme {
                SignInContent(
                    state = state,
                    isForReport = true,
                    actions = SignInActions(onBack = {}, onLeave = {}),
                    onOpen = {},
                    onRetry = {},
                )
            }
        }
        compose.waitForIdle()
        if (scrollToEnd) compose.onNodeWithText("Nie teraz").performScrollTo()
        compose.onRoot().captureRoboImage()
    }

    /** Prośba o konto przy wejściu ze „Zgłoś barierę”: trzy przyciski przypięte na dole. */
    @Test
    fun drawsLightRequest() {
        capture(SignInUiState())
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkRequest() {
        capture(SignInUiState())
    }

    /** Czcionka 200 %: trzy przyciski na końcu treści. Wymagania: NFR-52 */
    @Test
    fun drawsRequestAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(SignInUiState())
    }

    /** Brak sieci: plakietka, komunikat i „Wróć do mapy”. */
    @Test
    fun drawsLightOffline() {
        capture(SignInUiState(isOnline = false))
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkOffline() {
        capture(SignInUiState(isOnline = false))
    }

    /** Błąd logowania: „Spróbuj ponownie” i „Nie teraz”. */
    @Test
    fun drawsLightFailure() {
        capture(SignInUiState(stage = SignInStage.FAILED))
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp-xhdpi")
    fun drawsWideContent() {
        capture(SignInUiState())
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp-xhdpi", fontScale = 2f)
    fun drawsWideContentAtFont200() {
        capture(SignInUiState())
    }

    @Test
    @Config(qualifiers = "w840dp-h360dp-xhdpi")
    fun drawsLandscapeContent() {
        capture(SignInUiState())
    }

    @Test
    @Config(qualifiers = "w840dp-h360dp-xhdpi", fontScale = 2f)
    fun drawsLandscapeContentAtFont200() {
        capture(SignInUiState(), scrollToEnd = true)
    }
}
