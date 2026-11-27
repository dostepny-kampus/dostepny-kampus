/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

private const val HALO_WIDTH = 2.0
private const val DISTRICT_SPACING = 0.08
private const val DISTRICT_MAX_WIDTH = 8.0
private const val RIVER_PADDING = 40.0
private const val STREET_PADDING = 8.0
private const val REGULAR = "Noto Sans Regular"
private const val ITALIC = "Noto Sans Italic"
private const val PLACEMENT = "symbol-placement"
private val LINE = JsonPrimitive("line")
private val NAMED_STREETS = listOf("primary", "secondary", "tertiary", "minor")

/** Glify napisów podkładu z OpenFreeMap (ADR-0014). */
public const val BASEMAP_GLYPHS: String = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

/** Warstwa nazw ulic: od powiększenia [STREET_ZOOM], jak chodniki. */
public const val STREET_NAMES: String = "nazwy_ulic"
private val DISTRICT_CLASSES = listOf("suburb", "quarter", "neighbourhood")

/**
 * Warstwy napisów podkładu w kolorach [colors], z tekstem wysokim na [textSize] (dp mapy): nazwy rzek, ulic
 * i dzielnic. Wymagania: NFR-16, US1.4
 */
public fun basemapNames(colors: BasemapColors, textSize: Double): List<JsonObject> =
    listOf(riverNames(colors, textSize), streetNames(colors, textSize), districtNames(colors, textSize))

private fun riverNames(colors: BasemapColors, textSize: Double): JsonObject =
    names(
        id = "nazwy_rzek",
        layer = WATERWAY,
        filter = expression("==", property(CLASS), JsonPrimitive("river")),
        layout = textLayout(font = ITALIC, size = textSize, padding = RIVER_PADDING) + (PLACEMENT to LINE),
        paint = textPaint(color = colors.waterName, halo = colors.halo),
    )

private fun streetNames(colors: BasemapColors, textSize: Double): JsonObject =
    names(
        id = STREET_NAMES,
        layer = "transportation_name",
        filter = classIn(NAMED_STREETS),
        layout = textLayout(font = REGULAR, size = textSize, padding = STREET_PADDING) + (PLACEMENT to LINE),
        paint = textPaint(color = colors.streetName, halo = colors.halo),
    )

private fun districtNames(colors: BasemapColors, textSize: Double): JsonObject =
    names(
        id = "dzielnice",
        layer = "place",
        filter = classIn(DISTRICT_CLASSES),
        layout =
            textLayout(font = REGULAR, size = textSize, padding = 0.0) +
                mapOf(
                    "text-transform" to JsonPrimitive("uppercase"),
                    "text-letter-spacing" to JsonPrimitive(DISTRICT_SPACING),
                    "text-max-width" to JsonPrimitive(DISTRICT_MAX_WIDTH),
                ),
        paint = textPaint(color = colors.districtName, halo = colors.halo),
    )

private fun names(
    id: String,
    layer: String,
    filter: JsonElement,
    layout: Map<String, JsonElement>,
    paint: Map<String, JsonElement>,
): JsonObject =
    styleLayer(id, type = "symbol", source = BASEMAP) {
        put(key = "source-layer", value = layer)
        put(key = "filter", element = filter)
        put(key = "layout", element = JsonObject(layout))
        put(key = "paint", element = JsonObject(paint))
    }

private fun textLayout(font: String, size: Double, padding: Double): Map<String, JsonElement> =
    mapOf(
        "text-field" to property("name"),
        "text-font" to JsonArray(listOf(JsonPrimitive(font))),
        "text-size" to JsonPrimitive(size),
        "text-padding" to JsonPrimitive(padding),
    )

private fun textPaint(color: String, halo: String): Map<String, JsonElement> =
    mapOf(
        "text-color" to JsonPrimitive(color),
        "text-halo-color" to JsonPrimitive(halo),
        "text-halo-width" to JsonPrimitive(HALO_WIDTH),
    )
