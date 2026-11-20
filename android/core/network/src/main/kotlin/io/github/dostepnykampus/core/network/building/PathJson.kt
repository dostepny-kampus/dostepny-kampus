/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.building

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import io.github.dostepnykampus.core.model.path.AccessiblePathList
import io.github.dostepnykampus.core.network.decodeContract
import kotlinx.serialization.json.Json
import io.github.dostepnykampus.core.network.json.AccessiblePathList as AccessiblePathListJson
import io.github.dostepnykampus.core.network.json.GeoLineString as GeoLineStringJson

/**
 * Odcinki ścieżek dostosowanych z [body] (`AccessiblePathList`). Linia, która nie jest `LineString`
 * z co najmniej dwoma punktami, to zła treść jak w [decodeBuildingList]. Wymagania: US1.4
 */
internal fun Json.decodeAccessiblePaths(body: String): AccessiblePathList {
    val list = decodeContract(AccessiblePathListJson.serializer(), body)
    return AccessiblePathList(
        source = list.source.value,
        paths = list.paths.map { AccessiblePath(id = it.id, line = it.line.toModel()) },
    )
}

private fun GeoLineStringJson.toModel(): List<GeoPoint> {
    require(type == "LineString" && coordinates.size >= MIN_POINTS) { "Zła linia GeoJSON" }
    return coordinates.map { position ->
        require(position.size == POSITION_SIZE) { "Zły punkt linii GeoJSON" }
        GeoPoint(longitude = position[0], latitude = position[1])
    }
}

private const val MIN_POINTS = 2
private const val POSITION_SIZE = 2
