/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext

/** Dotknięcie znaku bariery na mapie V1 otwiera arkusz bariery, na widoku zastępczym mapy. Wymagania: US1.5, NFR-17 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapBarrierTouchTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private var touchBarrier: (String) -> Unit = {}
    private var touchBuildings: (List<String>) -> Unit = {}
    private var marked: String? = null
    private val fakeMap =
        CampusMapView { _: CampusMapData, selection, _, _, _, onBuildingsTouch, onSignTouch, _: Modifier ->
            marked = selection.barrier
            touchBuildings = onBuildingsTouch
            touchBarrier = { onSignTouch(TouchSign.BARRIER, it) }
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

    /** Wiersz: V1, dotknięcie znaku bariery otwiera arkusz bariery; „Zamknij” wraca do samej mapy. Wymagania: US1.5 */
    @Test
    fun opensSheetOnTouch() {
        start()
        compose.runOnIdle { touchBarrier("z1") }

        compose.onNodeWithText("Próg").assertExists()
        compose.onNodeWithText("Budynek C-13").assertExists()
        compose.onNodeWithText("Zamknij").performClick()
        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    /**
     * Wiersz: V1, „Wstecz” przy otwartym arkuszu bariery zamyka arkusz i zdejmuje wyróżnienie znaku.
     *
     * Wymagania: US1.5
     */
    @Test
    fun closesSheetOnBack() {
        start()
        compose.runOnIdle { touchBarrier("z1") }
        compose.onNodeWithText("Próg").assertExists()
        compose.runOnIdle { assertEquals("z1", marked) }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Próg").assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, marked) }
    }

    /**
     * Wiersz: arkusz bariery, wiersz budynku otwiera V2 budynku bariery i zamyka arkusz (PW-5).
     * Scenariusz (częściowo, przykład: dotknie znaku bariery przy budynku A-1): przegladanie-mapy.feature:
     *   Wiersz budynku w arkuszu otwiera kartę budynku bez przelotu mapy
     *
     * Wymagania: US1.5, US2.8
     */
    @Test
    fun opensBuildingFromSheet() {
        start()
        compose.runOnIdle { touchBarrier("z1") }
        compose.onNodeWithContentDescription("Budynek C-13").performClick()

        assertEquals(listOf("C-13"), opened)
        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    /** Wiersz: arkusz bariery i arkusz „Który budynek?” nie są otwarte jednocześnie. Wymagania: US1.5 */
    @Test
    fun opensOneSheetAtATime() {
        start()
        compose.runOnIdle { touchBuildings(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Który budynek?").assertExists()
        compose.runOnIdle { touchBarrier("z1") }
        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        compose.onNodeWithText("Próg").assertExists()
        compose.runOnIdle { touchBuildings(emptyList()) }

        compose.onNodeWithText("Próg").assertDoesNotExist()
    }

    /** „Wstecz” przy otwartym arkuszu „Który budynek?” zamyka arkusz, a mapa zostaje. Wymagania: US2, NFR-15 */
    @Test
    fun closesChoiceOnBack() {
        start()
        compose.runOnIdle { touchBuildings(listOf("C-13", "C-14")) }
        compose.onNodeWithText("Który budynek?").assertExists()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.onNodeWithText("Który budynek?").assertDoesNotExist()
        compose.runOnIdle { assertFalse(compose.activity.isFinishing) }
    }

    private fun start() {
        val viewModel =
            CampusMapViewModel(
                repository = repository,
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
                server = NO_SERVER.with(barriers = { listOf(THRESHOLD) }),
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
