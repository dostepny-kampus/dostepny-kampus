/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
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

/** V2 Karta budynku, gdy aplikacja nie ma żadnych danych o barierach (US2.7, NFR-10). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bez danych o barierach lista i karta budynku
     *   nic o barierach nie mówią.
     */
    @Test
    fun us2_7_no_data_says_nothing() {
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],""" +
                    """"name":"Budynek testowy","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                    """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"node/1",""" +
                    """"entrance":"main","wheelchair":"yes","ref":"Główne"},""" +
                    """"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
            }
        val viewModel =
            BuildingCardViewModel(symbol = "C-13", repository = repository, networkStatus = { flowOf(true) })
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "C-13",
                    mode = CardMode.Expanded,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.onNodeWithText("Budynek testowy").assertIsDisplayed()
        compose.onNodeWithText("Wejścia").assertIsDisplayed()
        compose.onNodeWithText("Główne").assertIsDisplayed()
        compose.onNodeWithText("Sprawdź, którędy wejść").assertIsDisplayed()
        listOf("Brak zgłoszonych barier", "Bariera pojawi się tutaj", "Bariery w budynku").forEach { text ->
            compose.onNodeWithText(text, substring = true, ignoreCase = true).assertDoesNotExist()
        }
    }
}
