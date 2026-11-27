/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.app.appMapViewModel
import io.github.dostepnykampus.app.navigation.MapScreen
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Zgłoszenie z arkusza planu 2 piętra A-1; plan z podróbki, bo plany ma tylko serwer. Wymagania: US4.6, BR-14 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickPlanObjectTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val saved = mutableListOf<SubmissionContent>()
    private var onSign: (TouchSign, String) -> Unit = { _, _ -> }
    private val map =
        CampusMapView { _, _, _, _, _, _, _, sign, modifier: Modifier ->
            onSign = sign
            Box(modifier)
        }

    /**
     * Scenariusz (przykład: arkusz schodów): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie planu ma miejsce
     *   w punkcie obiektu i piętro planu.
     */
    @Test
    fun reportsAtStairs() {
        reportAt("schody", "Zgłoś barierę przy schodach" to "Schody: Budynek A-1, 2 piętro")
    }

    /**
     * Scenariusz (przykład: arkusz toalety): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie planu ma miejsce
     *   w punkcie obiektu i piętro planu.
     */
    @Test
    fun reportsAtToilet() {
        reportAt("wc", "Zgłoś barierę przy toalecie" to "Toaleta dostosowana: Budynek A-1, 2 piętro")
    }

    /**
     * Scenariusz (przykład: arkusz windy): zgloszenie-szybkie.feature: Zgłoszenie przy obiekcie planu ma miejsce
     *   w punkcie obiektu i piętro planu.
     */
    @Test
    fun reportsAtElevator() {
        reportAt("winda", "Zgłoś barierę przy windzie" to "Winda: Budynek A-1, 2 piętro")
    }

    private fun reportAt(id: String, texts: Pair<String, String>) {
        val (button, line) = texts
        var isListened = false
        val positions = PositionSource { flow { isListened = true } }
        val mapModel = compose.activity.appMapViewModel()
        val navigator =
            compose.showQuickApp(app.quickModels(A1_CENTER, saved, positions)) { route ->
                val state by mapModel.uiState.collectAsStateWithLifecycle()
                if (route is MapRoute) {
                    MapScreen(focus = null, state = state, onListChange = {}, onSearch = {}, mapView = map, plan = PLAN)
                }
            }
        compose.runOnIdle { onSign(TouchSign.PLAN, id) }
        compose.onNode(hasText(button)).performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists("krok 1 zgłoszenia szybkiego")
        compose.onNodeWithText(line).assertExists("napis o obiekcie planu w kroku 1")
        compose.onAllNodesWithText("Zezwól na lokalizację").assertCountEquals(0)
        compose.takePhoto(navigator, app.cacheDir)
        compose.onNodeWithText("Schody i windy").performClick()
        compose.onNodeWithText("Na którym piętrze?").assertDoesNotExist()
        compose.onNodeWithText("Krok 3 z 3").assertExists("krok 3 bez pytania o piętro")
        compose.onNodeWithText("Wyślij").assertIsEnabled().performClick()
        compose.waitFor { saved.isNotEmpty() }
        val content = saved.single()
        assertEquals("punkt obiektu planu", ROOMS.first { it.id == id }.labelPoint, content.location)
        assertEquals("piętro planu", SubmissionFloor(floorId = FLOOR_2), content.floor)
        assertFalse("bieżąca pozycja nie jest ustalana", isListened)
    }

    private companion object {
        const val OFFSET = 0.00004
        const val FLOOR_2 = "A-1/p2"
        val A1_CENTER = GeoPoint(longitude = 17.0615954, latitude = 51.1074602)
        val IDS = listOf("schody", "wc", "winda")
        val KINDS = listOf(RoomCategory.STAIRCASE, RoomCategory.ACCESSIBLE_TOILET, RoomCategory.ELEVATOR)
        val AREA = GeoPolygon(listOf(listOf(A1_CENTER)))
        val LABELS = listOf("Parter", "1 piętro", "2 piętro")
        val ROOMS =
            IDS.mapIndexed { n, id ->
                val point = A1_CENTER.copy(longitude = A1_CENTER.longitude + n * OFFSET)
                Room(
                    id = id,
                    number = null,
                    function = id,
                    category = KINDS[n],
                    geometry = AREA,
                    labelPoint = point,
                    isLabelled = false,
                )
            }
        val PLAN =
            ShownPlan(
                building = "A-1",
                floors = LABELS.mapIndexed { order, label -> Floor(id = "A-1/p$order", label = label, order = order) },
                floor = FLOOR_2,
                rooms = mapOf(FLOOR_2 to RoomList("PWr", AREA, ROOMS)),
            )
    }
}
