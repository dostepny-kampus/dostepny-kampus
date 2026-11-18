/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.building.BuildingCardUiState
import io.github.dostepnykampus.feature.map.building.buildingCard
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val SYMBOL = "C-13"
private const val SHOW = "Pokaż wejście na mapie"
private const val REPORT = "Zgłoś barierę przy wejściu"

private val EntranceRow =
    SemanticsMatcher("wiersz z czynnością „$SHOW”") { node ->
        node.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == SHOW
    }

/** Wiersz wejścia na liście V2 od dotknięcia do V1 z zaznaczonym wejściem. Wymagania: US3.3, US4.6 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CardEntranceRowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navigator: AppNavigator

    /**
     * Wiersz: V2 pełny ekran → dotknięcie wiersza ostatniego wejścia na liście prowadzi do V1 z zaznaczonym tym
     *   wejściem i jego arkuszem na całym ekranie, jak „Pokaż wejście na mapie” z karty otwartej od razu.
     *
     * Wymagania: US3.3
     */
    @Test
    fun opensEntranceFromRow() {
        val ids = entranceIds()
        show()
        compose.onAllNodes(EntranceRow).assertCountEquals(ids.size)
        val row = compose.onAllNodes(EntranceRow)[ids.lastIndex]
        row.performScrollTo()
        row.performClick()
        compose.waitForIdle()
        val focus = MapFocus.Entrance(id = ids.last(), buildingSymbol = SYMBOL, isFullSheet = true)
        assertEquals(MapRoute(focus), navigator.current)
    }

    /**
     * Wiersz: V2 pełny ekran nie ma „Zgłoś barierę przy wejściu” przy wejściach; ta czynność jest w arkuszu wejścia.
     *
     * Wymagania: US4.6
     */
    @Test
    fun leavesReportToSheet() {
        show()
        compose.onAllNodes(hasText(REPORT, substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasContentDescription(REPORT, substring = true)).assertCountEquals(0)
    }

    private fun entranceIds(): List<String> =
        runBlocking {
            val catalog =
                BuildingCatalogRepository(EmptyCoroutineContext) {
                    val assets = compose.activity.assets
                    assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() }
                }
            val card = buildingCard(catalog.catalog(), SYMBOL) as BuildingCardUiState.Ready
            card.entrances.map { it.entrance.id }
        }

    private fun show() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route: NavKey ->
                navigator = this
                if (route is MapRoute) {
                    Text(text = "V1", modifier = Modifier.clickable { openBuilding(SYMBOL, isDirect = true) })
                } else if (route is BuildingRoute) {
                    TestBuildingCard(route.symbol)
                }
            }
        }
        compose.onNodeWithText("V1").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Wejścia").fetchSemanticsNodes().isNotEmpty() }
    }
}
