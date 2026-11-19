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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import kotlin.math.cos
import kotlin.math.sin

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
 * Właściwość punktu bariery przy wejściu z przesunięciem znaku w dp, `[x, y]` na ekranie, na zewnątrz konturu;
 * brak u bariery bez wejścia. Wymagania: US2.2, NFR-17
 */
const val BARRIER_OFFSET = "offset"

/** Właściwość punktu bariery przy wejściu z przesunięciem słowa uciążliwości w dp, `[x, y]`. Wymagania: NFR-20 */
const val BARRIER_WORD_OFFSET = "word_offset"

/** Odległość znaku bariery przy wejściu od znaku wejścia w dp: bok obszaru dotyku znaku. Wymagania: NFR-17 */
const val ENTRANCE_BARRIER_DP = 48.0

/** Odstęp słowa uciążliwości od środka znaku bariery w dp. */
const val BARRIER_WORD_DP = 20.0

/**
 * Te same dane mapy z barierami [barriers] jako punktami warstwy barier, z budynkiem i piętrem zgłoszenia.
 * Pusta lista czyści warstwę, więc zgłoszenie, które przestało być przyjęte, znika z mapy. Bariery przy jednym
 * wejściu dają jeden punkt, tej o najwyższej uciążliwości, z przesunięciem znaku na zewnątrz konturu budynku,
 * prostopadle do najbliższej ściany; przy wierzchołku wklęsłym znak stoi nad wejściem.
 *
 * Wymagania: US1.3, BR-6, BR-8, US16.1, US2.2, NFR-17
 */
fun CampusMapData.withBarriers(barriers: List<AcceptedBarrier>): CampusMapData {
    val atEntrances =
        barriers
            .filter { it.entranceId != null }
            .groupBy { it.entranceId }
            .values
            .map { found -> found.maxBy { it.severity.value } }
    val features =
        (barriers.filter { it.entranceId == null } + atEntrances).map { barrier ->
            val properties =
                buildJsonObject {
                    put(key = BARRIER_ID, value = barrier.id)
                    put(key = BARRIER_SEVERITY, value = barrier.severity.name)
                    barrier.buildingSymbol?.let { put(key = BARRIER_BUILDING, value = it) }
                    barrier.floor?.floorId?.let { put(key = BARRIER_FLOOR, value = it) }
                    outwardDirection(barrier)?.let { (x, y) ->
                        val signX = x * ENTRANCE_BARRIER_DP
                        val signY = y * ENTRANCE_BARRIER_DP
                        put(key = BARRIER_OFFSET, element = dpOffset(x = signX, y = signY))
                        put(key = BARRIER_WORD_OFFSET, element = dpOffset(x = signX + BARRIER_WORD_DP, y = signY))
                    }
                }
            Feature(Point(barrier.location.toPosition()), properties)
        }
    return copy(barriers = featureCollection(features))
}

private fun CampusMapData.outwardDirection(barrier: AcceptedBarrier): Pair<Double, Double>? {
    val entrance = barrier.entranceId?.let(entrancePoints::get)
    val outline = barrier.buildingSymbol?.let(buildingOutlines::get)
    return entrance?.let { point ->
        outline?.let { area ->
            val bearing = Math.toRadians(area.outwardBearing(point) ?: 0.0)
            sin(bearing) to -cos(bearing)
        }
    }
}

private fun dpOffset(x: Double, y: Double): JsonArray = JsonArray(listOf(JsonPrimitive(x), JsonPrimitive(y)))
