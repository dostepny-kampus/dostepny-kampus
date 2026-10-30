/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.app.navigation.openBuilding
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.map.navigation.MapRoute
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
    private val visitor = RoleProvider { Account.SignedIn() }
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
        compose.waitForIdle()
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
        val ys = listOf("Mapa", "Konto i\u00A0praca").map { compose.onNodeWithText(it).getUnclippedBoundsInRoot().top }
        assertEquals(1, ys.distinct().size) // Decyzja 69, NFR-51: okno < 600 dp → pozycje w jednym rzędzie.
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

    // Wiersz: V2, „Wstecz” → V1 w poprzednim stanie, US1.1, decyzja 83; ponowne „Mapa” → świeże V1, decyzja 72.
    @Test
    fun us1_1_v2_back_keeps_v1_state() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                if (route is MapRoute) {
                    Column {
                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        Text(text = "v=$taps", modifier = Modifier.clickable { taps++ })
                        Text(text = "A-1", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
                    }
                }
            }
        }
        compose.onNodeWithText("v=0").performClick()
        compose.onNodeWithText("A-1").performClick()
        pressBack()
        compose.waitUntil { compose.onAllNodesWithText("v=1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("A-1").performClick()
        compose.onNodeWithText("Mapa").performClick()
        compose.onNodeWithText("v=0").assertExists()
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

    // Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie w 3 krokach z pozycją dołączoną
    //   automatycznie
    @Test
    fun us4_1_report_stubs_steps_sent() {
        show(roles = visitor)
        compose.onNodeWithText("Zgłoś barierę").performClick()
        listOf("Dalej", "Dalej", "Wyślij").forEachIndexed { index, action ->
            compose.onNodeWithText("Krok ${index + 1} z 3").assertExists()
            compose.onNodeWithText(action).performClick()
        }
        compose.onNodeWithText("Wróć do mapy").assertExists()
        pressBack()
        compose.onNodeWithText("Mapa").assertIsSelected()
    }

    // Wiersz: V2 → „Sprawdź, którędy wejść” → V3, „Wstecz” → V2 → V1 z zaznaczonym budynkiem; historia z parametrami
    // tras przetrwa odtworzenie aktywności, US3, US1.1, NFR-22.
    @Test
    fun nfr22_params_survive_restore() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                if (route is MapRoute) {
                    Text(route.focus?.toString() ?: "V1", Modifier.clickable { openBuilding(symbol = "A-1") })
                } else {
                    AppScreen(route)
                }
            }
        }
        compose.onNodeWithText("V1").performClick()
        compose.onNodeWithText("Sprawdź, którędy wejść").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Którędy wejść do A-1").assertExists()
        pressBack()
        compose.onNodeWithText("Pokaż na mapie").assertExists()
        pressBack()
        compose.onNodeWithText("Building(symbol=A-1)").assertExists()
    }

    // Wiersz: wymagania wspólne, pasek dolny na V1, bez niego na L1: treść nie wchodzi pod paski, NFR-52.
    @Test
    fun nfr52_content_insets_after_bar() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) {
                var consumed by remember { mutableStateOf(WindowInsets(0)) }
                val rest = WindowInsets.safeDrawing.exclude(consumed)
                val text = "${rest.getTop(LocalDensity.current)}:${rest.getBottom(LocalDensity.current)}"
                Text(text, Modifier.onConsumedWindowInsetsChanged { insets -> consumed = insets })
            }
        }
        val bars = WindowInsetsCompat.Builder()
        bars.setInsets(Type.statusBars(), Insets.of(0, 24, 0, 0))
        bars.setInsets(Type.navigationBars(), Insets.of(0, 0, 0, 40))
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        compose.runOnUiThread { ViewCompat.dispatchApplyWindowInsets(view, bars.build()) }
        compose.onNodeWithText("24:0").assertExists()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("24:40").assertExists()
    }
}
