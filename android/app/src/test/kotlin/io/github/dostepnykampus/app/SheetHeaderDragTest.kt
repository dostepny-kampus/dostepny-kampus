/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.github.dostepnykampus.app.navigation.BuildingCard
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.building.BuildingCardUiState
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NAME = "Gmach Główny"
private const val ENTRANCE_COUNT = 20
private const val FIRST = "Wejście 1"
private const val LAST = "Wejście $ENTRANCE_COUNT"
private const val SWIPE_MILLIS = 600L
private val DRAG = 160.dp

/**
 * Arkusz V2 na całym ekranie z przewiniętą treścią: przeciągnięcie w dół po nagłówku karty opuszcza kartę na niską
 * wysokość, tak jak przeciągnięcie uchwytu, a mała karta po rozwinięciu znowu zaczyna treść od początku.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetHeaderDragTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, cały ekran, treść przewinięta: przeciągnięcie nagłówka w dół → niska wysokość.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun lowersOnHeaderDrag() {
        openScrolledFull()
        dragDown(compose.onNodeWithText(NAME))
        compose.onNode(sheetHandle(isExpanded = false)).assertExists("niska po przeciągnięciu nagłówka")
    }

    /**
     * Wiersz: V2, cały ekran, treść przewinięta: przeciągnięcie nagłówka w dół i rozwinięcie → początek treści.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun showsStartAfterHeaderDrag() {
        val start = openScrolledFull()
        dragDown(compose.onNodeWithText(NAME))
        expandAgain()
        assertEquals("początek treści pod nagłówkiem po rozwinięciu", start.value, firstBelowHeader().value, 1f)
    }

    /**
     * Wiersz: V2, cały ekran, treść przewinięta: przeciągnięcie uchwytu w dół i rozwinięcie → początek treści.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun showsStartAfterHandleDrag() {
        val start = openScrolledFull()
        dragDown(compose.onNodeWithContentDescription(EXPAND_HANDLE))
        expandAgain()
        assertEquals("początek treści pod nagłówkiem po rozwinięciu", start.value, firstBelowHeader().value, 1f)
    }

    private fun expandAgain() {
        compose.onNode(sheetHandle(isExpanded = false)).performClick()
        compose.waitForIdle()
        assertEquals("cały ekran po rozwinięciu", 0f, handleTop().value, 1f)
    }

    private fun openScrolledFull(): Dp {
        compose.openCardOverMap { BuildingCard(symbol = "A-1", state = ready(), isOnline = true, onRetry = {}) }
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitForIdle()
        assertEquals("cały ekran", 0f, handleTop().value, 1f)
        val start = firstBelowHeader()
        compose.onNodeWithText(LAST).performScrollTo()
        return start
    }

    private fun dragDown(node: SemanticsNodeInteraction) {
        node.performTouchInput {
            swipe(start = center, end = center + Offset(x = 0f, y = DRAG.toPx()), durationMillis = SWIPE_MILLIS)
        }
        compose.waitForIdle()
    }

    private fun firstBelowHeader(): Dp =
        compose.onNodeWithText(FIRST).getUnclippedBoundsInRoot().top -
            compose.onNodeWithText(NAME).getUnclippedBoundsInRoot().top

    private fun map(): Dp = compose.onNodeWithTag(MAP_TAG).getUnclippedBoundsInRoot().height

    private fun handleTop(): Dp = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().top

    private fun ready(): BuildingCardUiState =
        BuildingCardUiState.Ready(
            symbol = "A-1",
            name = NAME,
            entrances =
                orderedEntrances(
                    (1..ENTRANCE_COUNT).map { number ->
                        Entrance(
                            id = "node/$number",
                            buildingSymbol = "A-1",
                            name = "Wejście $number",
                            isMain = false,
                            type = null,
                            location = GeoPoint(longitude = 0.5, latitude = 0.5),
                            marking = EntranceMarking.INACCESSIBLE,
                        )
                    },
                ),
        )
}
