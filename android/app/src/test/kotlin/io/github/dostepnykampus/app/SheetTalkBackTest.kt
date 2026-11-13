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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHiddenFromAccessibility
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val Button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

/** Arkusz V2 nad V1 z TalkBack: uchwyt, fokus po otwarciu i mapa pod małą kartą. Wymagania: US2.8, NFR-18, NFR-19 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetTalkBackTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Shell(isCard: Boolean) {
        AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
            if (route is MapRoute) {
                Text(text = "mapa", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
            } else if (route is BuildingRoute) {
                if (isCard) TestBuildingCard(route.symbol) else Text(text = "karta")
            }
        }
    }

    private fun open(isCard: Boolean = false) {
        compose.setContent { Shell(isCard) }
        compose.onNodeWithText("mapa").performClick()
        compose.waitForIdle()
    }

    private fun readable(matcher: SemanticsMatcher): SemanticsMatcher =
        matcher and !isHiddenFromAccessibility() and !hasAnyAncestor(isHiddenFromAccessibility())

    /**
     * Wiersz: uchwyt małej karty → przycisk „Rozwiń opis budynku” ze stanem „zwinięte”, a po rozwinięciu ten sam
     * przycisk ze stanem „rozwinięte”.
     *
     * Wymagania: US2.8, NFR-18
     */
    @Test
    fun nfr18_handle_button_with_state() {
        open()
        compose.onNode(sheetHandle(isExpanded = false) and Button).performClick()
        compose.onNode(sheetHandle(isExpanded = true) and Button).assertExists("uchwyt pełnego ekranu")
    }

    /** Wiersz: V1, dotknięcie konturu → mała karta, fokus TalkBack na nagłówku „Budynek A-1, …”. Wymagania: NFR-18 */
    @Test
    fun nfr18_focus_on_header() {
        open(isCard = true)
        compose.onNode(hasContentDescription("Budynek A-1, ", substring = true)).assertIsFocused()
    }

    /**
     * Wiersz: mapa nad małą kartą → TalkBack jej nie czyta, a uchwyt karty czyta; po zamknięciu karty mapa znów
     * czytana.
     *
     * Wymagania: NFR-19
     */
    @Test
    fun nfr19_map_under_card_silent() {
        compose.setContent { Shell(isCard = false) }
        compose.onAllNodes(readable(hasText("mapa"))).assertCountEquals(1)
        compose.onNodeWithText("mapa").performClick()
        compose.onAllNodes(readable(hasText("mapa"))).assertCountEquals(0)
        compose.onAllNodes(readable(sheetHandle(isExpanded = false))).assertCountEquals(1)
        compose.onAllNodes(readable(hasText("karta"))).assertCountEquals(1)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onAllNodes(readable(hasText("mapa"))).assertCountEquals(1)
    }
}
