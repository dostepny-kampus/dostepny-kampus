/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHANGE = "Zmień oznaczenie"

/** „Zmień oznaczenie” Zbierającego pod każdym wejściem na V2. Wymagania: US17.2, BR-24, NFR-17, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntranceMarkingChangeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun group(onChangeMarking: ((String) -> Unit)?) {
        val card = BuildingCardUiState.Ready(symbol = "B-8", name = null, entrances = orderedEntrances(ENTRANCES))
        compose.setContent {
            DostepnyKampusTheme {
                EntrancesGroup(
                    card = card,
                    withAssessment = true,
                    onShowEntrance = { calls += "show $it" },
                    onReport = {},
                    onChangeMarking = onChangeMarking,
                )
            }
        }
    }

    /**
     * Wiersz: Zbierający, przycisk pod każdym wejściem, także przy „Brak danych”; dotknięcie otwiera C6 tego
     *   wejścia i nie pokazuje wejścia na mapie.
     *
     * Wymagania: US17.2, BR-24, NFR-17
     */
    @Test
    fun changesEachEntrance() {
        group { calls += "change $it" }
        val buttons = compose.onAllNodes(hasText(CHANGE) and hasClickAction())
        buttons.assertCountEquals(ENTRANCES.size)
        ENTRANCES.indices.forEach { buttons[it].performClick() }
        assertEquals("każdy przycisk zmienia swoje wejście", ENTRANCES.map { "change ${it.id}" }, calls)
    }

    /**
     * Wiersz: bez roli Zbierającego czynności „Zmień oznaczenie” nie ma.
     * Scenariusz (częściowo): oznaczenie-wejscia.feature:
     *   Bez roli Zbierającego zmiany oznaczenia wejścia nie da się złożyć
     *
     * Wymagania: US14.1, US17.2
     */
    @Test
    fun hidesWithoutCollector() {
        group(onChangeMarking = null)
        compose.onAllNodesWithText(CHANGE).assertCountEquals(0)
    }

    private companion object {
        val ENTRANCES =
            listOf(
                entrance(id = "node/1", name = "Główne", marking = EntranceMarking.ACCESSIBLE),
                entrance(id = "node/2", name = "Boczne", marking = EntranceMarking.NO_DATA),
            )

        fun entrance(id: String, name: String, marking: EntranceMarking): Entrance =
            Entrance(
                id = id,
                buildingSymbol = "B-8",
                name = name,
                isMain = false,
                type = "yes",
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = marking,
            )
    }
}
