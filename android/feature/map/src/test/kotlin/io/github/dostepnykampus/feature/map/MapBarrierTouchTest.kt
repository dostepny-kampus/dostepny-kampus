/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

/** Dotknięcie znaku bariery na mapie V1 otwiera arkusz bariery, na widoku zastępczym mapy (US1.5, NFR-17). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapBarrierTouchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private var touchBarrier: (String) -> Unit = {}
    private var touchBuildings: (List<String>) -> Unit = {}
    private val fakeMap =
        CampusMapView { _: CampusMapData, _, _, _, onBuildingsTouch, onBarrierTouch, _: Modifier ->
            touchBuildings = onBuildingsTouch
            touchBarrier = onBarrierTouch
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            val features =
                listOf("C-13" to 0, "C-14" to 2).joinToString(separator = ",") { (symbol, west) ->
                    """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"]},"geometry":""" +
                        """{"type":"Polygon","coordinates":[[[$west,0],[${west + 1},0],[${west + 1},1],[$west,0]]]}}"""
                }
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[$features]}"""
        }

    /** Wiersz: V1, dotknięcie znaku bariery otwiera arkusz bariery; „Zamknij” wraca do samej mapy (US1.5). */
    @Test
    fun us1_5_touch_opens_sheet() {
        start()
        compose.runOnIdle { touchBarrier("z1") }

        compose.onNodeWithText("Próg").assertExists()
        compose.onNodeWithText("Budynek C-13").assertExists()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    /** Wiersz: arkusz bariery, „Pokaż opis budynku” otwiera V2 budynku bariery i zamyka arkusz (US1.5, PW-5). */
    @Test
    fun us1_5_sheet_opens_building() {
        start()
        compose.runOnIdle { touchBarrier("z1") }
        compose.onNodeWithText("Pokaż opis budynku").performClick()

        assertEquals(listOf("C-13"), opened)
        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    /** Wiersz: arkusz bariery i arkusz „Który budynek?” nie są otwarte jednocześnie (US1.5, decyzja 85). */
    @Test
    fun us1_5_one_sheet_at_a_time() {
        start()
        compose.runOnIdle { touchBuildings(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Który budynek?").assertExists()
        compose.runOnIdle { touchBarrier("z1") }
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        compose.onNodeWithText("Próg").assertExists()
        compose.runOnIdle { touchBuildings(emptyList()) }

        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    private fun start() {
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
                barriers = { listOf(THRESHOLD) },
            )
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(onBuildingClick = { opened += it }, mapView = fakeMap, viewModel = viewModel)
            }
        }
        val state = viewModel.uiState
        compose.waitUntil { state.value.barriers.isNotEmpty() }
        compose.waitForIdle()
    }

    private companion object {
        val THRESHOLD =
            AcceptedBarrier(
                id = "z1",
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                severity = Severity.HINDRANCE,
                type = BarrierType.THRESHOLD,
                buildingSymbol = "C-13",
                verifiedAt = null,
            )
    }
}
