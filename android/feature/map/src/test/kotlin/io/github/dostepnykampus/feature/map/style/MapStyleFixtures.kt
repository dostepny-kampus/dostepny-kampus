/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val STYLE_ORIGIN = GeoPoint(longitude = 0.0, latitude = 0.0)

internal fun styleCatalog(): BuildingCatalog {
    val corner = STYLE_ORIGIN
    val outline =
        GeoPolygon(listOf(listOf(corner, corner.copy(longitude = 1.0), corner.copy(latitude = 1.0), corner)))
    val building = Building(symbol = "C-13", name = "Budynek testowy", outline = outline)
    return BuildingCatalog(buildings = listOf(building), entrances = emptyList(), attribution = "")
}

internal fun styleColors(): MapStyleColors =
    MapStyleColors(
        land = "#E8EDEE",
        building = "#BFE0EC",
        buildingOutline = "#004E5E",
        selectedOutline = "#001F27",
        basemap =
            BasemapColors(
                green = "#CBE2C6",
                water = "#A7CADD",
                road = "#FFFFFF",
                roadCasing = "#C3CBCE",
                rail = "#A3ACAF",
                path = "#F7F9FA",
                building = "#DADFE0",
                buildingOutline = "#C6CDCF",
                streetName = "#3A4549",
                waterName = "#2B454D",
                districtName = "#4E5A5E",
                halo = "#FFFFFF",
            ),
    )

/** Budynek testowy [symbol] z konturem przesuniętym o ostatnią cyfrę symbolu w długości, nie dane PWr. */
internal fun shiftedBuilding(symbol: String): Building {
    val shift = symbol.last().digitToInt().toDouble()
    val corner = GeoPoint(longitude = shift, latitude = 0.0)
    val ring = listOf(corner, corner.copy(longitude = shift + 0.5), corner.copy(latitude = 0.5), corner)
    return Building(symbol = symbol, name = "Budynek testowy $symbol", outline = GeoPolygon(listOf(ring)))
}

/** Warstwa [id] stylu JSON. */
internal fun JsonObject.layerById(id: String): JsonObject =
    getValue("layers").jsonArray.map { it.jsonObject }.single { it.getValue("id").jsonPrimitive.content == id }

/** Pole rysowania [key] warstwy jako tekst JSON. */
internal fun JsonObject.paintText(key: String): String = getValue("paint").jsonObject.getValue(key).toString()
