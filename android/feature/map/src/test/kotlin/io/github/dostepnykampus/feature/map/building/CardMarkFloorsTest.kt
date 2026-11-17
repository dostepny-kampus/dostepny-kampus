/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** „Oznacz obejrzane piętra” pod grupą „Piętra” pełnej karty V2, tylko gdy jest czynność Zbierającego. */
@RunWith(RobolectricTestRunner::class)
class CardMarkFloorsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: V2, Zbierający, „Oznacz obejrzane piętra” otwiera C5. Wymagania: US15.1 */
    @Test
    fun opensFloorMarking() {
        var opened = 0
        show(onMarkFloors = { opened++ })
        compose.onNodeWithText(BUTTON).performScrollTo().performClick()
        assertEquals("otwarcia C5", 1, opened)
    }

    /** Bez roli Zbierającego przycisku nie ma. Wymagania: US15.1, BR-20 */
    @Test
    fun hidesMarkingWithoutRole() {
        show(onMarkFloors = null)
        compose.onNodeWithText(BUTTON).assertDoesNotExist()
    }

    private fun show(onMarkFloors: (() -> Unit)?) {
        val entrance =
            Entrance(
                id = "a1-main",
                buildingSymbol = "A-1",
                name = "Główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = EntranceMarking.ACCESSIBLE,
            )
        val card = BuildingCardUiState.Ready("A-1", name = null, entrances = orderedEntrances(listOf(entrance)))
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    card = card,
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReportEntrance = {},
                    onRetry = {},
                    onMarkFloors = onMarkFloors,
                )
            }
        }
    }

    private companion object {
        const val BUTTON = "Oznacz obejrzane piętra"
    }
}
