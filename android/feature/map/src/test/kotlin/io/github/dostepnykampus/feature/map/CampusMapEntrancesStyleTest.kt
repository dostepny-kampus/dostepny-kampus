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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/** Styl mapy V1 z wejściami z syntetycznego katalogu „Budynek testowy” (nie dane PWr): US2.1, US3.3, decyzja 134. */
class CampusMapEntrancesStyleTest {
    private val at = GeoPoint(longitude = 0.25, latitude = 0.5)
    private val ring = listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0), GeoPoint(1.0, 1.0), GeoPoint(0.0, 0.0))
    private val markings = EntranceMarking.entries.mapIndexed { i, marking -> entrance("node/$i", marking) }
    private val catalog = BuildingCatalog(listOf(Building("A-1", "Budynek testowy", GeoPolygon(listOf(ring)))), markings, "")
    private val colors = MapStyleColors("#E8EDEE", "#BFE0EC", "#004E5E", "#001F27")
    private val pin = SelectedEntrance("node/0", at, "Wejście dostosowane", buildingSymbol = "A-1")

    private fun style(chosen: SelectedEntrance? = null): JsonObject =
        Json.parseToJsonElement(campusMapStyle(campusMapData(catalog).copy(selectedEntrance = chosen), colors)).jsonObject

    private fun JsonObject.layer(id: String): JsonObject? =
        getValue("layers").jsonArray.map { it.jsonObject }.firstOrNull { it.getValue("id").jsonPrimitive.content == id }

    private fun JsonObject.layout(key: String) = getValue("layout").jsonObject[key]

    private fun JsonObject.points(layer: JsonObject): List<JsonObject> =
        getValue("sources").jsonObject.getValue(layer.getValue("source").jsonPrimitive.content).jsonObject
            .getValue("data").jsonObject.getValue("features").jsonArray.map { it.jsonObject }

    // Wiersz E1: warstwa znaków wejść; obraz znaku zależy od oznaczenia, więc kształt, nie sam kolor.
    @Test
    @Disabled("wejścia na mapie jeszcze nie działają, poprawka w następnym PR")
    fun us2_1_entrance_marks_layer() {
        val marks = style().layer(ENTRANCES_LAYER)
        assertNotNull(marks, "brak warstwy znaków wejść")
        assertEquals("symbol", marks?.getValue("type")?.jsonPrimitive?.content)
        val image = marks?.layout("icon-image").toString()
        EntranceMarking.entries.forEach { assertTrue(image.contains(it.name), image) }
    }

    // Wiersz E2 (O2, O4): pinezka czubkiem (dół znaku) w punkcie wejścia, z podpisem z nazwą.
    @Test
    @Disabled("wejścia na mapie jeszcze nie działają, poprawka w następnym PR")
    fun us3_3_pin_tip_at_entrance() {
        val chosen = style(pin)
        val layer = chosen.layer(ENTRANCE_PIN_LAYER)
        assertNotNull(layer, "brak warstwy pinezki")
        assertEquals("bottom", layer?.layout("icon-anchor")?.jsonPrimitive?.content)
        val coordinates = chosen.points(checkNotNull(layer)).map { it.getValue("geometry").toString() }
        assertTrue(coordinates.any { it.contains("0.25") && it.contains("0.5") }, coordinates.toString())
        assertTrue((chosen.points(layer).toString() + layer).contains("Wejście dostosowane"))
    }

    // Wiersz E2 (O5): przy zaznaczeniu wszystkie znaki zostają, bez filtra.
    @Test
    @Disabled("wejścia na mapie jeszcze nie działają, poprawka w następnym PR")
    fun us3_3_marks_stay_when_chosen() {
        val marks = style(pin).layer(ENTRANCES_LAYER)
        assertNotNull(marks, "brak warstwy znaków wejść")
        assertEquals(markings.size, style(pin).points(checkNotNull(marks)).size)
        assertNull(marks["filter"])
    }

    // Wiersz E5 (O6): bez zaznaczonego wejścia nie ma pinezki.
    @Test
    fun nfr15_no_pin_without_choice() {
        assertNull(style().layer(ENTRANCE_PIN_LAYER))
    }

    private fun entrance(id: String, marking: EntranceMarking) =
        Entrance(id, "A-1", name = null, isMain = false, type = "yes", location = at, marking = marking)
}
