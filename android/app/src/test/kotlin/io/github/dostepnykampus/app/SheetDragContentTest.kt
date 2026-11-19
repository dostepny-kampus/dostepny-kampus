/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.sheetCard
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.building.BuildingCardContent
import io.github.dostepnykampus.feature.map.building.BuildingCardUiState
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private const val SYMBOL = "A-1"
private const val MAIN = "Główne"
private const val SIDE = "Boczne"
private const val ENTRANCES = "Wejścia"
private const val STEPS = 10
private const val MOVE_MILLIS = 16L
private val Step = 20.dp
private val Assessed = EntranceAssessment(date = LocalDate.of(2026, 11, 1), source = AssessmentSource.PWR_PLANS)

/**
 * Mała karta budynku V2 ciągnięta za uchwyt w górę, zanim palec puści: pełny opis jest już pod krawędzią karty,
 * a nagłówek „Wejścia” stoi w tym samym miejscu karty co przed gestem.
 *
 * Wymagania: NFR-56, US2.8
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetDragContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: mała karta, palec w połowie gestu w górę — drugie wejście już jest, „Wejścia” nie skacze. */
    @Test
    fun showsWholeWhileDragging() {
        compose.openCardOverMap { Card() }
        val before = entrancesBelowHandle()
        val side = hasText(SIDE, substring = true)
        assertEquals("mała karta bez drugiego wejścia", 0, compose.onAllNodes(side).fetchSemanticsNodes().size)
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE)
        handle.performTouchInput {
            down(center)
            repeat(STEPS) { moveBy(delta = Offset(x = 0f, y = -Step.toPx()), delayMillis = MOVE_MILLIS) }
        }
        compose.waitForIdle()
        val during = entrancesBelowHandle()
        val sides = compose.onAllNodes(side).fetchSemanticsNodes().size
        handle.performTouchInput { up() }
        assertEquals("drugie wejście w połowie gestu", 1, sides)
        assertEquals("„Wejścia” względem góry karty: $before → $during", before.value, during.value, 1f)
    }

    @Composable
    private fun AppNavigator.Card() {
        val sheet = sheetCard()
        BuildingCardContent(
            symbol = SYMBOL,
            mode = sheet.mode,
            card = ready(),
            isOnline = true,
            onShowOnMap = {},
            onBack = sheet.onBack,
            onClose = sheet.onClose,
            onReport = {},
            onRetry = {},
            onExpand = sheet.onExpand,
            scroll = sheet.scroll,
        )
    }

    private fun entrancesBelowHandle(): Dp {
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE).getUnclippedBoundsInRoot().top
        val title = hasText(ENTRANCES)
        val top =
            if (compose.onAllNodes(title).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNode(title).getUnclippedBoundsInRoot().top
            } else {
                compose.onNode(hasContentDescription(ENTRANCES) and hasClickAction()).getUnclippedBoundsInRoot().top +
                    Odstepy.l
            }
        return top - handle
    }

    private fun ready(): BuildingCardUiState =
        BuildingCardUiState.Ready(
            symbol = SYMBOL,
            name = "Budynek testowy",
            entrances = orderedEntrances(listOf(entrance(MAIN), entrance(SIDE))),
        )

    private fun entrance(name: String): Entrance =
        Entrance(
            id = name,
            buildingSymbol = SYMBOL,
            name = name,
            isMain = name == MAIN,
            type = null,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = EntranceMarking.ACCESSIBLE,
            assessment = Assessed,
        )
}
