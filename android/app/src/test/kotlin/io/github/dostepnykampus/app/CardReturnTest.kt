/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.returnToCard
import io.github.dostepnykampus.app.navigation.sheetCard
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SCROLLED_PX = 300
private const val CARD = "Karta"
private const val BACK = "Wstecz z arkusza"
private const val BUILDING = "A-1"

/**
 * Karta budynku, którą zastąpił arkusz znaku, wraca na tę samą wysokość i z tym samym przewinięciem; dotknięcie
 * konturu otwiera kartę od nowa, na niskiej wysokości.
 *
 * Wymagania: US2.8, US3.3, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardReturnTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navigator: AppNavigator
    private var scroll: ScrollState? = null

    private fun open() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                navigator = this
                if (route is MapRoute) {
                    Column {
                        Text(text = BUILDING, modifier = Modifier.clickable { openBuilding(symbol = BUILDING) })
                        Text(text = BACK, modifier = Modifier.clickable { returnToCard(BUILDING) })
                    }
                } else {
                    val card = sheetCard()
                    scroll = card.scroll
                    Column(modifier = Modifier.verticalScroll(card.scroll)) {
                        Text(text = CARD, modifier = Modifier.requiredHeight(3000.dp))
                    }
                }
            }
        }
        compose.onNodeWithText(BUILDING).performClick()
        compose.waitForIdle()
    }

    private fun expand() {
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
    }

    private fun scrollCard() {
        compose.runOnIdle { checkNotNull(scroll).dispatchRawDelta(SCROLLED_PX.toFloat()) }
        compose.waitForIdle()
    }

    private fun replaceCardBySign() {
        compose.runOnIdle { navigator.back() }
        compose.waitForIdle()
    }

    private fun scrolled(): Int = compose.runOnIdle { checkNotNull(scroll).value }

    /**
     * Wiersz: karta rozwinięta i przewinięta, arkusz znaku w jej miejscu, „Wstecz” → karta rozwinięta, tak samo
     *   przewinięta.
     * Scenariusz (częściowo, przykład: rozwinie kartę A-1, przewinie ją i dotknie wiersza wejścia):
     *   przegladanie-mapy.feature: „Wstecz” w arkuszu wejścia wraca tam, skąd arkusz otwarto, a „Zamknij” zostawia
     *   mapę
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wiersz budynku w arkuszu otwartym z karty wraca do tej
     *   karty
     */
    @Test
    fun returnsToExpandedScrolledCard() {
        open()
        expand()
        scrollCard()
        replaceCardBySign()

        compose.onNodeWithText(BACK).performClick()
        compose.waitForIdle()

        compose.onNode(sheetHandle(isExpanded = true)).assertExists("karta rozwinięta po powrocie")
        assertEquals("przewinięcie po powrocie", SCROLLED_PX, scrolled())
    }

    /** Wiersz: karta niska, arkusz znaku w jej miejscu, „Wstecz” → karta niska. */
    @Test
    fun returnsToLowCard() {
        open()
        replaceCardBySign()

        compose.onNodeWithText(BACK).performClick()
        compose.waitForIdle()

        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta niska po powrocie")
    }

    /** Wiersz: po karcie rozwiniętej i przewiniętej dotknięcie konturu otwiera kartę od nowa, niską i od góry. */
    @Test
    fun opensFreshCardOnContour() {
        open()
        expand()
        scrollCard()
        replaceCardBySign()

        compose.onNodeWithText(BUILDING).performClick()
        compose.waitForIdle()

        compose.onNode(sheetHandle(isExpanded = false)).assertExists("karta niska po dotknięciu konturu")
        assertEquals("karta od góry", 0, scrolled())
    }
}
