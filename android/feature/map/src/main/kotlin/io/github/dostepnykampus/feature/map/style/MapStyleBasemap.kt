/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Kafelki wektorowe podkładu OpenFreeMap w schemacie OpenMapTiles, pobierane tylko online (ADR-0014). */
internal const val BASEMAP_TILES = "https://tiles.openfreemap.org/planet"

/** Źródło podkładu w stylu mapy. */
internal const val BASEMAP = "podklad"

/** Powiększenie, od którego mapa pokazuje budynki miasta. */
internal const val CITY_ZOOM = 13.0

/** Powiększenie, od którego mapa pokazuje chodniki. */
internal const val STREET_ZOOM = 15.0

/** Pole klasy obiektu w schemacie OpenMapTiles. */
internal const val CLASS = "class"

/** Warstwa źródła z liniami dróg, ścieżek i torów. */
internal const val TRANSPORTATION = "transportation"

/** Warstwa źródła z liniami rzek. */
internal const val WATERWAY = "waterway"

private const val CITY_BUILDINGS = "budynki_miasta"
private const val PATHS = "chodniki"
private val OWN_MIN_ZOOM = mapOf(CITY_BUILDINGS to CITY_ZOOM, PATHS to STREET_ZOOM)
private val GREEN_CLASSES = listOf("grass", "wood", "farmland", "wetland")
private val PATH_CLASSES = listOf("path", "track")
private val RAIL_CLASSES = listOf("rail", "transit")

/**
 * Kolory podkładu wektorowego z tokenów `mapa.kolory`, jako `#RRGGBB`.
 *
 * Wymagania: NFR-16
 *
 * @property green parki i trawniki (`zielen`)
 * @property water rzeka i kanały (`woda`)
 * @property road wypełnienie ulic (`droga`)
 * @property roadCasing obrzeże ulic (`droga_kontur`)
 * @property rail tory (`tory`)
 * @property path chodniki i place (`chodnik`)
 * @property building budynki spoza PWr (`budynek`)
 * @property buildingOutline kontur budynków spoza PWr (`budynek_kontur`)
 */
data class BasemapColors(
    val green: String,
    val water: String,
    val road: String,
    val roadCasing: String,
    val rail: String,
    val path: String,
    val building: String,
    val buildingOutline: String,
)

/** Źródło kafelków wektorowych podkładu w stylu JSON. Wymagania: NFR-39 */
internal fun basemapSource(): JsonObject =
    buildJsonObject {
        put(key = "type", value = "vector")
        put(key = "url", value = BASEMAP_TILES)
    }

/** Nazwy warstw podkładu w kolejności rysowania; [showBasemap] zmienia ich powiększenie. */
internal val BASEMAP_LAYERS: List<String>
    get() =
        listOf(
            "zielen",
            "parki",
            "woda",
            "rzeki",
            PATHS,
            "obrzeza_ulic",
            "ulice",
            "tory",
            CITY_BUILDINGS,
        )

/**
 * Najmniejsze powiększenie warstwy podkładu [layer] na terenie, którego podkład zaczyna się od [area]. Poniżej niego
 * MapLibre warstwy nie rysuje i nie pobiera do niej kafelków.
 *
 * Wymagania: NFR-39
 */
internal fun basemapMinZoom(layer: String, area: Double): Double = maxOf(a = area, b = OWN_MIN_ZOOM[layer] ?: 0.0)

/**
 * Warstwy podkładu wektorowego w kolorach [colors], każda od powiększenia [basemapMinZoom] dla [minZoom]. Mapa nie
 * ma punktów usług, numerów domów ani napisów: tylko zieleń, woda, ulice, chodniki, tory i budynki miasta.
 *
 * Wymagania: NFR-16, NFR-39, US1.4
 */
internal fun basemapLayers(colors: BasemapColors, minZoom: Double): List<JsonObject> {
    val layers =
        listOf(
            basemapFill(id = "zielen", layer = "landcover", color = colors.green, filter = classIn(GREEN_CLASSES)),
            basemapFill(id = "parki", layer = "park", color = colors.green),
            basemapFill(id = "woda", layer = "water", color = colors.water),
            basemapLine(id = "rzeki", layer = WATERWAY, color = colors.water, width = riverWidth()),
            basemapLine(id = PATHS, layer = TRANSPORTATION, color = colors.path, width = pathWidth())
                .filtered(PATH_CLASSES),
            basemapRoads(id = "obrzeza_ulic", color = colors.roadCasing, widths = CASING_WIDTHS),
            basemapRoads(id = "ulice", color = colors.road, widths = ROAD_WIDTHS),
            basemapLine(id = "tory", layer = TRANSPORTATION, color = colors.rail, width = railWidth())
                .filtered(RAIL_CLASSES),
            cityBuildings(id = CITY_BUILDINGS, colors = colors),
        )
    return layers.map { layer ->
        val id = layer.getValue("id").jsonPrimitive.content
        JsonObject(layer + ("minzoom" to JsonPrimitive(basemapMinZoom(layer = id, area = minZoom))))
    }
}

/** Filtr obiektów źródła, których klasa OpenMapTiles jest jedną z [names]. */
internal fun classIn(names: List<String>): JsonArray =
    expression("in", property(CLASS), expression("literal", JsonArray(names.map { JsonPrimitive(it) })))

private fun JsonObject.filtered(classes: List<String>): JsonObject = JsonObject(this + ("filter" to classIn(classes)))
