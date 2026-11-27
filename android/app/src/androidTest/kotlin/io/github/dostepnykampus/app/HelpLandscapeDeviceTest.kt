/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val CONFIG_TIMEOUT_MS = 15_000L

/** Pomoc w poziomie omija rzeczywiste krawędzie systemu. Wymagania: NFR-20, NFR-22 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HelpLandscapeDeviceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun keepsHelpOutsideSystemBars() {
        checkHelpInLandscape(fontScale = 1f)
    }

    @Test
    fun keepsHelpClearAtFont200() {
        checkHelpInLandscape(fontScale = 2f)
    }

    private fun checkHelpInLandscape(fontScale: Float) {
        val previous = DeviceEvidence.shell("settings get system font_scale").trim().toFloatOrNull()
        try {
            DeviceEvidence.shell("settings put system font_scale $fontScale")
            compose.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            compose.waitUntil(CONFIG_TIMEOUT_MS) {
                val config = compose.activity.resources.configuration
                config.orientation == Configuration.ORIENTATION_LANDSCAPE && config.fontScale == fontScale
            }
            compose.waitUntil(CONFIG_TIMEOUT_MS) {
                compose.onAllNodesWithText("Pomiń instrukcję").fetchSemanticsNodes().isNotEmpty()
            }
            val skip = compose.onNodeWithText("Pomiń instrukcję")
            if (fontScale > 1f) skip.performScrollTo()
            skip.assertIsDisplayed()
            assertInsideSystemBars()
            DeviceEvidence.record("pomoc-poziom-$fontScale")
        } finally {
            if (previous == null) {
                DeviceEvidence.shell("settings delete system font_scale")
            } else {
                DeviceEvidence.shell("settings put system font_scale $previous")
            }
        }
    }

    private fun assertInsideSystemBars() {
        val type = Type.statusBars() or Type.navigationBars() or Type.displayCutout() or Type.mandatorySystemGestures()
        val insets =
            compose.runOnUiThread {
                requireNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView)).getInsets(type)
            }
        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        val map = compose.onNode(hasText("Mapa") and tab).getUnclippedBoundsInRoot()
        val skip = compose.onNodeWithText("Pomiń instrukcję").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        with(compose.density) {
            assertTrue("Mapa jest pod paskiem stanu", map.top >= root.top + insets.top.toDp())
            assertTrue("Mapa omija lewą krawędź systemu", map.left >= root.left + insets.left.toDp())
            assertTrue(
                "Pomiń instrukcję omija dolną krawędź systemu",
                skip.bottom <= root.bottom - insets.bottom.toDp(),
            )
        }
    }
}
