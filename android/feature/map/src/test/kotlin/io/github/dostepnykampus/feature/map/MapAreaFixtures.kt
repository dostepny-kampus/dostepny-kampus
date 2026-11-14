/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.mutableStateOf
import io.github.dostepnykampus.feature.map.area.MapAreas
import kotlinx.serialization.json.JsonObject

/** Tereny testowe mapy V1 na prostokątach (nie dane PWr), wspólne dla testów nakładki. */
internal object MapAreaFixtures {
    private const val SIDE = 0.01

    /** Teren [name] z budynkiem [symbol]: kwadrat o boku 0,01° od długości [west] i szerokości 51°. */
    fun area(name: String, west: Double, symbol: String = name): MapArea =
        MapArea(
            name = name,
            symbols = listOf(symbol),
            bounds = CampusBounds(west = west, south = 51.0, east = west + SIDE, north = 51.0 + SIDE),
        )

    /** Mapa bez budynków z terenami [main] i [other], pokazująca [main]; zaznaczony budynek [symbol]. */
    fun twoAreas(main: MapArea, other: MapArea, symbol: String? = null): MapAreas =
        MapAreas(
            data =
                CampusMapData(
                    buildings = JsonObject(emptyMap()),
                    labels = emptyList(),
                    labelPoints = JsonObject(emptyMap()),
                    entrances = JsonObject(emptyMap()),
                    start = null,
                    bounds = main.bounds,
                    areas = listOf(main, other),
                ),
            symbol = symbol,
            chosen = mutableStateOf(null),
            seen = mutableStateOf(null),
        )
}
