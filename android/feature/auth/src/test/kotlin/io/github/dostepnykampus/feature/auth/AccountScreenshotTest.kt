/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty L2 według makiety `m-l2-konto-praca`.
 *
 * Wymagania: US14.1, US14.3, NFR-52
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AccountScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun capture() {
        val account = Account.SignedIn(setOf(StaffRole.COLLECTOR, StaffRole.COORDINATOR), displayName = "Jan Kowalski")
        compose.setContent {
            DostepnyKampusTheme {
                AccountScreen(account = account, actions = AccountActions(onBack = {}, onWork = {}, onSignOut = {}))
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    /** Zbierający i Koordynator: karta osoby, „Twoja praca” i sekcja „Konto”. */
    @Test
    fun drawsLightWork() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkWork() {
        capture()
    }

    /** Czcionka 200 %. Wymagania: NFR-52 */
    @Test
    fun drawsWorkAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }
}
