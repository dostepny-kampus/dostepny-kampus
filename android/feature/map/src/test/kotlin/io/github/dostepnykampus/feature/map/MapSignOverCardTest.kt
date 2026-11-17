/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.SIGN_PLAN
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val CARD_HEIGHT = 300

/**
 * Arkusz znaku otwarty przy karcie budynku i powrót do karty nie ruszają kamery ani zaznaczenia budynku. Ekran
 * dostaje trasę jak w aplikacji: zamknięcie karty zdejmuje budynek z trasy mapy, a otwarcie karty go przywraca.
 *
 * Wymagania: US2.1, US2.8, US16.1, NFR-17
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapSignOverCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val map = TouchableMap()
    private var cover by mutableStateOf(CARD_HEIGHT.dp)
    private var focus by mutableStateOf<MapFocus?>(MapFocus.Building("C-13"))

    /** Wiersz: V1, arkusz wejścia przy karcie i powrót do karty nie ruszają kamery. Wymagania: US2.1 */
    @Test
    fun entranceKeepsCamera() = assertCameraStays(TouchSign.ENTRANCE, "node/1")

    /** Wiersz: V1, arkusz bariery przy karcie i powrót do karty nie ruszają kamery. Wymagania: US1.5 */
    @Test
    fun barrierKeepsCamera() = assertCameraStays(TouchSign.BARRIER, "z1")

    /** Wiersz: V1, arkusz znaku planu przy karcie i powrót do karty nie ruszają kamery. Wymagania: US16.1 */
    @Test
    fun planSignKeepsCamera() = assertCameraStays(TouchSign.PLAN, "wc")

    private fun assertCameraStays(sign: TouchSign, id: String) {
        start()
        val card = compose.runOnIdle { map.selected to map.camera }
        compose.runOnIdle { map.touchSign(sign, id) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("arkusz w miejsce karty", card, map.selected to map.camera) }
        compose.onNodeWithText("Zamknij").performClick()
        compose.waitForIdle()

        compose.runOnIdle { assertEquals("powrót do karty", card, map.selected to map.camera) }
        compose.runOnIdle { assertEquals("karta otwarta znów", CARD_HEIGHT.dp, cover) }
    }

    private fun start() {
        val viewModel =
            CampusMapViewModel(
                repository =
                    unitSquareCatalog(
                        symbol = "C-13",
                        entrances = listOf(entranceFeature(symbol = "C-13", id = "node/1", tags = """"ref":"G"""")),
                    ),
                networkStatus = { flowOf(true) },
                mapListChoice = TestMapListChoice(),
                computeContext = UnconfinedTestDispatcher(),
                geometry = ::campusMapData,
                barriers = { listOf(THRESHOLD) },
            )
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            DostepnyKampusTheme {
                CampusMapScreen(
                    state = state,
                    onListChange = {},
                    onSearch = {},
                    onBuildingClick = {
                        focus = MapFocus.Building(it)
                        cover = CARD_HEIGHT.dp
                    },
                    focus = focus,
                    contentPadding = PaddingValues(bottom = cover),
                    onEmptyMapClick = {
                        focus = null
                        cover = 0.dp
                    },
                    mapView = map.view,
                    plan = SIGN_PLAN,
                )
            }
        }
        val ui = viewModel.uiState
        compose.waitUntil { ui.value.mapData != null && ui.value.barriers.isNotEmpty() }
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
