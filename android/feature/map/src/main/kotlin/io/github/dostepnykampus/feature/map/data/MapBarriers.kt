/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.geo.toPosition
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point

/** Właściwość punktu bariery z nazwą uciążliwości: `HINDRANCE`, `ONLY_WITH_HELP` albo `BLOCKING`. */
const val BARRIER_SEVERITY = "severity"

/**
 * Te same dane mapy z barierami [barriers] jako punktami warstwy barier (US1.3, BR-6). Pusta lista czyści
 * warstwę, więc zgłoszenie, które przestało być przyjęte, znika z mapy.
 */
fun CampusMapData.withBarriers(barriers: List<AcceptedBarrier>): CampusMapData {
    val features =
        barriers.map { barrier ->
            val properties =
                buildJsonObject {
                    put(key = "id", value = barrier.id)
                    put(key = BARRIER_SEVERITY, value = barrier.severity.name)
                }
            Feature(Point(barrier.location.toPosition()), properties)
        }
    return copy(barriers = featureCollection(features))
}
