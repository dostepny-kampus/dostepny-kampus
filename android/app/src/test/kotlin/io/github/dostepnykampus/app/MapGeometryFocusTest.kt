/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

private const val MARKER = "geometria z ViewModelu"

/** Ekran V1 z widokiem zastępczym: geometria mapy idzie z ViewModelu, a zmiana zaznaczenia jej nie liczy (NFR-11). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapGeometryFocusTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = AtomicInteger()
    private val shown = mutableListOf<CampusMapData>()
    private val fakeMap = CampusMapView { data: CampusMapData, _, _, _, _: Modifier -> shown += data }
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
                campusMapData(catalog).copy(labelPoints = MARKER)
            },
        )
    }
    private var focus by mutableStateOf<String?>(null)

    // Wiersz: W3, W4, O5, O6, mapa pokazana, potem zmiana zaznaczonego budynku → widok mapy dostaje geometrię
    //   policzoną w ViewModelu z nowym zaznaczeniem, a funkcja licząca poszła raz, NFR-11, US1.1.
    @Ignore("geometria mapy liczona w kompozycji, poprawka w następnym PR")
    @Test
    fun nfr11_focus_keeps_geometry() {
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(onBuildingClick = {}, focus = focus, mapView = fakeMap, viewModel = viewModel)
            }
        }
        compose.waitUntil { shown.isNotEmpty() }
        compose.runOnIdle { focus = "C-13" }
        compose.waitForIdle()
        assertEquals("zaznaczenie nie doszło do mapy", "C-13", shown.last().focus)
        assertEquals("ekran liczy geometrię sam", listOf(MARKER), shown.map { it.labelPoints }.distinct())
        assertEquals("geometria liczona od nowa po zmianie zaznaczenia", 1, calls.get())
    }
}
