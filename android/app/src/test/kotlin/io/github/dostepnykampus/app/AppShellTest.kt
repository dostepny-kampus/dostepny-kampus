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
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Oboczka z paskiem dolnym w wąskim oknie (Robolectric): historia, pasek na właściwych ekranach i role. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val staff = RoleProvider { Account.SignedIn(setOf(StaffRole.COLLECTOR)) }
    private var helpLeft = 0

    private fun show(firstLaunch: Boolean = false, roles: RoleProvider = GuestRoleProvider) {
        compose.setContent { Shell(firstLaunch = firstLaunch, roles = roles) }
    }

    @Composable
    private fun Shell(firstLaunch: Boolean, roles: RoleProvider) {
        DostepnyKampusTheme { AppShell(firstLaunch = firstLaunch, roles = roles, onFirstHelpLeft = { helpLeft++ }) }
    }

    private fun tabTops(): List<Float> =
        listOf("Mapa", "Zgłoś barierę", "Pomoc", "Konto").map { label ->
            compose.onNodeWithText(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            val bounds = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
            bounds.top.value
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

    // Wymagania wspólne, decyzja 69: w oknie węższym niż 600 dp pasek dolny, pozycje w rzędzie jako karty, NFR-18.
    @Test
    fun nfr18_narrow_window_bottom_bar() {
        show()
        assertEquals(1, tabTops().distinct().size)
    }

    // Wymagania wspólne, decyzja 69: w oknie co najmniej 600 dp pasek z boku, te same karty od góry, NFR-51.
    @Test
    @Config(qualifiers = "w640dp-h360dp")
    fun nfr51_wide_window_side_bar() {
        show()
        assertEquals(tabTops().sorted(), tabTops())
        assertEquals(4, tabTops().distinct().size)
        compose.onNodeWithText("Mapa").assertIsSelected()
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
