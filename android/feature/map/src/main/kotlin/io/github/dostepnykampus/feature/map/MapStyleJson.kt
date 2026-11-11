/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.Geometry

/** Układ znaku, który rysuje się także wtedy, gdy nachodzi na inny znak. */
internal val OVERLAP: Pair<String, JsonElement> = "icon-allow-overlap" to JsonPrimitive(true)

/** Warstwa stylu MapLibre o nazwie [id] i rodzaju [type] ze źródła [source]; [content] dopisuje resztę pól. */
internal fun styleLayer(
    id: String,
    type: String,
    source: String? = null,
    content: JsonObjectBuilder.() -> Unit,
): JsonObject =
    buildJsonObject {
        put(key = "id", value = id)
        put(key = "type", value = type)
        source?.let { put(key = "source", value = it) }
        content()
    }

/** Warstwa linii o barwie [color] i grubości [width]; [filter] zawęża ją do części źródła. */
internal fun lineLayer(
    id: String,
    source: String,
    color: String,
    width: Double,
    filter: JsonElement? = null,
): JsonObject =
    styleLayer(id, type = "line", source = source) {
        filter?.let { put(key = "filter", element = it) }
        paint("line-color" to JsonPrimitive(color), "line-width" to JsonPrimitive(width))
    }

/** Warstwa znaków z obrazem [image]; [layout] dopisuje pozostałe pola układu. */
internal fun symbolLayer(
    id: String,
    source: String,
    image: JsonElement,
    vararg layout: Pair<String, JsonElement>,
): JsonObject =
    styleLayer(id, type = "symbol", source = source) {
        put(key = "layout", element = JsonObject(mapOf("icon-image" to image) + layout))
    }

/** Pole `paint` warstwy z wpisów [entries]. */
internal fun JsonObjectBuilder.paint(vararg entries: Pair<String, JsonElement>) {
    put(key = "paint", element = JsonObject(entries.toMap()))
}

/** Wyrażenie stylu MapLibre: nazwa operatora [name] i jego [arguments]. */
internal fun expression(name: String, vararg arguments: JsonElement): JsonArray =
    JsonArray(listOf(JsonPrimitive(name)) + arguments)

/** Wyrażenie, które czyta właściwość [name] obiektu ze źródła. */
internal fun property(name: String): JsonArray = expression("get", JsonPrimitive(name))

/** Tablica liczb [values], np. współrzędne albo przesunięcie. */
internal fun numbers(vararg values: Double): JsonArray = JsonArray(values.map { JsonPrimitive(it) })

/** Źródło GeoJSON stylu z danymi [data]. */
internal fun geoJsonSource(data: JsonElement): JsonObject =
    buildJsonObject {
        put(key = "type", value = "geojson")
        put(key = "data", element = data)
    }

/** Kolekcja GeoJSON z obiektów [features] jako element JSON. */
internal fun featureCollection(features: List<Feature<Geometry, JsonObject?>>): JsonElement =
    Json.parseToJsonElement(GeoJsonObject.toJson(FeatureCollection(features)))
