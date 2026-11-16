/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.plan.ShownPlan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Styl mapy przy zaznaczonym budynku: syntetyczny katalog „Budynek testowy” i „Budynek obok” (nie dane PWr)
 * z wejściem każdego budynku i jednym wejściem wspólnym.
 *
 * Wymagania: US1.4, US2.1, US2.8
 */
class CampusMapSelectionStyleTest {
    private val at = GeoPoint(longitude = 0.25, latitude = 0.5)
    private val catalog =
        BuildingCatalog(
            buildings = listOf(building("A-1", name = "Budynek testowy"), building("A-2", name = "Budynek obok")),
            entrances =
                listOf("w1" to "A-1", "w2" to "A-2", "w3" to "A-1", "w3" to "A-2").map { (id, symbol) ->
                    entrance(id = id, symbol = symbol)
                },
            attribution = "",
        )
    private val colors =
        MapStyleColors(
            land = "#E8EDEE",
            building = "#BFE0EC",
            buildingOutline = "#004E5E",
            selectedOutline = "#001F27",
            dimmed = "#D6DBDD",
            dimmedOutline = "#C2C9CC",
        )
    private val data = campusMapData(catalog)

    /** Wiersz: V2, „Mapa nad małą kartą”: zaznaczony budynek ma barwę PWr, sąsiednie są przygaszone. */
    @Test
    fun dimsNeighbours() {
        val style = style(building = "A-1")
        val own = """["any",["in","A-1",["get","symbols"]]]"""
        val fill = style.layer(BUILDINGS_LAYER).paint("fill-color")
        val line = style.layer("kontury").paint("line-color")
        assertEquals("""["case",$own,"#BFE0EC","#D6DBDD"]""", fill)
        assertEquals("""["case",$own,"#004E5E","#C2C9CC"]""", line)
    }

    /** Wiersz: V2, „Mapa nad małą kartą”: sąsiednie budynki zachowują podpisy symboli, więc da się je wybrać. */
    @Test
    fun keepsNeighbourLabels() {
        val labels = style(building = "A-1").layer("podpisy")
        assertNull(labels["filter"])
        assertEquals(listOf("A-1", "A-2"), data.labels)
    }

    /** Wiersz: V2, „Mapa nad małą kartą”: znaki wejść ma tylko zaznaczony budynek, także wejście wspólne. */
    @Test
    fun marksSelectedOnly() {
        val style = style(building = "A-1")
        val filter = style.layer(ENTRANCES_LAYER).getValue("filter")
        assertEquals("""["case",["any",["in","A-1",["get","symbols"]]],true,false]""", filter.toString())
        val symbols = style.entrances().associate { it.text("id") to it.getValue("symbols").toString() }
        val expected = mapOf("w1" to """["A-1"]""", "w2" to """["A-2"]""", "w3" to """["A-1","A-2"]""")
        assertEquals(expected, symbols)
    }

    /** Wiersz: V1, bez zaznaczonego budynku wszystkie budynki mają barwę PWr i wszystkie wejścia mają znak. */
    @Test
    fun marksAllWithoutSelection() {
        val style = style()
        assertEquals(JsonPrimitive("#BFE0EC").toString(), style.layer(BUILDINGS_LAYER).paint("fill-color"))
        assertEquals(JsonPrimitive("#004E5E").toString(), style.layer("kontury").paint("line-color"))
        assertEquals("""["!",false]""", style.layer(ENTRANCES_LAYER).getValue("filter").toString())
    }

    /** Wiersz: V1, bez zaznaczonego budynku znaki wejść od powiększenia 17, a zaznaczony budynek ma je zawsze. */
    @Test
    fun showsMarksFromZoom17() {
        assertEquals("17.0", style().layer(ENTRANCES_LAYER).getValue("minzoom").toString())
        assertEquals("0.0", style(building = "A-1").layer(ENTRANCES_LAYER).getValue("minzoom").toString())
    }

    /** Wiersz: V1, zaznaczone wejście: pozostałe wejścia i budynki zostają jak bez zaznaczenia. */
    @Test
    fun keepsAllMarksWhenChosen() {
        val pin = SelectedEntrance(id = "w1", location = at, label = "Wejście dostosowane", buildingSymbol = "A-1")
        val style = style(building = "A-1", entrance = pin)
        assertEquals("""["!",false]""", style.layer(ENTRANCES_LAYER).getValue("filter").toString())
        assertEquals(JsonPrimitive("#BFE0EC").toString(), style.layer(BUILDINGS_LAYER).paint("fill-color"))
    }

