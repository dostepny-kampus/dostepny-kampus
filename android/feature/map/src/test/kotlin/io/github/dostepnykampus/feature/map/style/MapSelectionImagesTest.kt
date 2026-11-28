/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.selectionImages
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** Obrazy przy zmianie zaznaczenia na mapie „Budynek testowy” (nie dane PWr). Wymagania: US1.1, US3.3 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapSelectionImagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val a = GeoPoint(longitude = 0.0, latitude = 0.0)
    private val outline = GeoPolygon(listOf(listOf(a, a.copy(longitude = 1.0), a.copy(latitude = 1.0), a)))
    private val data =
        campusMapData(
            BuildingCatalog(
                buildings = listOf(Building(symbol = "C-13", name = "Budynek testowy", outline = outline)),
                entrances = emptyList(),
                attribution = "",
            ),
        )
    private val floors =
        mapOf(
            "P0" to
                floor(
                    listOf(
                        room("329", isLabelled = true),
                        room("330", isLabelled = false),
                        room(null, isLabelled = true),
                    ),
                ),
            "P1" to floor(listOf(room("101", isLabelled = true), room("329", isLabelled = true))),
        )

    /** Zmiana zaznaczonego wejścia dodaje tylko podpis tego wejścia, bez obrazów podpisów konturów. */
    @Test
    fun addsCaptionOnly() {
        val entrance = SelectedEntrance(id = "w1", location = a, label = "Główne", buildingSymbol = "C-13")
        val images = images()
        val target = RecordingStyleTarget()

        val selection = data.selection(entrance = entrance)
        target.showSelection(selectionLook(data, selection), selectionImages(selection, images))

        assertEquals(listOf(pointCaptionImage("Główne")), target.images)
    }

    /** Zmiana zaznaczonego budynku nie dodaje żadnego obrazu. */
    @Test
    fun addsNoImagesForBuilding() {
        val images = images()
        val target = RecordingStyleTarget()

        val selection = data.selection(building = "C-13")
        target.showSelection(selectionLook(data, selection), selectionImages(selection, images))

        assertEquals(emptyList<String>(), target.images)
    }

    /**
     * Dane planu dodają obrazy podpisanych numerów sal wszystkich pięter raz: ponowne podanie planu nie rysuje ich
     * od nowa. Wymagania: US16.1, NFR-10
     */
    @Test
    fun addsNumbersOfAllFloorsOnce() {
        val images = images()
        val target = RecordingStyleTarget()
        val numbered = mutableSetOf<String>()

        repeat(2) {
            runBlocking {
                target.showPlanData(
                    floors = floors,
                    images = images,
                    numbered = numbered,
                    worker = EmptyCoroutineContext,
                )
            }
        }

        val expected = listOf(roomNumberImage("329"), roomNumberImage("101"))
        assertEquals("Plan nie dodał numerów sal raz", expected, target.images)
        assertEquals("Plan nie dostał danych", listOf(PLAN_SOURCE, PLAN_SOURCE), target.sources)
    }

    /** Dane planu trafiają do stylu przed obrazami numerów sal. Wymagania: US16.1, NFR-10 */
    @Test
    fun givesPlanDataBeforeNumbers() {
        val images = images()
        val target = RecordingStyleTarget()

        runBlocking {
            target.showPlanData(
                floors = floors,
                images = images,
                numbered = mutableSetOf(),
                worker = EmptyCoroutineContext,
            )
        }

        assertEquals("Plan czekał na obrazy numerów", PLAN_SOURCE, target.calls.first())
    }

    /** Zmiana piętra planu zmienia filtry, bez nowych danych planu i bez obrazów. Wymagania: US16.1, NFR-10 */
    @Test
    fun keepsPlanDataOnFloor() {
        val images = images()
        val target = RecordingStyleTarget()

        listOf("P0", "P1").forEach { floor ->
            val plan = ShownPlan(building = "C-13", floors = emptyList(), floor = floor, rooms = floors)
            val selection = data.selection().copy(plan = plan)
            target.showSelection(selectionLook(data, selection), selectionImages(selection, images))
        }

        assertEquals("Zmiana piętra dodała obrazy", emptyList<String>(), target.images)
        assertFalse("Zmiana piętra podała dane planu", PLAN_SOURCE in target.sources)
    }

    private fun floor(rooms: List<Room>) =
        RoomList(source = "", floorOutline = GeoMultiPolygon(emptyList()), rooms = rooms)

    private fun room(number: String?, isLabelled: Boolean) =
        Room(
            id = number.orEmpty(),
            number = number,
            function = "Sala",
            category = RoomCategory.CLASSROOM,
            geometry = GeoMultiPolygon(emptyList()),
            labelPoint = a,
            isLabelled = isLabelled,
        )

    private fun images(): MapImages {
        var palette: MapColors? = null
        compose.setContent { DostepnyKampusTheme { palette = DostepnyKampusTheme.mapColors } }
        compose.waitForIdle()
        return MapImages(colors = checkNotNull(palette), density = DENSITY, labelSize = LABEL_PX)
    }

    private companion object {
        const val DENSITY = 2f
        const val LABEL_PX = 28f
    }
}
