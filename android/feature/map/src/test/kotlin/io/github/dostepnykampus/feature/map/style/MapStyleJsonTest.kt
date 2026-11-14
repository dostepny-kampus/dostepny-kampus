/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.feature.map.data.featureCollection
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/** Części stylu MapLibre budowane jako struktura JSON, nie jako napis (ADR-0005). */
class MapStyleJsonTest {
    /** Warstwa ma nazwę, rodzaj i źródło, a warstwa bez źródła nie ma pola `source`. */
    @Test
    fun adr5_layer_fields() {
        val layer = styleLayer(id = "a", type = "fill", source = "s") { paint("fill-color" to JsonPrimitive("#fff")) }
        assertEquals("""{"id":"a","type":"fill","source":"s","paint":{"fill-color":"#fff"}}""", layer.toString())
        assertEquals("""{"id":"b","type":"background"}""", styleLayer(id = "b", type = "background") {}.toString())
    }

    /** Warstwa linii niesie barwę i grubość, a filtr tylko wtedy, gdy jest podany. */
    @Test
    fun adr5_line_layer() {
        val filter = expression("==", property("label"), JsonPrimitive("C-13"))
        val line = lineLayer(id = "l", source = "s", color = "#111", width = 2.0, filter = filter)
        assertEquals("""["==",["get","label"],"C-13"]""", line.getValue("filter").toString())
        assertEquals("""{"line-color":"#111","line-width":2.0}""", line.getValue("paint").toString())
        assertNull(lineLayer(id = "l", source = "s", color = "#111", width = 2.0)["filter"])
    }

    /** Warstwa znaków ma obraz i pozostałe pola układu w podanej kolejności. */
    @Test
    fun adr5_symbol_layer() {
        val layer = symbolLayer(id = "z", source = "s", image = property("image"), OVERLAP)
        assertEquals("symbol", layer.getValue("type").jsonPrimitive.content)
        val layout = layer.getValue("layout")
        assertEquals("""{"icon-image":["get","image"],"icon-allow-overlap":true}""", layout.toString())
        assertEquals("[0.0,16.0]", numbers(0.0, 16.0).toString())
    }

    /** Źródło GeoJSON niesie kolekcję obiektów z ich właściwościami. */
    @Test
    fun adr5_geojson_source() {
        val properties = buildJsonObject { put(key = "label", value = "Główne") }
        val point = Point(Position(longitude = 17.0, latitude = 51.0))
        val source = geoJsonSource(featureCollection(listOf(Feature(point, properties))))
        assertEquals("geojson", source.getValue("type").jsonPrimitive.content)
        val data = source.getValue("data").jsonObject
        assertEquals("FeatureCollection", data.getValue("type").jsonPrimitive.content)
        val features = data.getValue("features")
        val feature = features.jsonArray.single().jsonObject
        assertEquals("""{"label":"Główne"}""", feature.getValue("properties").toString())
        val geometry = feature.getValue("geometry")
        assertEquals("[17.0,51.0]", geometry.jsonObject.getValue("coordinates").toString())
    }
}
