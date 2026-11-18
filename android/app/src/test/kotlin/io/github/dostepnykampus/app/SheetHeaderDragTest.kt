/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
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
import io.github.dostepnykampus.app.navigation.sheetCard
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.building.BuildingCardContent
import io.github.dostepnykampus.feature.map.building.BuildingCardUiState
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NAME = "Gmach Główny"
private const val ENTRANCE_COUNT = 20
private const val LAST = "Wejście $ENTRANCE_COUNT"
private const val SWIPE_MILLIS = 600L
private val DRAG = 160.dp

/**
 * Arkusz V2 na całym ekranie z przewiniętą treścią: przeciągnięcie w dół po nagłówku karty opuszcza kartę o stopień,
 * tak jak przeciągnięcie uchwytu.
 *
 * Wymagania: US2.8, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SheetHeaderDragTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, cały ekran, treść przewinięta: przeciągnięcie nagłówka w dół → połowa ekranu.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Uchwyt zmienia wysokość karty bez przeciągania
     */
    @Test
    fun lowersFromFullOnHeaderDragWhenScrolled() {
        open()
        val handle = compose.onNodeWithContentDescription(EXPAND_HANDLE)
        handle.performClick()
        handle.performClick()
        assertEquals("cały ekran", 0f, handleTop().value, 1f)
        compose.onNodeWithText(LAST).performScrollTo()
        compose.onNodeWithText(NAME).performTouchInput {
            swipe(start = center, end = center + Offset(x = 0f, y = DRAG.toPx()), durationMillis = SWIPE_MILLIS)
        }
        compose.waitForIdle()
        assertEquals("połowa ekranu po przeciągnięciu nagłówka", (map() / 2).value, handleTop().value, 1f)
    }

    private fun open() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
                if (route is MapRoute) {
                    Column(modifier = Modifier.fillMaxSize().testTag("V1")) {
                        Text(text = "A-1", modifier = Modifier.clickable { openBuilding(symbol = "A-1") })
                    }
                } else {
                    val card = sheetCard()
                    BuildingCardContent(
                        symbol = "A-1",
                        mode = card.mode,
                        card = ready(),
                        isOnline = true,
                        onShowOnMap = {},
                        onBack = {},
                        onClose = {},
                        onReport = {},
                        onRetry = {},
                        onExpand = card.onExpand,
                    )
                }
            }
        }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
    }

    private fun map(): Dp = compose.onNodeWithTag("V1").getUnclippedBoundsInRoot().height

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
