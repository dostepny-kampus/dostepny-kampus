/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.geo.toPosition
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.featureCollection
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point

/**
 * Właściwość punktu bariery z identyfikatorem zgłoszenia; po niej dotknięcie znaku wskazuje barierę.
 *
 * Wymagania: US1.5
 */
const val BARRIER_ID = "id"

/** Właściwość punktu bariery z nazwą uciążliwości: `HINDRANCE`, `ONLY_WITH_HELP` albo `BLOCKING`. */
const val BARRIER_SEVERITY = "severity"

/** Właściwość punktu bariery z symbolem budynku; brak, gdy zgłoszenie jest poza budynkiem. Wymagania: US16.1 */
const val BARRIER_BUILDING = "building"

/** Właściwość punktu bariery z piętrem budynku szczegółowego (`Floor.id`); brak bez piętra. Wymagania: US16.1 */
const val BARRIER_FLOOR = "floor"

/**
 * Te same dane mapy z barierami [barriers] jako punktami warstwy barier, z budynkiem i piętrem zgłoszenia.
 * Pusta lista czyści warstwę, więc zgłoszenie, które przestało być przyjęte, znika z mapy.
 *
 * Wymagania: US1.3, BR-6, US16.1
 */
fun CampusMapData.withBarriers(barriers: List<AcceptedBarrier>): CampusMapData {
    val features =
        barriers.map { barrier ->
            val properties =
                buildJsonObject {
                    put(key = BARRIER_ID, value = barrier.id)
                    put(key = BARRIER_SEVERITY, value = barrier.severity.name)
                    barrier.buildingSymbol?.let { put(key = BARRIER_BUILDING, value = it) }
                    barrier.floor?.floorId?.let { put(key = BARRIER_FLOOR, value = it) }
                }
            Feature(Point(barrier.location.toPosition()), properties)
        }
    return copy(barriers = featureCollection(features))
}
