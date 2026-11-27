/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.bar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Układ ekranu.
 *
 * Wymagania: NFR-52
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w840dp-h800dp")
class AdaptiveScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() {
        compose.setContent {
            AdaptiveScreen(
                header = { Box(Modifier.fillMaxWidth().height(64.dp).testTag("header")) },
                actions = { Box(Modifier.fillMaxWidth().height(96.dp).testTag("actions")) },
            ) {
                Box(Modifier.fillMaxWidth().height(1000.dp))
                Box(Modifier.height(48.dp).testTag("last"))
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun keepsControlsFixedWithinBudget() {
        show()
        val before = compose.onNodeWithTag("header").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("last").performScrollTo()
        assertEquals(
            "Header stays fixed within the height budget",
            before,
            compose.onNodeWithTag("header").getUnclippedBoundsInRoot(),
        )
    }

    @Test
    @Config(qualifiers = "w840dp-h360dp")
    fun scrollsControlsInShortWindow() {
        show()
        compose.onNodeWithTag("actions").performScrollTo()
        compose.onNodeWithTag("header").assertIsNotDisplayed()
        compose.onNodeWithTag("header").performScrollTo()
        assertEquals(
            "Header returns when scrolling to the start",
            0f,
            compose.onNodeWithTag("header").getUnclippedBoundsInRoot().top.value,
            1f,
        )
    }

    @Test
    @Config(fontScale = 2f)
    fun scrollsControlsAtLargeFont() {
        show()
        compose.onNodeWithTag("actions").performScrollTo()
        compose.onNodeWithTag("header").assertIsNotDisplayed()
    }
}
