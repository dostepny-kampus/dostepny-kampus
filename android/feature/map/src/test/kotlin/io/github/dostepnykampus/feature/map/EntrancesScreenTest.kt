/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** V3 przy pustych danych o wejściach istniejącego budynku (US3.1, US17.1, PW-10). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class EntrancesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun us3_1_empty_shows_no_data() {
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-2"],""" +
                    """"name":"Budynek testowy","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            }
        val viewModel =
            BuildingCardViewModel(symbol = "A-2", repository = repository, networkStatus = { flowOf(true) })
        compose.setContent {
            DostepnyKampusTheme {
                EntrancesScreen(symbol = "A-2", onShowEntrance = {}, onBack = {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("Którędy wejść do A-2").assertIsDisplayed()
        compose.onNodeWithText("Nie mamy jeszcze danych o wejściach do budynku A-2.").assertIsDisplayed()
        listOf("nie ma wejścia dostępnego", "0 z 0", "Wybierz", "bariery", "każde wejście").forEach { advice ->
            compose.onNodeWithText(advice, substring = true).assertDoesNotExist()
        }
    }

    /**
     * Wiersz: V3, „Brak danych o wejściach”: „z datą” tylko gdy wejścia mają datę i źródło, inaczej „każde wejście”,
     * PW-10, US17.3.
     */
    @Test
    fun us17_3_unknown_no_date_promise() {
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-2"],""" +
                    """"name":"Budynek testowy","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                    """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-2"],"osm_id":"node/1",""" +
                    """"entrance":"yes"},"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
            }
        val viewModel =
            BuildingCardViewModel(symbol = "A-2", repository = repository, networkStatus = { flowOf(true) })
        compose.setContent {
            DostepnyKampusTheme {
                EntrancesScreen(symbol = "A-2", onShowEntrance = {}, onBack = {}, viewModel = viewModel)
            }
        }
        compose.onNodeWithText("brak danych o 1 z 1 wejść", substring = true).assertIsDisplayed()
        compose.onNodeWithText("zobaczyć każde wejście.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("z datą", substring = true).assertDoesNotExist()
    }
}
