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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions.OnClick
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapView
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.data.CampusMapData
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
 * Katalog to „Budynek testowy” C-13, nie dane PWr.
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
        CampusMapView { _: CampusMapData, _, _, onBuildingsTouch, modifier: Modifier ->
            touch = onBuildingsTouch
            Box(modifier = modifier.fillMaxSize().background(DostepnyKampusTheme.mapColors.lad))
        }
    private val repository =
        BuildingCatalogRepository(ioContext = EmptyCoroutineContext) {
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
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
                AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
                    if (route is MapRoute) {
                        CampusMapScreen(
                            onBuildingClick = { openBuilding(it) },
                            contentPadding = LocalSheetPadding.current,
                            mapView = landOnly,
                            viewModel = remember { CampusMapViewModel(repository, { flowOf(isOnline) }, choice) },
                        )
                    } else {
                        AppScreen(route)
                    }
                }
            }
        }
        compose.waitForIdle()
        // Akcja zamiast dotyku: zrzut bez śladu dotknięcia na przełączniku.
        val list = compose.onNode(hasContentDescription("Lista budynków") and isSelectable())
        if (isList) list.performSemanticsAction(OnClick)
        // Przy dużej czcionce V1 startuje na liście, więc V2 otwiera budynek z listy (NFR-19).
        val isFromList = compose.onAllNodes(hasContentDescription("C-13")).fetchSemanticsNodes().isNotEmpty()
        if (isSheetOpen && isFromList) compose.onNodeWithContentDescription("C-13").performSemanticsAction(OnClick)
        if (isSheetOpen && !isFromList) compose.runOnUiThread { touch(listOf("C-13")) }
        query?.let { compose.onNode(hasSetTextAction()).performTextInput(it) }
        if (isKeyboardOpen) compose.runOnUiThread { dispatchKeyboard(compose.activity, isOpen = true) }
        compose.onRoot().captureRoboImage()
    }

    // V1: przyciski mapy, podpis OSM i pasek dolny, NFR-16, NFR-37.
    @Test
    fun nfr37_v1_jasny() {
        capture(isSheetOpen = false)
    }

    // V1: wynik pod polem po wpisaniu „c13”, US1.1.
    @Test
    fun us1_1_v1_wynik_jasny() {
        capture(isSheetOpen = false, query = "c13")
    }

    // V1 przy otwartej klawiaturze: bez przycisków mapy, z podpisem OSM, NFR-53, decyzja 91.
    @Test
    fun nfr53_v1_klawiatura_jasny() {
        capture(isSheetOpen = false, query = "c13", isKeyboardOpen = true)
    }

    // V1: brak wyniku dla „C-31”, UC1 3a, PW-9.
    @Test
    fun us1_1_v1_brak_wyniku_jasny() {
        capture(isSheetOpen = false, query = "C-31")
    }

    // V1 bez internetu przy danych z aplikacji: linia „Bez internetu” bez daty, US9.1, NFR-10, decyzja 84.
    @Test
    fun us9_1_v1_bez_internetu_jasny() {
        capture(isSheetOpen = false, isOnline = false)
    }

    // V1 z wybraną listą budynków, NFR-19.
    @Test
    fun nfr19_v1_lista_jasny() {
        capture(isSheetOpen = false, isList = true)
    }

    // Przy czcionce 200 % V1 startuje na liście budynków, a przełącznik ma napis „Lista”, NFR-19, NFR-20.
    @Test
    @Config(fontScale = 2f)
    fun nfr20_v1_lista_200() {
        capture(isSheetOpen = false)
    }

    // V2 jako arkusz do połowy ekranu nad V1, decyzja 48, NFR-16.
    @Test
    fun nfr16_v2_arkusz_jasny() {
        capture()
    }

    @Test
    fun nfr16_v2_arkusz_ciemny() {
        RuntimeEnvironment.setQualifiers("+night")
        capture()
    }

    // Czcionka 200 %: nagłówek, uchwyt i pasek dolny, NFR-20.
    @Test
    fun nfr20_v2_arkusz_200() {
        RuntimeEnvironment.setFontScale(2f)
        capture()
    }

    // Okno ≥ 600 dp: pasek z boku, a arkusz dalej od dołu, decyzja 69.
    @Test
    @Config(qualifiers = "w640dp-h480dp-xhdpi")
    fun nfr51_v2_arkusz_szerokie() {
        capture()
    }
}
