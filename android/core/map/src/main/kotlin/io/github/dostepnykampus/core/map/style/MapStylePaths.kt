/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.map.style

import io.github.dostepnykampus.core.designsystem.MapaSciezka
import io.github.dostepnykampus.core.designsystem.ZoomStop
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

/** Źródło stylu z odcinkami ścieżek dostosowanych. Wymagania: US1.4 */
public const val PATHS_SOURCE: String = "sciezki"

/** Warstwa całej wstęgi ścieżki w barwie brzegu. Wymagania: US1.4 */
public const val PATH_EDGE_LAYER: String = "sciezki_brzeg"

/** Warstwa wnętrza ścieżki, węższa niż wstęga, więc z obu stron zostaje brzeg. Wymagania: US1.4 */
public const val PATH_INSIDE_LAYER: String = "sciezki_wnetrze"

private val ROUND = JsonObject(mapOf("line-join" to JsonPrimitive("round"), "line-cap" to JsonPrimitive("round")))

/**
 * Barwy ścieżki dostosowanej z tokenów `mapa.kolory`.
 *
 * @property edge brzeg, czyli cała wstęga (`sciezka_brzeg`)
 * @property inside wnętrze na wstędze (`sciezka_wnetrze`)
 */
public data class PathColors(val edge: String, val inside: String)

/**
 * Warstwy ścieżek dostosowanych: wstęga w barwie brzegu i węższe wnętrze na niej, od powiększenia
 * `mapa.sciezka.od_powiekszenia`, z szerokością rosnącą wykładniczo jak ulice między progami z tokenów.
 *
 * Wymagania: US1.4, NFR-16, NFR-21
 */
public fun pathLayers(colors: PathColors): List<JsonObject> =
    listOf(
        pathLayer(id = PATH_EDGE_LAYER, color = colors.edge, widths = MapaSciezka.wstegaDp),
        pathLayer(id = PATH_INSIDE_LAYER, color = colors.inside, widths = MapaSciezka.wnetrzeDp),
    )

private fun pathLayer(id: String, color: String, widths: List<ZoomStop>): JsonObject =
    styleLayer(id = id, type = "line", source = PATHS_SOURCE) {
        put(key = "minzoom", value = MapaSciezka.odPowiekszenia)
        put(key = "layout", element = ROUND)
        val stops = widths.flatMap { listOf(JsonPrimitive(it.zoom), JsonPrimitive(it.dp)) }
        paint("line-color" to JsonPrimitive(color), "line-width" to growingWidth(stops))
    }
