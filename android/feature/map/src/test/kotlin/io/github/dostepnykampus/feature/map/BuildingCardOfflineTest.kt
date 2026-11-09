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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** V2 Karta budynku bez internetu (NFR-10, US2.8). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardOfflineTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2 bez internetu → linia „Bez internetu” bez daty jak na V1, ze stanu sieci telefonu; po powrocie
     *   sieci linia znika, NFR-10, US2.8, decyzje 84, 115.
     */
    @Test
    fun nfr10_offline_line_on_card() {
        val online = MutableStateFlow(false)
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],""" +
                    """"name":"Gmach Główny","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            }
        val viewModel = BuildingCardViewModel(symbol = "A-1", repository = repository, networkStatus = { online })
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.onNodeWithText("Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText("dane z", substring = true).assertDoesNotExist()
        online.value = true
        compose.waitForIdle()
        compose.onNodeWithText("Bez internetu").assertDoesNotExist()
    }

    /** Wiersz: V2, stan sieci jeszcze nie przyszedł → brak linii „Bez internetu”, żeby nie mignęła, NFR-10, US2.8. */
    @Test
    fun nfr10_no_line_before_state() {
        val repository =
            BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],""" +
                    """"name":"Gmach Główny","osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            }
        val viewModel =
            BuildingCardViewModel(
                symbol = "A-1",
                repository = repository,
                networkStatus = { flow { awaitCancellation() } },
            )
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    onCheckEntrances = {},
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        compose.onNodeWithText("Gmach Główny").assertIsDisplayed()
        compose.onNodeWithText("Bez internetu").assertDoesNotExist()
    }
}
