/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** NFR-51: the same centered width for prose, fields and actions, independent of font size. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w840dp-h800dp")
class ReadableWidthTest {
    @get:Rule
    val compose = createComposeRule()

    private fun check(width: Float, left: Float) {
        compose.setContent {
            Box(Modifier.readableWidth()) { Box(Modifier.fillMaxSize().testTag("column")) }
        }
        val bounds = compose.onNodeWithTag("column").getUnclippedBoundsInRoot()
        assertEquals(width, bounds.width.value, 1f)
        assertEquals(left, bounds.left.value, 1f)
    }

    @Test
    fun centersWideContent() = check(width = 560f, left = 140f)

    @Test
    @Config(fontScale = 2f)
    fun keepsActionsAlignedAtFont200() = check(width = 560f, left = 140f)

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun keepsNarrowContentFullWidth() = check(width = 360f, left = 0f)

    @Test
    @Config(qualifiers = "w600dp-h800dp")
    fun constrainsAtWideBreakpoint() = check(width = 560f, left = 20f)
}
