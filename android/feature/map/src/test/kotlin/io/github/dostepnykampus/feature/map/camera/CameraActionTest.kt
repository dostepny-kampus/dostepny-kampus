/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.CampusMapViewModel
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.testMapViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext

private val Card = 280.dp
private const val FLIGHT_MAX_MILLIS = 1500L

/**
 * Ruchy kamery po czynnościach osoby na mapie V1 z prawdziwymi danymi PWr, prawdziwym sterownikiem kamery i udawaną
 * mapą. Karta V2 to dół ekranu zasłonięty na 280 dp razem z zaznaczeniem budynku, jak w aplikacji.
 *
 * Wymagania: US2.8, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class CameraActionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val camera = RecordedCamera()
    private var focus by mutableStateOf<MapFocus?>(null)
    private var cover by mutableStateOf(0.dp)

    /**
     * Wiersz: V1, dotknięcie konturu: najwyżej jeden ruch w 300 ms, tylko w pionie i bez zmiany powiększenia.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun tapMovesOnlyNorthOnce() {
        showMap()
        compose.runOnIdle { camera.touch(listOf("A-1")) }
        compose.waitForIdle()
        assertTrue("Najwyżej jeden ruch: ${camera.moves}", camera.moves.size <= 1)
        camera.moves.forEach { call ->
            assertTrue("Najwyżej 300 ms: $call", call.millis <= 300L)
            assertEquals("Bez zmiany powiększenia: $call", call.before.zoom, call.after.zoom, 1e-9)
            assertEquals("Tylko w pionie: $call", call.before.center.longitude, call.after.center.longitude, 1e-9)
        }
    }

    /**
     * Wiersz: V1, wynik wyszukiwania budynku obok to jeden ruch mapy nad kartę w najwyżej 400 ms.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun searchNearMovesOnce() {
        showMap()
        open("A-2")
        assertEquals("Jeden ruch: ${camera.moves}", 1, camera.moves.size)
        assertTrue("Najwyżej 400 ms: ${camera.moves}", camera.moves.single().millis <= 400L)
    }

    /**
     * Wiersz: V1, wynik wyszukiwania z innego terenu to jeden przelot w najwyżej 1,5 s, a granice nowego terenu idą
     * do mapy po nim.
     *
     * Wymagania: US1.8, NFR-56, BR-G5
     */
    @Test
    fun otherAreaFliesOnce() {
        val model = showMap()
        open("T-16")
        val wittigowo = requireNotNull(model.uiState.value.mapData).areas.single { it.name == "Wittigowo" }.bounds
        assertEquals("Jeden ruch: ${camera.moves}", 1, camera.moves.size)
        assertTrue("Przelot ≤ 1,5 s: ${camera.moves}", camera.moves.single().millis <= FLIGHT_MAX_MILLIS)
        assertEquals("Granice nowego terenu", wittigowo, camera.limits.last()?.bounds)
    }

    /**
     * Wiersz: V2, karta rosnąca tuż po otwarciu nie daje drugiego ruchu mapy.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Ignore("#2823: dopełnienie rosnącej karty dosuwa się drugim ruchem po końcu pierwszego")
    @Test
    fun cardGrowthMovesOnce() {
        showMap()
        open("A-2", height = Card - 80.dp)
        compose.runOnIdle { cover = Card }
        compose.waitForIdle()
        assertEquals("Jeden ruch: ${camera.moves}", 1, camera.moves.size)
    }

    /**
     * Wiersz: V2, „Zamknij” zostawia obraz mapy bez ruchu.
     *
     * Wymagania: US2.8, NFR-56
     */
    @Test
    fun closeKeepsPicture() {
        showMap()
        open("A-2")
        camera.clear()
        compose.runOnIdle { cover = 0.dp }
        compose.waitForIdle()
        assertTrue("Obraz mapy stoi: ${camera.moves}", camera.moves.none { it.isVisible })
    }

    /**
     * Wiersz: V1 i V2, granice kamery w spoczynku: nad kartą V2 cała wysokość mapy, po zamknięciu karty tylko część
     * pod paskiem, przy tej samej szerokości.
     *
     * Wymagania: NFR-39, BR-G5
     */
    @Test
    fun restLimitsFollowCard() {
        showMap()
        open("A-2")
        val overCard = requireNotNull(camera.limits.last())
        compose.runOnIdle { cover = 0.dp }
        compose.waitForIdle()
        val closed = requireNotNull(camera.limits.last())
        assertEquals("Ta sama szerokość", overCard.widthDp, closed.widthDp, 1e-9)
        assertTrue(
            "Nad kartą cała mapa: ${overCard.heightDp} > ${closed.heightDp}",
            overCard.heightDp > closed.heightDp,
        )
    }

    /**
     * Wiersz: V1, wybór z listy budynków: mapa od razu na budynku, bez klatek ruchu.
     *
     * Wymagania: US2.8, NFR-19, NFR-56
     */
    @Test
    fun listPickStandsStill() {
        focus = MapFocus.Building(symbol = "A-5")
        cover = Card
        showMap(isSettled = false)
        assertTrue("Bez ruchu na ekranie: ${camera.moves}", camera.moves.all { it.motion == CameraMotion.NONE })
    }

    /**
     * Wiersz: V1, „Powiększ” to jeden ruch mapy.
     *
     * Wymagania: NFR-53, NFR-56
     */
    @Test
    fun zoomButtonMovesOnce() {
        showMap()
        compose.onNode(hasClickAction() and hasContentDescription("Powiększ")).performClick()
        compose.waitForIdle()
        assertEquals("Jeden ruch: ${camera.moves}", 1, camera.moves.size)
    }

    /**
     * Wiersz: V1, wybór dalekiego terenu z listy terenów to jeden przelot z czasem z drogi, dłuższy niż ruch
     * przycisku i najwyżej 1,5 s.
     *
     * Wymagania: US1.7, NFR-56
     */
    @Test
    fun areaChoiceFliesOnce() {
        showMap()
        compose.onNode(hasClickAction() and hasContentDescription("Teren: Kampus główny. Zmień teren")).performClick()
        compose.onNode(hasText("Wittigowo") and isSelectable()).performClick()
        compose.waitForIdle()
        val flight = camera.moves.singleOrNull()
        assertEquals("Jeden przelot: ${camera.moves}", CameraMotion.FLIGHT, flight?.motion)
        assertTrue("Czas z drogi, 300–1500 ms: ${camera.moves}", (flight?.millis ?: 0L) in 301L..FLIGHT_MAX_MILLIS)
    }

    /**
     * Wiersz: V1 przy systemowym „Usuń animacje”: przelot na inny teren od razu w stanie końcowym.
     *
     * Wymagania: NFR-56
     */
    @Test
    fun noAnimationsJumps() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        showMap()
        open("T-16")
        assertTrue("Bez klatek ruchu: ${camera.moves}", camera.moves.all { it.millis == 0L })
    }

    private fun showMap(isSettled: Boolean = true): CampusMapViewModel {
        val file =
            generateSequence(Path.of("").toAbsolutePath(), Path::getParent)
                .map { it.resolve("data/buildings/pwr-buildings.geojson") }
                .first(Files::exists)
        val model = testMapViewModel(BuildingCatalogRepository(EmptyCoroutineContext) { Files.readString(file) })
        compose.setContent {
            DostepnyKampusTheme {
                CampusMapScreen(
                    onBuildingClick = { open(it, wait = false) },
                    focus = focus,
                    contentPadding = PaddingValues(bottom = cover),
                    mapView = camera.view,
                    viewModel = model,
                )
            }
        }
        compose.waitUntil { model.uiState.value.mapData != null }
        compose.waitForIdle()
        if (isSettled) camera.clear()
        return model
    }

    private fun open(symbol: String, height: Dp = Card, wait: Boolean = true) {
        val show = {
            focus = MapFocus.Building(symbol = symbol)
            cover = height
        }
        if (wait) {
            compose.runOnIdle(show)
            compose.waitForIdle()
        } else {
            show()
        }
    }
}
