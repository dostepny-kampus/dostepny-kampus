/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pasek kroków V4: 3 odcinki, wykonane wypełnione, a TalkBack czyta „Krok N z 3” z postępem (US4.1, NFR-18). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickStepProgressTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: V4 „Krok N z 3” i pod nim pasek z 3 odcinków; wykonane odcinki są wypełnione, NFR-21. */
    @Test
    fun us4_1_progress_segments() {
        var step by mutableIntStateOf(1)
        compose.setContent { DostepnyKampusTheme { QuickStepProgress(step = step) } }
        (1..QUICK_STEPS).forEach { current ->
            step = current
            compose.onAllNodesWithTag(STEP_DONE_TAG, useUnmergedTree = true).assertCountEquals(current)
            compose.onAllNodesWithTag(STEP_AHEAD_TAG, useUnmergedTree = true).assertCountEquals(QUICK_STEPS - current)
        }
    }

    /** Wiersz: V4 pasek kroków ma dla TalkBack opis „Krok N z 3” i postęp, nie sam kolor, NFR-18. */
    @Test
    fun nfr18_progress_semantics() {
        compose.setContent { DostepnyKampusTheme { QuickStepProgress(step = 2) } }
        val progress = ProgressBarRangeInfo(current = 2f, range = 0f..3f, steps = 2)
        compose
            .onNodeWithText("Krok 2 z 3")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, progress))
    }
}
