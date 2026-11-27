/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val STATUS_BAR_PX = 48
private const val GESTURE_BAR_PX = 48
private const val CUTOUT_PX = 40

/** Wcięcia systemowe na ekranie V5 w poziomie. Wymagania: NFR-20, NFR-22 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h360dp-land-xhdpi", sdk = [36])
class HelpLandscapeScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsHelpOutsideSystemInsets() {
        showHelp()
        checkBounds()
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(fontScale = 2f)
    fun drawsScrolledHelpOutsideSystemInsetsAtFont200() {
        showHelp()
        compose.onNodeWithText("Pomiń instrukcję").performScrollTo()
        checkBounds()
        compose.onRoot().captureRoboImage()
    }

    private fun showHelp() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = true, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                    if (route is HelpRoute) AppScreen(route)
                }
            }
        }
        val bars =
            WindowInsetsCompat.Builder()
                .setInsets(Type.statusBars(), Insets.of(0, STATUS_BAR_PX, 0, 0))
                .setInsets(Type.displayCutout(), Insets.of(CUTOUT_PX, 0, 0, 0))
                .setInsets(Type.mandatorySystemGestures(), Insets.of(0, 0, 0, GESTURE_BAR_PX))
                .build()
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        compose.runOnUiThread { ViewCompat.dispatchApplyWindowInsets(view, bars) }
    }

    private fun checkBounds() {
        val map = compose.onNode(MapTab).assertIsDisplayed().getUnclippedBoundsInRoot()
        val skip = compose.onNodeWithText("Pomiń instrukcję").assertIsDisplayed().getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val status = with(compose.density) { STATUS_BAR_PX.toDp() }
        val cutout = with(compose.density) { CUTOUT_PX.toDp() }
        val gestures = with(compose.density) { GESTURE_BAR_PX.toDp() }
        assertTrue("Mapa jest poniżej paska stanu", map.top >= root.top + status)
        assertTrue("Mapa jest obok wycięcia ekranu", map.left >= root.left + cutout)
        assertTrue("Ostatnia czynność jest powyżej paska gestów", skip.bottom <= root.bottom - gestures + 1.dp)
    }
}
