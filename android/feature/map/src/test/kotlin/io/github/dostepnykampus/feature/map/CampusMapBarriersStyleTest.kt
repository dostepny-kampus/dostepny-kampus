/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.data.BARRIER_ID
import io.github.dostepnykampus.feature.map.data.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.campusMapData
import io.github.dostepnykampus.feature.map.data.selection
import io.github.dostepnykampus.feature.map.data.withBarriers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Warstwy barier w stylu mapy V1 na konturze „Budynek testowy” (nie dane PWr; US1.3, NFR-20, NFR-21). */
class CampusMapBarriersStyleTest {
    private val a = GeoPoint(longitude = 0.0, latitude = 0.0)
    private val outline = GeoPolygon(listOf(listOf(a, a.copy(longitude = 1.0), a.copy(latitude = 1.0), a)))
    private val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    private val catalog = BuildingCatalog(buildings = listOf(building), entrances = emptyList(), attribution = "")
    private val colors =
        MapStyleColors(land = "#E8EDEE", building = "#BFE0EC", buildingOutline = "#004E5E", selectedOutline = "#001F27")
    private val barriers = Severity.entries.map(::barrier)

    /** Wiersz: V1, bariery ze zgłoszeń przyjętych jako znak uciążliwości: kształt, nie kolor (US1.3, NFR-21). */
    @Test
    fun us1_3_sign_per_severity() {
        val style = style(campusMapData(catalog).withBarriers(barriers))
        val points = style.points()
        val severities = points.map { it.jsonObject.obj("properties").text(BARRIER_SEVERITY) }
        assertEquals(Severity.entries.map(Severity::name), severities)
        val signs = style.layer(BARRIERS_LAYER).obj("layout")
        val images = signs.getValue("icon-image").jsonArray.map { it.toString() }
        assertEquals(true, images.containsAll(Severity.entries.map { JsonPrimitive(barrierSignImage(it)).toString() }))
        assertEquals("true", signs.getValue("icon-allow-overlap").toString())
    }

    /** Wiersz: V1, słowo obok znaku od powiększenia 17, bez nakładania; symbole budynków pierwsze (NFR-20). */
    @Test
    fun nfr20_word_from_zoom_17() {
        val style = style(campusMapData(catalog).withBarriers(barriers))
        val words = style.layer(BARRIER_WORDS_LAYER)
        assertEquals(BARRIER_WORD_ZOOM, words.getValue("minzoom").jsonPrimitive.int)
        assertNull(words.obj("layout")["icon-allow-overlap"])
        assertEquals("true", words.obj("layout").getValue("symbol-avoid-edges").toString())
        val order = style.getValue("layers").jsonArray.map { it.jsonObject.text("id") }
        assertEquals(true, order.indexOf(BARRIER_WORDS_LAYER) < order.indexOf(BARRIERS_LAYER))
        assertEquals(true, order.indexOf(BARRIERS_LAYER) < order.indexOf("podpisy"))
    }

    /** Wiersz: V1, wybrana bariera ma osobną warstwę z większym znakiem nad pozostałymi (US1.5, NFR-21). */
    @Test
    fun us1_5_chosen_barrier_layer() {
        val data = campusMapData(catalog).withBarriers(barriers)
        val chosen = barriers.first().id
        val plain = Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject
        val marked = data.selection().copy(barrier = chosen)
        val style = Json.parseToJsonElement(campusMapStyle(data, marked, colors)).jsonObject
        val order = style.getValue("layers").jsonArray.map { it.jsonObject.text("id") }
        assertEquals(false, plain.getValue("layers").jsonArray.any { it.jsonObject.text("id") == CHOSEN_BARRIER_LAYER })
        assertEquals(true, order.indexOf(BARRIERS_LAYER) < order.indexOf(CHOSEN_BARRIER_LAYER))
        val layer = style.layer(CHOSEN_BARRIER_LAYER)
        assertEquals("""["==",["get","$BARRIER_ID"],"$chosen"]""", layer.getValue("filter").toString())
        val images = layer.obj("layout").getValue("icon-image").jsonArray.map { it.toString() }
        val expected = Severity.entries.map { JsonPrimitive(chosenBarrierImage(it)).toString() }
        assertEquals(true, images.containsAll(expected))
    }

    /** Wiersz: V1 bez pobranych barier działa bez znaków barier (NFR-11). */
    @Test
    fun nfr11_no_barriers_empty_layer() {
        val shown = campusMapData(catalog).withBarriers(barriers).withBarriers(emptyList())
        assertEquals(0, style(shown).points().size)
    }

    private fun style(data: CampusMapData): JsonObject =
        Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject

    private fun barrier(severity: Severity) =
        AcceptedBarrier(
            id = severity.name,
            location = a,
            severity = severity,
            type = null,
            buildingSymbol = "C-13",
            verifiedAt = null,
        )

    private fun JsonObject.layer(id: String): JsonObject =
        getValue("layers").jsonArray.map { it.jsonObject }.single { it.text("id") == id }

    private fun JsonObject.points(): JsonArray {
        val source = obj("sources").obj(BARRIERS_SOURCE)
        return source.obj("data").getValue("features").jsonArray
    }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
