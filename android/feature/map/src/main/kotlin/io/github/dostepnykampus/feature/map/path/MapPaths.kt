/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.path

import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString

/** Właściwość linii ścieżki z identyfikatorem odcinka z danych PWr. */
const val PATH_ID = "id"

/**
 * Dane mapy z odcinkami ścieżek dostosowanych [paths] jako liniami GeoJSON z właściwością [PATH_ID] zamiast
 * poprzednich ścieżek.
 *
 * Wymagania: US1.4
 */
fun CampusMapData.withPaths(paths: List<AccessiblePath>): CampusMapData {
    val features =
        paths.map { path ->
            val properties = buildJsonObject { put(key = PATH_ID, value = path.id) }
            Feature(LineString(path.line.map { it.toPosition() }), properties)
        }
    return copy(paths = featureCollection(features))
}
