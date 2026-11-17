/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.geo.toGeometry
import io.github.dostepnykampus.core.model.GeoArea
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature

/** Właściwość obiektu konturu: `true` przy budynku szczegółowym (z planem pięter), `false` przy podstawowym. */
internal const val BUILDING_DETAILED = "detailed"

/**
 * Obiekty konturów [outlines] (kontur → symbole jego budynków, w kolejności katalogu) z właściwościami `label`,
 * `symbols`, numerem [OUTLINE_NUMBER] i [BUILDING_DETAILED]; numer jest też identyfikatorem obiektu GeoJSON.
 * Kontur jest szczegółowy, gdy plan pięter ma któryś z jego budynków, czyli jego symbol jest w [detailed].
 *
 * Wymagania: US1, US1.1, US16.1
 */
internal fun outlineFeatures(outlines: Map<GeoArea, List<String>>, detailed: Set<String>) =
    outlines.entries.mapIndexed { number, (outline, symbols) ->
        val properties =
            buildJsonObject {
                put(key = "label", value = symbols.joinToString(separator = LABEL_SEPARATOR))
                put(key = "symbols", element = JsonArray(symbols.map { JsonPrimitive(it) }))
                put(key = OUTLINE_NUMBER, value = number)
                put(key = BUILDING_DETAILED, value = symbols.any { it in detailed })
            }
        Feature(outline.toGeometry(), properties, id = JsonPrimitive(number))
    }
