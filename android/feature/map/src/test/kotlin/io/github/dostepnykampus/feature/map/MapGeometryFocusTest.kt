/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

private const val DELTA = 1e-9

private val MARKER = JsonPrimitive("geometria z ViewModelu")

private val OUTLINE_NOT_FROM_CATALOG = CampusBounds(west = 0.2, south = 0.6, east = 0.4, north = 0.8)

/**
 * Ekran V1 z widokiem zastępczym: geometria mapy idzie z ViewModelu, a zmiana zaznaczenia jej nie liczy, także punktu
 * podpisu dla kamery.
 *
 * Wymagania: NFR-11
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapGeometryFocusTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = AtomicInteger()
    private val shown = mutableListOf<CampusMapData>()
    private val focuses = mutableListOf<String?>()
    private val cameras = mutableListOf<MapCamera>()
    private val fakeMap =
        CampusMapView { data: CampusMapData, selection, camera, _, _, _, _: Modifier ->
            shown += data
            focuses += selection.building
            cameras += camera
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        }
    private val viewModel by lazy {
        CampusMapViewModel(
            repository = repository,
            networkStatus = { flowOf(true) },
            mapListChoice = TestMapListChoice(),
            computeContext = UnconfinedTestDispatcher(),
            geometry = { catalog ->
                calls.incrementAndGet()
                campusMapData(catalog)
                    .copy(labelPoints = MARKER, outlinesBySymbol = mapOf("C-13" to OUTLINE_NOT_FROM_CATALOG))
            },
        )
    }
    private var focus by mutableStateOf<MapFocus?>(null)

    /**
     * Wiersz: W3, W4, O5, O6, mapa pokazana, potem zmiana zaznaczonego budynku → widok mapy dostaje geometrię
     *   policzoną w ViewModelu z nowym zaznaczeniem, a funkcja licząca poszła raz.
     *
     * Wymagania: NFR-11, US1.1
     */
    @Test
    fun nfr11_focus_keeps_geometry() {
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(onBuildingClick = {}, focus = focus, mapView = fakeMap, viewModel = viewModel)
            }
        }
        compose.waitUntil { shown.isNotEmpty() }
        compose.runOnIdle { focus = MapFocus.Building("C-13") }
        compose.waitForIdle()
        assertEquals("zaznaczenie nie doszło do mapy", "C-13", focuses.last())
        assertEquals("ekran liczy geometrię sam", listOf(MARKER), shown.map { it.labelPoints }.distinct())
        assertEquals("geometria liczona od nowa po zmianie zaznaczenia", 1, calls.get())
    }

    /**
     * Wiersz: W3, W4, arkusz V2 zasłania dół mapy, budynek C-13 zaznaczony → środek kamery nad arkuszem to środek
     *   prostokąta C-13 z danych mapy policzonych w ViewModelu, inny niż kontur z katalogu, więc nie liczony na
     *   ekranie.
     *
     * Wymagania: NFR-11
     */
    @Test
    fun nfr11_sheet_camera_from_data() {
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(
                    onBuildingClick = {},
                    focus = MapFocus.Building("C-13"),
                    contentPadding = PaddingValues(bottom = 200.dp),
                    mapView = fakeMap,
                    viewModel = viewModel,
                )
            }
        }
        compose.waitUntil { cameras.isNotEmpty() }
        compose.waitForIdle()
        val center = cameras.last().center
        assertEquals(0.3, center.longitude, DELTA)
        assertEquals(0.7, center.latitude, DELTA)
    }
}
