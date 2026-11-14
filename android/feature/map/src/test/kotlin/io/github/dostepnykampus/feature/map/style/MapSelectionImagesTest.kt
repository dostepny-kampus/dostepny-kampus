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
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.MapImages
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
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
    fun us3_3_caption_only() {
        val entrance = SelectedEntrance(id = "w1", location = a, label = "Główne", buildingSymbol = "C-13")
        val images = images()
        val target = Recorded()

        val look = selectionLook(data, data.selection(entrance = entrance), colors)
        target.showSelection(look, selectionImages(entrance, images))

        assertEquals(listOf(entranceCaptionImage("Główne")), target.images)
    }

    /** Zmiana zaznaczonego budynku nie dodaje żadnego obrazu. */
    @Test
    fun us1_1_building_adds_no_images() {
        val images = images()
        val target = Recorded()

        val look = selectionLook(data, data.selection(building = "C-13"), colors)
        target.showSelection(look, selectionImages(null, images))

        assertEquals(emptyList<String>(), target.images)
    }

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

        override fun data(source: String, geoJson: String) = Unit

        override fun image(name: String, image: Bitmap) {
            images += name
        }
    }

    private companion object {
        const val DENSITY = 2f
        const val LABEL_PX = 28f
    }
}