    /**
     * Zaznaczenie pokazane w gotowym stylu daje ten sam styl co styl zbudowany z zaznaczeniem, więc zmiana
     * budynku, wejścia, bariery albo piętra planu nie wymaga nowego stylu, a mapa wygląda tak samo. Dane planu są
     * już w gotowym stylu z innym piętrem. Wymagania: US1.1, US1.5, US3.3, US16.1
     */
    @Test
    fun selectsOnReadyStyle() {
        val chosen = SelectedEntrance(id = "w1", location = at, label = "Główne", buildingSymbol = "A-1")
        val selections =
            listOf(
                data.selection(),
                data.selection(building = "A-1"),
                data.selection(building = "A-2", entrance = chosen),
                data.selection().copy(barrier = "b1"),
                data.selection().copy(plan = plan()),
            )
        selections.forEach { selection ->
            val shown = MapSelection(bounds = selection.bounds, plan = selection.plan?.copy(floor = "P9"))
            val ready = JsonStyle(campusMapStyle(data, shown, colors))
            ready.showSelection(selectionLook(data, selection, colors))
            assertEquals(Json.parseToJsonElement(campusMapStyle(data, selection, colors)), ready.json, "$selection")
        }
    }

    /**
     * Podkład terenu pokazany w gotowym stylu daje ten sam styl co styl zbudowany z nim, więc zmiana terenu
     * nie wczytuje stylu od nowa, a mapa wygląda tak samo. Wymagania: US1.7, NFR-39
     */
    @Test
    fun showsAreaOnReadyStyle() {
        val main = CampusBounds(west = 0.0, south = 0.0, east = 1.0, north = 1.0)
        val other = CampusBounds(west = 2.0, south = 0.0, east = 3.0, north = 1.0)
        val areas =
            listOf(
                MapSelection() to null,
                MapSelection(bounds = main) to Basemap(),
                MapSelection(bounds = other, boundary = building("A-1", name = "").outline) to
                    Basemap(minZoom = 12.0),
            )
        val ready = JsonStyle(styleOf(area = MapSelection(bounds = main), basemap = Basemap()))
        areas.forEach { (area, basemap) ->
            ready.showBasemap(basemap = basemap)
            assertEquals(Json.parseToJsonElement(styleOf(area = area, basemap = basemap)), ready.json, "$area")
        }
    }

    private fun styleOf(area: MapSelection, basemap: Basemap?): String =
        campusMapStyle(data = data, selection = area, colors = colors, basemap = basemap)

    private fun style(building: String? = null, entrance: SelectedEntrance? = null): JsonObject =
        Json.parseToJsonElement(campusMapStyle(data, data.selection(building, entrance), colors)).jsonObject

    private fun JsonObject.layer(id: String): JsonObject =
        getValue("layers").jsonArray.map { it.jsonObject }.single { it.text("id") == id }

    private fun JsonObject.paint(key: String): String = getValue("paint").jsonObject.getValue(key).toString()

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.entrances(): List<JsonObject> {
        val sources = getValue("sources").jsonObject
        val data = sources.getValue(ENTRANCES_LAYER).jsonObject.getValue("data")
        val features = data.jsonObject.getValue("features").jsonArray
        return features.map { it.jsonObject.getValue("properties").jsonObject }
    }

    private fun plan(): ShownPlan {
        val floor = RoomList(source = "", floorOutline = building("A-1", name = "").outline, rooms = emptyList())
        return ShownPlan(building = "A-1", floors = emptyList(), floor = "P0", rooms = mapOf("P0" to floor))
    }

    private fun building(symbol: String, name: String): Building {
        val shift = symbol.last().digitToInt().toDouble()
        val corner = GeoPoint(longitude = shift, latitude = 0.0)
        val ring = listOf(corner, corner.copy(longitude = shift + 0.5), corner.copy(latitude = 0.5), corner)
        return Building(symbol = symbol, name = name, outline = GeoPolygon(listOf(ring)))
    }

    private fun entrance(id: String, symbol: String) =
        Entrance(
            id = id,
            buildingSymbol = symbol,
            name = null,
            isMain = false,
            type = "yes",
            location = at,
            marking = EntranceMarking.NO_DATA,
        )
}

/** Styl jako JSON, w którym [MapStyleTarget] zmienia pola tak jak widok mapy w stylu MapLibre. */
private class JsonStyle(style: String) : MapStyleTarget {
    var json: JsonObject = Json.parseToJsonElement(style).jsonObject

    override fun filter(layer: String, filter: JsonElement) = edit(layer) { it + ("filter" to filter) }

    override fun paint(layer: String, property: String, value: JsonElement) =
        edit(layer) { found ->
            val paint = found.getValue("paint").jsonObject + (property to value)
            found + ("paint" to JsonObject(paint))
        }

    override fun minZoom(layer: String, zoom: Double) = edit(layer) { it + ("minzoom" to JsonPrimitive(zoom)) }

    override fun data(source: String, geoJson: String) {
        val sources = json.getValue("sources").jsonObject
        val changed = sources.getValue(source).jsonObject + ("data" to Json.parseToJsonElement(geoJson))
        json = JsonObject(json + ("sources" to JsonObject(sources + (source to JsonObject(changed)))))
    }

    override fun image(name: String, image: Bitmap) = Unit

    private fun edit(layer: String, change: (JsonObject) -> Map<String, JsonElement>) {
        val layers =
            json.getValue("layers").jsonArray.map { element ->
                val found = element.jsonObject
                if (found.getValue("id").jsonPrimitive.content == layer) JsonObject(change(found)) else found
            }
        json = JsonObject(json + ("layers" to JsonArray(layers)))
    }
}
