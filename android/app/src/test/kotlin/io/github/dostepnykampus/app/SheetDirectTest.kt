/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SHOW_ON_MAP = "Pokaż budynek na mapie"

/**
 * V2 od razu na pełnym ekranie: szerokie okno albo poziomo bez mapy, duża czcionka na cały ekran nad mapą.
 *
 * Wymagania: US2.9, NFR-20, NFR-51
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetDirectTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
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

    private fun checkFullSheet(moment: String) {
        compose.onNodeWithText(SHOW_ON_MAP).assertExists("„$SHOW_ON_MAP” na karcie na cały ekran: $moment")
        compose.onNodeWithText("Wstecz").assertDoesNotExist()
        compose.onNodeWithText("Zamknij").assertExists("„Zamknij” w nagłówku karty na cały ekran: $moment")
        val handle = compose.onNode(hasContentDescription(EXPAND_HANDLE) and hasStateDescription("cały ekran"))
        handle.assertExists("uchwyt karty na cały ekran: $moment")
        val actions = handle.fetchSemanticsNode().config.getOrElse(SemanticsActions.CustomActions) { emptyList() }
        val labels = actions.map { it.label }
        assertEquals("bez „Rozwiń” i „Zwiń” przy dużej czcionce: $moment", emptyList<String>(), labels)
        assertEquals("karta od góry okna: $moment", 0f, handle.getUnclippedBoundsInRoot().top.value, 1f)
    }

    private fun checkCompact(moment: String) {
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("mała karta nad mapą: $moment")
        compose.onNodeWithText("V1").assertExists("mapa V1 nad małą kartą: $moment")
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
    }

    /**
     * Wiersz: V2 przy skali czcionki co najmniej 1,5 → karta od razu na cały ekran nad mapą, z „Pokaż budynek na
     * mapie”, z dwiema wysokościami: cały ekran i zamknięta; uchwyt bez „Rozwiń” i „Zwiń”.
     *
     * Wymagania: US2.9, NFR-20
     */
    @Test
    @Config(fontScale = 2f)
    fun opensFullSheetAtLargeFont() {
        show()
        checkFullSheet("czcionka 200 %")
    }

    /**
     * Wiersz: V2 przy skali czcionki co najmniej 1,5, przeciągnięcie karty w dół → karta zamyka się od razu, bez
     * niższej wysokości po drodze.
     *
     * Wymagania: US2.9, NFR-20
     */
    @Test
    @Config(fontScale = 2f)
    fun closesOnDragDownAtLargeFont() {
        show()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithText(SHOW_ON_MAP).assertDoesNotExist()
        compose.onNodeWithContentDescription(EXPAND_HANDLE).assertDoesNotExist()
        compose.onNodeWithText("V1").assertExists("mapa V1 po zamknięciu")
    }

    /**
     * Wiersz: V2, pełny ekran otwarty od razu poziomo, „Wstecz” w górnym pasku albo telefonu → ekran sprzed V2
     * jednym krokiem.
     *
     * Wymagania: NFR-15, NFR-51
     */
    @Test
    @Config(qualifiers = "w800dp-h360dp")
    fun goesBackOneStepFromDirect() {
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
     * i przy czcionce 200 % pełny ekran od razu, pionowo przy zwykłej czcionce mała karta.
     *
     * Wymagania: NFR-51, NFR-20, NFR-22
     */
    @Test
    fun followsWindowMode() {
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
        checkFullSheet("po zmianie czcionki na 200 %")
    }
}
