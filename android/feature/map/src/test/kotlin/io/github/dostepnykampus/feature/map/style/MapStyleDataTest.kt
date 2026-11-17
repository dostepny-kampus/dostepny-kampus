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
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.unitSquareBuilding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val LABEL_PX = 16f

/** Dane katalogu i barier idą do wczytanego stylu, a nie w tekst stylu. Wymagania: NFR-11 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapStyleDataTest {
    private val colors =
        MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val worker = UnconfinedTestDispatcher()

    /** Styl bez danych ma te same źródła co pełny styl, ale źródła danych są puste. Wymagania: NFR-11 */
    @Test
    fun buildsShellWithEmptySources() {
        val shell = sourcesOf(campusMapShell(selection = MapSelection(), colors = colors, basemap = null))
        val full = sourcesOf(fullStyle(dataOf("C-13")))
        assertEquals("źródła stylu", full.keys, shell.keys)
        dataOf("C-13").sourceTexts().keys.forEach { source ->
            assertEquals("puste źródło $source", "[]", shell.getValue(source).features())
        }
    }

    /** Teksty źródeł po wczytaniu dają te same dane co pełny styl. Wymagania: NFR-11, US1.1 */
    @Test
    fun showsSameDataAsFullStyle() {
        val data = dataOf("C-13")
        val full = sourcesOf(fullStyle(data))
        val target = RecordingStyleTarget()
        val images = images()
        runBlocking { target.showData(data = data, images = images, labelled = mutableSetOf(), worker = worker) }
        target.data.forEach { (source, text) ->
            assertEquals("dane źródła $source", full.getValue(source).jsonObject["data"], Json.parseToJsonElement(text))
        }
        assertEquals("źródła z danymi", data.sourceTexts().keys, target.data.keys)
    }

    /** Obraz podpisu powstaje raz: kolejny katalog dodaje tylko nowe podpisy. Wymagania: NFR-11 */
    @Test
    fun addsOnlyNewLabelImages() {
        val target = RecordingStyleTarget()
        val labelled = mutableSetOf<String>()
        val images = images()
        runBlocking {
            listOf("C-13", "C-14", "C-13").forEach { symbol ->
                target.showData(data = dataOf(symbol), images = images, labelled = labelled, worker = worker)
            }
        }
        assertEquals("obrazy podpisów", listOf("C-13", "C-14"), target.images)
    }

    private fun dataOf(vararg symbols: String): CampusMapData =
        campusMapData(
            BuildingCatalog(
                buildings = symbols.map(::unitSquareBuilding),
                entrances = emptyList(),
                attribution = "",
            ),
        )

    private fun images(): MapImages {
        var palette: MapColors? = null
        compose.setContent { DostepnyKampusTheme { palette = DostepnyKampusTheme.mapColors } }
        compose.waitForIdle()
        return MapImages(colors = checkNotNull(palette), density = 1f, labelSize = LABEL_PX)
    }

    private fun sourcesOf(style: String): Map<String, JsonElement> {
        val root = Json.parseToJsonElement(style).jsonObject
        return root.getValue("sources").jsonObject
    }

    private fun JsonElement.features(): String {
        val data = jsonObject.getValue("data").jsonObject
        return data.getValue("features").toString()
    }

    private fun fullStyle(data: CampusMapData): String =
        campusMapStyle(data = data, selection = MapSelection(), colors = colors, basemap = null)
}
