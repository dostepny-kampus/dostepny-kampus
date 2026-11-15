/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
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
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.plan.ShownPlan
import io.github.dostepnykampus.feature.map.selectionImages
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

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
    private val colors =
        MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")

    /** Zmiana zaznaczonego wejścia dodaje tylko podpis tego wejścia, bez obrazów podpisów konturów. */
    @Test
    fun addsCaptionOnly() {
        val entrance = SelectedEntrance(id = "w1", location = a, label = "Główne", buildingSymbol = "C-13")
        val images = images()
        val target = Recorded()

        val selection = data.selection(entrance = entrance)
        target.showSelection(selectionLook(data, selection, colors), selectionImages(selection, images))

        assertEquals(listOf(entranceCaptionImage("Główne")), target.images)
    }

    /** Zmiana zaznaczonego budynku nie dodaje żadnego obrazu. */
    @Test
    fun addsNoImagesForBuilding() {
        val images = images()
        val target = Recorded()

        val selection = data.selection(building = "C-13")
        target.showSelection(selectionLook(data, selection, colors), selectionImages(selection, images))

        assertEquals(emptyList<String>(), target.images)
    }

    /** Plan dodaje obrazy podpisanych numerów sal tylko wybranego piętra. Wymagania: US16.1 */
    @Test
    fun addsNumbersOfChosenFloor() {
        val rooms =
            listOf(room("329", isLabelled = true), room("330", isLabelled = false), room(null, isLabelled = true))
        val floors = mapOf("P0" to floor(rooms), "P1" to floor(listOf(room("101", isLabelled = true))))
        val plan = ShownPlan(building = "C-13", floors = emptyList(), floor = "P0", rooms = floors)
        val target = Recorded()

        val selection = data.selection().copy(plan = plan)
        target.showSelection(selectionLook(data, selection, colors), selectionImages(selection, images()))

        assertEquals("Plan nie dodał numerów sal wybranego piętra", listOf(roomNumberImage("329")), target.images)
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

    private class Recorded : MapStyleTarget {
        val images = mutableListOf<String>()

        override fun filter(layer: String, filter: JsonElement) = Unit

        override fun paint(layer: String, property: String, value: JsonElement) = Unit

        override fun minZoom(layer: String, zoom: Double) = Unit

        override fun data(source: String, geoJson: String) = Unit

        override fun image(name: String, image: Bitmap) {
            images += name
        }

        override fun basemapBounds(bounds: CampusBounds?) = Unit
    }

    private companion object {
        const val DENSITY = 2f
        const val LABEL_PX = 28f
    }
}
