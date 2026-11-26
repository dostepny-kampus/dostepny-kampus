/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CARD = "karta"
private const val HEADER = "Wejścia"
private const val NO_ENTRANCES = "Nie mamy jeszcze danych o wejściach do tego budynku."
private val TOUCH_TARGET = 48.dp

private val peek = hasContentDescription(HEADER) and hasClickAction()

/**
 * Niska karta V2: pod treścią wystaje cały nagłówek „Wejścia” i ucięty następny wiersz pełnego opisu, tak samo
 * przy wejściu dostępnym, bez wejścia dostępnego i bez danych o wejściach; dotknięcie nagłówka rozwija kartę.
 *
 * Wymagania: US2.8, NFR-17, NFR-18, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntrancesPeekTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val mode = mutableStateOf(CardMode.Compact)
    private val card = mutableStateOf<BuildingCardUiState>(ready())
    private var expanded = 0

    /**
     * Wiersz: V2, niska wysokość: nagłówek „Wejścia” wystaje na dole karty we wszystkich trzech stanach wiersza
     *   o wejściach, a jego cel dotyku ma co najmniej 48 dp.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     */
    @Test
    fun showsHeaderAtCardBottom() {
        open()
        val heights =
            listOf(
                ready(entrance(EntranceMarking.ACCESSIBLE)),
                ready(entrance(EntranceMarking.INACCESSIBLE)),
                ready(entrance(EntranceMarking.NO_DATA)),
                ready(),
            ).map { state ->
                card.value = state
                compose.waitForIdle()
                val bounds = compose.onNode(peek).getUnclippedBoundsInRoot()
                val cardBottom = compose.onNodeWithTag(CARD).getUnclippedBoundsInRoot().bottom
                assertEquals("„Wejścia” na dole karty: $state", cardBottom.value, bounds.bottom.value, 1f)
                (bounds.bottom - bounds.top).also { height ->
                    assertTrue("cel dotyku „Wejścia” 48 dp, a jest $height", height >= TOUCH_TARGET)
                }
            }
        assertTrue("ta sama wysokość wystającej części we wszystkich stanach: $heights", heights.distinct().size == 1)
    }

    /**
     * Wiersz: V2, niska wysokość: widać cały nagłówek „Wejścia”, a następny wiersz grupy jest ucięty.
     * Scenariusz (częściowo, przykład: A-2): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     */
    @Test
    fun cutsNextLine() {
        open()
        val shown = compose.onNode(peek).getUnclippedBoundsInRoot().run { bottom - top }
        mode.value = CardMode.Expanded
        compose.waitForIdle()
        val heading = compose.onNodeWithText(HEADER).getUnclippedBoundsInRoot()
        val top = heading.top - Odstepy.l
        val line = compose.onNodeWithText(NO_ENTRANCES).getUnclippedBoundsInRoot()
        assertBetween(what = "cały nagłówek", shown = shown, above = heading.bottom - top, below = line.bottom - top)
        assertTrue("część następnego wiersza widoczna: $shown, wiersz od ${line.top - top}", shown > line.top - top)
    }

    /**
     * Wiersz: V2, dotknięcie wystającego nagłówka „Wejścia” podnosi kartę na połowę ekranu; TalkBack czyta go jako
     *   przycisk.
     * Scenariusz (częściowo, przykład: A-1): przegladanie-mapy.feature: Niska karta nad mapą mówi słowami o wejściach
     */
    @Test
    fun expandsOnHeaderTap() {
        open()
        compose.onNode(peek).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        compose.onNode(peek).performClick()
        assertEquals("jedno rozwinięcie po dotknięciu", 1, expanded)
    }

    /** Wiersz: V2, pełny opis na połowie ekranu i wyżej nie ma wystającego nagłówka, tylko grupę „Wejścia”. */
    @Test
    fun hidesPeekWhenExpanded() {
        open()
        mode.value = CardMode.Expanded
        compose.waitForIdle()
        compose.onNode(peek).assertDoesNotExist()
    }

    private fun assertBetween(what: String, shown: Dp, above: Dp, below: Dp) {
        assertTrue("$what: wystaje $shown, a powinno więcej niż $above", shown >= above)
        assertTrue("$what: wiersz ucięty, wystaje $shown z $below", shown < below)
    }

    private fun open() {
        compose.setContent {
            DostepnyKampusTheme {
                Box(Modifier.testTag(CARD)) {
                    BuildingCardContent(
                        symbol = "A-2",
                        mode = mode.value,
                        card = card.value,
                        isOnline = true,
                        onShowOnMap = {},
                        onBack = {},
                        onClose = {},
                        onReport = {},
                        onRetry = {},
                        onExpand = { expanded++ },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun ready(vararg entrances: Entrance): BuildingCardUiState =
        BuildingCardUiState.Ready(symbol = "A-2", name = "Budynek", entrances = orderedEntrances(entrances.toList()))

    private fun entrance(marking: EntranceMarking): Entrance =
        Entrance(
            id = "node/1",
            buildingSymbol = "A-2",
            name = "Główne",
            isMain = true,
            type = "main",
            location = GeoPoint(longitude = 0.5, latitude = 0.5),
            marking = marking,
        )
}
