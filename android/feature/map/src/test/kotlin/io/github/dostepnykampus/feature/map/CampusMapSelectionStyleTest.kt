/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.data.SelectedEntrance
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
import kotlinx.serialization.json.Json
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
 * z wejściem każdego budynku i jednym wejściem wspólnym (US1.4, US2.1, US2.8, decyzja 153).
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
    fun us2_8_neighbours_dimmed() {
        val style = style(building = "A-1")
        val own = """["any",["in","A-1",["get","symbols"]]]"""
        val fill = style.layer(BUILDINGS_LAYER).paint("fill-color")
        val line = style.layer("kontury").paint("line-color")
        assertEquals("""["case",$own,"#BFE0EC","#D6DBDD"]""", fill)
        assertEquals("""["case",$own,"#004E5E","#C2C9CC"]""", line)
    }

    /** Wiersz: V2, „Mapa nad małą kartą”: sąsiednie budynki zachowują podpisy symboli, więc da się je wybrać. */
    @Test
    fun us2_8_neighbours_keep_labels() {
        val labels = style(building = "A-1").layer("podpisy")
        assertNull(labels["filter"])
        assertEquals(listOf("A-1", "A-2"), data.labels)
    }

    /** Wiersz: V2, „Mapa nad małą kartą”: znaki wejść ma tylko zaznaczony budynek, także wejście wspólne. */
    @Test
    fun us1_4_marks_of_selected_only() {
        val style = style(building = "A-1")
        val filter = style.layer(ENTRANCES_LAYER).getValue("filter")
        assertEquals("""["case",["any",["in","A-1",["get","symbols"]]],true,false]""", filter.toString())
        val symbols = style.entrances().associate { it.text("id") to it.getValue("symbols").toString() }
        val expected = mapOf("w1" to """["A-1"]""", "w2" to """["A-2"]""", "w3" to """["A-1","A-2"]""")
        assertEquals(expected, symbols)
    }

    /** Wiersz: V1, bez zaznaczonego budynku wszystkie budynki mają barwę PWr i wszystkie wejścia mają znak. */
    @Test
    fun us2_1_no_selection_all_marks() {
        val style = style()
        assertEquals(JsonPrimitive("#BFE0EC").toString(), style.layer(BUILDINGS_LAYER).paint("fill-color"))
        assertEquals(JsonPrimitive("#004E5E").toString(), style.layer("kontury").paint("line-color"))
        assertNull(style.layer(ENTRANCES_LAYER)["filter"])
    }

    /** Wiersz: V1, zaznaczone wejście: pozostałe wejścia i budynki zostają jak bez zaznaczenia (decyzja 134). */
    @Test
    fun us3_3_chosen_keeps_all_marks() {
        val pin = SelectedEntrance(id = "w1", location = at, label = "Wejście dostosowane", buildingSymbol = "A-1")
        val style = style(building = "A-1", entrance = pin)
        assertNull(style.layer(ENTRANCES_LAYER)["filter"])
        assertEquals(JsonPrimitive("#BFE0EC").toString(), style.layer(BUILDINGS_LAYER).paint("fill-color"))
    }

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
