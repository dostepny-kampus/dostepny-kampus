/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Oboczka w Robolectric: historia, pasek dolny w wąskim oknie, pasek z boku w szerokim i role. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val staff = RoleProvider { Account.SignedIn(setOf(StaffRole.COLLECTOR)) }
    private val tabRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private var helpLeft = 0

    private fun show(firstLaunch: Boolean = false, roles: RoleProvider = GuestRoleProvider) {
        compose.setContent { Shell(firstLaunch = firstLaunch, roles = roles) }
    }

    @Composable
    private fun Shell(firstLaunch: Boolean, roles: RoleProvider) {
        DostepnyKampusTheme { AppShell(firstLaunch = firstLaunch, roles = roles, onFirstHelpLeft = { helpLeft++ }) }
    }

    private fun checkSideBar(windowHeight: Dp = 360.dp) {
        val heights = mutableMapOf<String, Dp>()
        listOf("Mapa", "Zgłoś barierę", "Pomoc", "Konto").forEach { label ->
            val node = compose.onNodeWithText(label).performScrollTo().assert(tabRole)
            val item = node.getUnclippedBoundsInRoot()
            val text = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals(listOf(8.dp, 80.dp), listOf(item.left, item.width))
            assertTrue(text.left >= item.left && text.right <= item.right && item.bottom <= windowHeight)
            heights[label] = text.height
        }
        assertTrue(heights.getValue("Zgłoś barierę") > heights.getValue("Pomoc") * 1.5f)
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_first_launch_v5_back_v1() {
        show(firstLaunch = true)
        compose.onNodeWithText("Pomoc").assertIsSelected()
        pressBack()
        compose.onNodeWithText("Mapa").assertIsSelected()
        assertEquals(1, helpLeft)
    }

    // Wiersz: pasek dolny, „Zgłoś barierę” → L1 bez paska dolnego, RO-GOS, US14.2.
    @Test
    fun us14_2_guest_report_l1_no_bar() {
        show()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onAllNodesWithText("Mapa").assertCountEquals(0)
        pressBack()
        compose.onNodeWithText("Mapa").assertIsSelected()
    }

    // Wiersz: pasek dolny, „Konto i praca” → L2, role pracownicze, US14.1.
    @Test
    fun us14_1_staff_konto_i_praca_l2() {
        show(roles = staff)
        compose.onNodeWithText("Konto i\u00A0praca").assertIsNotSelected().performClick()
        compose.onNodeWithText("Konto i\u00A0praca").assertIsSelected()
    }

    // Wymagania wspólne i reguła 8, decyzje 69 i 77: okno ≥ 600 dp → pasek z boku, karty od góry, NFR-18, NFR-51.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun nfr51_side_bar_font_100() {
        show()
        checkSideBar()
    }

    // Reguła 8, decyzje 69 i 77: pasek z boku przy czcionce 200 % ma te same zasady napisów i się przewija, NFR-20.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w640dp-h360dp-xhdpi", fontScale = 2f)
    fun nfr20_side_bar_font_200() {
        show()
        compose.onRoot().captureRoboImage()
        checkSideBar()
    }

    // Reguła 8, decyzja 77: gdy cztery pozycje nie mieszczą się w wysokości okna, pasek z boku się przewija.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w640dp-h240dp", fontScale = 2f)
    fun nfr20_side_bar_scrolls() {
        show()
        assertTrue(compose.onNodeWithText("Konto").getUnclippedBoundsInRoot().bottom > 240.dp)
        checkSideBar(windowHeight = 240.dp)
    }

    // Wiersz: każdy ekran poza V1, „Wstecz”: historia przetrwa odtworzenie aktywności, NFR-15, NFR-22.
    @Test
    fun nfr22_history_survives_restore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Shell(firstLaunch = false, roles = GuestRoleProvider) }
        compose.onNodeWithText("Pomoc").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Pomoc").assertIsSelected()
        pressBack()
        compose.onNodeWithText("Mapa").assertIsSelected()
    }
}
