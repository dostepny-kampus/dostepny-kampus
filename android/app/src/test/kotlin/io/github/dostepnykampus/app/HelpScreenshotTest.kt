/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty V5 z paskiem dolnym w stanach makiet hi-fi: krok 1 przy pierwszym uruchomieniu, krok 2 z „Pomoc”.
 *
 * Wymagania: NFR-16
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class HelpScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(isFirstStep: Boolean) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = isFirstStep, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                    if (route !is MapRoute) AppScreen(route)
                }
            }
        }
        if (!isFirstStep) {
            compose.onNodeWithText("Pomoc").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Dalej").performSemanticsAction(SemanticsActions.OnClick)
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    /** Krok 1 przy pierwszym uruchomieniu: pole wyszukiwania V1 jako obraz. Wymagania: US5.1, NFR-18 */
    @Test
    fun drawsLightStep1() {
        capture(isFirstStep = true)
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkStep1() {
        capture(isFirstStep = true)
    }

    /** Czcionka 200 %: „Dalej” i „Pomiń instrukcję” na końcu treści (reguła 10). Wymagania: NFR-20 */
    @Test
    fun drawsStep1AtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(isFirstStep = true)
    }

    /** Krok 2 z paska „Pomoc”: wejście z oznaczeniem „Dostępne”, PW-15. Wymagania: US5.1, US2.1 */
    @Test
    fun drawsLightStep2() {
        capture(isFirstStep = false)
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkStep2() {
        capture(isFirstStep = false)
    }

    @Test
    fun drawsStep2AtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(isFirstStep = false)
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp-xhdpi")
    fun drawsWideContent() {
        capture(isFirstStep = true)
    }

    @Test
    @Config(qualifiers = "w840dp-h800dp-xhdpi", fontScale = 2f)
    fun drawsWideContentAtFont200() {
        capture(isFirstStep = true)
    }
}
