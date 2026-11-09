/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SHOW_ON_MAP = "Pokaż budynek na mapie"

/** V2 otwarte od razu na pełnym ekranie: duża czcionka, szerokie okno albo poziomo (US2.9, NFR-20, NFR-51). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetDirectTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
            if (route is MapRoute) {
                Text(text = "V1", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
            } else if (route is BuildingRoute) {
                TestBuildingCard(route.symbol)
            }
        }
    }

    private fun show() {
        compose.setContent { Shell() }
        compose.onNodeWithText("V1").performClick()
    }

    private fun checkDirect(moment: String) {
        compose.onNodeWithText(SHOW_ON_MAP).assertExists("„$SHOW_ON_MAP” na pełnym ekranie od razu: $moment")
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        compose.onNodeWithText("V1").assertDoesNotExist()
    }

    private fun checkCompact(moment: String) {
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("mała karta nad mapą: $moment")
        compose.onNodeWithText("V1").assertExists("mapa V1 nad małą kartą: $moment")
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
    }

    /**
     * Wiersz: V2 przy skali czcionki co najmniej 1,5 → od razu pełny ekran bez mapy i bez małej karty,
     * z „Pokaż budynek na mapie”, NFR-20, reguła 5, US2.9.
     */
    @Test
    @Config(fontScale = 2f)
    fun nfr20_large_font_v2_direct() {
        show()
        checkDirect("czcionka 200 %")
    }

    /**
     * Wiersz: V2, pełny ekran otwarty od razu, „Wstecz” w górnym pasku albo telefonu → ekran sprzed V2
     * jednym krokiem, NFR-15, US2.9.
     */
    @Test
    @Config(fontScale = 2f)
    fun nfr15_direct_back_one_step() {
        show()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("V1").assertExists("V1 po jednym „Wstecz” w górnym pasku")
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        compose.onNodeWithText("V1").performClick()
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("V1").assertExists("V1 po jednym „Wstecz” telefonu")
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
    }

    /**
     * Wiersz: V2 otwarte, obrót do poziomu albo zmiana czcionki w trakcie → tryb z bieżącego okna: poziomo
     * i przy czcionce 200 % pełny ekran od razu, pionowo przy zwykłej czcionce mała karta, NFR-51, NFR-20, NFR-22.
     */
    @Test
    fun nfr22_mode_follows_window() {
        var size by mutableStateOf(DpSize(width = 360.dp, height = 800.dp))
        var fontScale by mutableFloatStateOf(1f)
        compose.setContent {
            val font = DeviceConfigurationOverride.FontScale(fontScale)
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(size) then font) { Shell() }
        }
        compose.onNodeWithText("V1").performClick()
        checkCompact("pionowo")
        size = DpSize(width = 800.dp, height = 360.dp)
        checkDirect("po obrocie do poziomu")
        size = DpSize(width = 360.dp, height = 800.dp)
        checkCompact("po obrocie z powrotem")
        fontScale = 2f
        checkDirect("po zmianie czcionki na 200 %")
    }
}
