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
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Zrzuty V5 z paskiem dolnym w stanach makiet hi-fi: krok 1 przy pierwszym uruchomieniu, krok 2 z „Pomoc”. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class HelpScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture(isFirstStep: Boolean) {
        val screenData = compose.activity.appScreenData()
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = isFirstStep, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                    if (route !is MapRoute) AppScreen(route, screenData)
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

    // Krok 1 przy pierwszym uruchomieniu: pole wyszukiwania V1 jako obraz, US5.1, NFR-18.
    @Test
    fun us5_1_v5_krok1_jasny() {
        capture(isFirstStep = true)
    }

    @Test
    fun nfr16_v5_krok1_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        capture(isFirstStep = true)
    }

    // Czcionka 200 %: „Dalej” i „Pomiń instrukcję” na końcu treści (reguła 10), NFR-20.
    @Test
    fun nfr20_v5_krok1_200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(isFirstStep = true)
    }

    // Krok 2 z paska „Pomoc”: wejście z oznaczeniem „Dostępne”, US5.1, US2.1, PW-15.
    @Test
    fun us5_1_v5_krok2_jasny() {
        capture(isFirstStep = false)
    }

    @Test
    fun nfr16_v5_krok2_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        capture(isFirstStep = false)
    }

    @Test
    fun nfr20_v5_krok2_200() {
        RuntimeEnvironment.setFontScale(2f)
        capture(isFirstStep = false)
    }
}
