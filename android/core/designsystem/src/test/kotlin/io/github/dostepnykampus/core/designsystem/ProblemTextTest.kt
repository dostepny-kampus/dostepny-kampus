/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Komunikat błędu: znak „!” w kole i tekst ogłaszany przez TalkBack. Wymagania: NFR-18, NFR-21 */
@RunWith(RobolectricTestRunner::class)
class ProblemTextTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun signsAndAnnouncesProblem() {
        compose.setContent { DostepnyKampusTheme { ProblemText(text = "Wybierz rolę.") } }
        compose.onNodeWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).assertIsDisplayed()
        compose
            .onNodeWithText("Wybierz rolę.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
    }
}
