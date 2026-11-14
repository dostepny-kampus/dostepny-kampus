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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val SHOW_ENTRANCE = "Pokaż wejście na mapie"

private val ShowButton = hasClickAction() and hasContentDescription("$SHOW_ENTRANCE: »Boczne«")

/** „Pokaż wejście na mapie” na V2 od dotknięcia do V1 z zaznaczonym wejściem. Wymagania: US3.3, US2.1, BR-25 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ShowEntranceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navigator: AppNavigator

    /**
     * Wiersz: V2 pełny ekran, wejście zalecane nie jest pierwsze w danych ani główne → „Pokaż wejście na mapie” przy
     *   nim prowadzi do V1 z zaznaczonym tym wejściem i budynkiem karty.
     *
     * Wymagania: US3.3, US2.1, BR-25
     */
    @Test
    fun us3_3_show_opens_v1_entrance() {
        compose.setContent {
            AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                navigator = this
                if (route is MapRoute) {
                    Text(text = "V1", modifier = Modifier.clickable { openBuilding("A-1", isDirect = true) })
                } else if (route is BuildingRoute) {
                    FixtureCard(route.symbol)
                }
            }
        }
        compose.onNodeWithText("V1").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Wejścia").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasContentDescription(SHOW_ENTRANCE, substring = true)).assertCountEquals(1)
        compose.onNode(ShowButton).performScrollTo().performClick()
        assertEquals(MapRoute(MapFocus.Entrance(id = "node/2", buildingSymbol = "A-1")), navigator.current)
    }

    @Composable
    private fun AppNavigator.FixtureCard(symbol: String) {
        val card = remember(symbol) { BuildingCardViewModel(symbol, repository, { flowOf(true) }) }
        val state by card.uiState.collectAsStateWithLifecycle()
        val isOnline by card.online.collectAsStateWithLifecycle()
        BuildingCard(symbol = symbol, state = state, isOnline = isOnline, onRetry = card::retry)
    }

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                listOf(
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"osm_id":"way/1"},""" +
                        """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}""",
                    entrance(id = "node/1", tags = """"entrance":"yes","wheelchair":"no","ref":"Schody""""),
                    entrance(id = "node/2", tags = """"entrance":"yes","wheelchair":"yes","ref":"Boczne""""),
                ).joinToString(
                    separator = ",",
                    prefix = """{"type":"FeatureCollection","attribution":"©","features":[""",
                    postfix = "]}",
                )
            }

        fun entrance(id: String, tags: String): String =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"$id",$tags},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}
