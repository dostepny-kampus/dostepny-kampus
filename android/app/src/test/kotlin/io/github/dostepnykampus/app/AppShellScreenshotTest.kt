/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.app.navigation.LocalSheetPadding
import io.github.dostepnykampus.app.navigation.sheetCard
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.building.BuildingCardScreen
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Zrzuty oboczki z ekranami: V1 z przyciskami mapy i podpisem OSM, V2 jako arkusz nad V1 i pasek nawigacji.
 * Natywnej mapy MapLibre w testach JVM nie ma: zamiast niej jest tło lądu z palety mapy, bez konturów i ulic.
 * Klawiatury Robolectric nie rysuje, więc zrzut z klawiaturą ma w jej miejscu ciemny prostokąt jej wysokości.
 * Katalog to „Budynek testowy” C-13, nie dane PWr. Zrzut robi akcja zamiast dotyku, więc na przełączniku nie ma
 * śladu dotknięcia. Przy dużej czcionce V1 startuje na liście, więc V2 otwiera budynek z listy.
 *
 * Wymagania: NFR-19, NFR-51
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AppShellScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var touch: (List<String>) -> Unit = {}
    private val choice = TestMapListChoice()
    private val landOnly =
        CampusMapView { _: CampusMapData, _, _, _, _, _, onBuildingsTouch, _, modifier: Modifier ->
            touch = onBuildingsTouch
            Box(modifier = modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad))
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"node/1",""" +
                """"entrance":"yes","wheelchair":"yes","ref":"Od podwórza"},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"node/2",""" +
                """"entrance":"main","wheelchair":"no","ref":"Główne"},""" +
                """"geometry":{"type":"Point","coordinates":[1,0]}}]}"""
        }

    @Composable
    private fun ShellWithMap(isOnline: Boolean) {
        AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
            if (route is MapRoute) {
                CampusMapScreen(
                    onBuildingClick = { openBuilding(it) },
                    contentPadding = LocalSheetPadding.current,
                    mapView = landOnly,
                    viewModel = remember { inlineMapViewModel(repository, { flowOf(isOnline) }, choice) },
                )
            } else if (route is BuildingRoute) {
                val card =
                    remember(route.symbol) {
                        BuildingCardViewModel(route.symbol, repository, { flowOf(isOnline) })
                    }
                val sheet = sheetCard()
                BuildingCardScreen(
                    symbol = route.symbol,
                    mode = sheet.mode,
                    onShowOnMap = { showOnMap(MapFocus.Building(route.symbol)) },
                    onBack = sheet.onBack,
                    onClose = sheet.onClose,
                    viewModel = card,
                )
            } else {
                AppScreen(route)
            }
        }
    }

    private fun capture(
        isSheetOpen: Boolean = true,
        query: String? = null,
        isOnline: Boolean = true,
        isList: Boolean = false,
        isKeyboardOpen: Boolean = false,
    ) {
        compose.setContent {
            DostepnyKampusTheme {
                Box {
                    ShellWithMap(isOnline)
                    if (isKeyboardOpen) KeyboardPlaceholder(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        compose.waitForIdle()
        val list = compose.onNode(hasText("Pokaż listę"))
        if (isList) list.performSemanticsAction(OnClick)
        val isFromList = compose.onAllNodes(hasContentDescription("C-13")).fetchSemanticsNodes().isNotEmpty()
        if (isSheetOpen && isFromList) compose.onNodeWithContentDescription("C-13").performSemanticsAction(OnClick)
        if (isSheetOpen && !isFromList) compose.runOnUiThread { touch(listOf("C-13")) }
        query?.let { compose.onNode(hasSetTextAction()).performTextInput(it) }
        if (isKeyboardOpen) compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen = true) }
        val card = hasContentDescription("Budynek C-13", substring = true)
        if (isSheetOpen) compose.waitUntil { compose.onAllNodes(card).fetchSemanticsNodes().isNotEmpty() }
        compose.onRoot().captureRoboImage()
    }

    /** V1: przyciski mapy, podpis OSM i pasek dolny. Wymagania: NFR-16, NFR-37 */
    @Test
    fun drawsLightMap() {
        capture(isSheetOpen = false)
    }

    /** V1: wynik pod polem po wpisaniu „c13”. Wymagania: US1.1 */
    @Test
    fun drawsLightSearchResult() {
        capture(isSheetOpen = false, query = "c13")
    }

    /** V1 przy otwartej klawiaturze: bez przycisków mapy, z podpisem OSM. Wymagania: NFR-53 */
    @Test
    fun drawsLightKeyboard() {
        capture(isSheetOpen = false, query = "c13", isKeyboardOpen = true)
    }

    /** V1: brak wyniku dla „C-31”, PW-9. Wymagania: UC1 */
    @Test
    fun drawsLightNoResult() {
        capture(isSheetOpen = false, query = "C-31")
    }

    /** V1 bez internetu przy danych z aplikacji: linia „Bez internetu” bez daty. Wymagania: US9.1, NFR-10 */
    @Test
    fun drawsLightOffline() {
        capture(isSheetOpen = false, isOnline = false)
    }

    /** V2 mała karta bez internetu: plakietka „Bez internetu” nad symbolem. Wymagania: US2.8, NFR-10 */
    @Test
    fun drawsOfflineSheet() {
        capture(isOnline = false)
    }

    /** V1 z wybraną listą budynków. Wymagania: NFR-19 */
    @Test
    fun drawsLightList() {
        capture(isSheetOpen = false, isList = true)
    }

    /** Przy czcionce 200 % V1 startuje na liście budynków, a przełącznik ma napis „Lista”. Wymagania: NFR-19, NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun drawsListAtLargeFont() {
        capture(isSheetOpen = false)
    }

    /** V2 jako mała karta nad V1. Wymagania: NFR-16 */
    @Test
    fun drawsLightSheet() {
        capture()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkSheet() {
        capture()
    }

    /** Czcionka 200 %: nagłówek, uchwyt i pasek dolny. Wymagania: NFR-20 */
    @Test
    fun drawsSheetAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    /** Okno ≥ 600 dp: pasek z boku, a arkusz dalej od dołu. */
    @Test
    @Config(qualifiers = "w640dp-h480dp-xhdpi")
    fun drawsWideSheet() {
        capture()
    }
}

@Composable
private fun KeyboardPlaceholder(modifier: Modifier = Modifier) {
    val height = with(LocalDensity.current) { KEYBOARD_HEIGHT.toDp() }
    Box(
        modifier = modifier.fillMaxWidth().height(height).background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "klawiatura", color = MaterialTheme.colorScheme.inverseOnSurface)
    }
}
