/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.symbolsOf
import kotlinx.serialization.json.JsonPrimitive

/** Warstwa wypełnienia konturów; widok mapy szuka w niej dotkniętego budynku. */
internal const val BUILDINGS_LAYER = "budynki"

/** Warstwa obrysu zaznaczonego budynku. */
internal const val SELECTED_LAYER = "zaznaczenie"

/** Kafelki podkładu z serwerów OSM, pobierane tylko online i bez zapisu do użytku offline (ADR-0005). */
internal const val OSM_TILES = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

/**
 * Kolory stylu mapy z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property land tło lądu (`lad`)
 * @property building wypełnienie budynków PWr (`pwr`)
 * @property buildingOutline kontur budynków PWr (`pwr_kontur`)
 * @property selectedOutline obrys zaznaczonego budynku (`pwr_wybrany`), grubszy niż kontur
 */
data class MapStyleColors(
    val land: String,
    val building: String,
    val buildingOutline: String,
    val selectedOutline: String,
)

/**
 * Styl mapy V1 (ADR-0005). Kontury i podpisy ze źródła w aplikacji są zawsze, a podkład kafelków OSM tylko
 * online. Bez sieci warstwa podkładu jest pusta i dalej widać kontury na tle lądu (NFR-10, NFR-39). Podpis konturu
 * to obraz o nazwie z właściwości `label`, który dodaje widok mapy, bo napisy z czcionek stylu wymagają sieci.
 * Obraz stoi w jednym punkcie wewnątrz konturu, więc każdy kontur ma jeden podpis przy każdym przybliżeniu.
 * Podpisy się nie nakładają (reguła 4 wymagań wspólnych). Kontur budynku [CampusMapData.focus] ma grubszy obrys
 * `pwr_wybrany` (US1.1).
 */
fun campusMapStyle(data: CampusMapData, colors: MapStyleColors): String =
    """
        |{
        |  "version": 8,
        |  "sources": {
        |    "osm": {"type": "raster", "tiles": ["$OSM_TILES"], "tileSize": 256, "maxzoom": 19},
        |    "budynki": {"type": "geojson", "data": ${data.buildings}},
        |    "podpisy": {"type": "geojson", "data": ${data.labelPoints}}
        |  },
        |  "layers": [
        |    {"id": "lad", "type": "background", "paint": {"background-color": "${colors.land}"}},
        |    {"id": "osm", "type": "raster", "source": "osm"},
        |    {
        |      "id": "$BUILDINGS_LAYER", "type": "fill", "source": "budynki",
        |      "paint": {"fill-color": "${colors.building}"}
        |    },
        |    {
        |      "id": "kontury", "type": "line", "source": "budynki",
        |      "paint": {"line-color": "${colors.buildingOutline}", "line-width": 1.5}
        |    },
        |    ${selectedLayer(data, colors)}
        |    {"id": "podpisy", "type": "symbol", "source": "podpisy", "layout": {"icon-image": ["get", "label"]}}
        |  ]
        |}
    """.trimMargin()

// Obrys konturu z budynkiem zaznaczonym, także konturu wspólnego „C-3/C-4”; bez zaznaczenia brak warstwy.
private fun selectedLayer(data: CampusMapData, colors: MapStyleColors): String {
    val label = data.labels.firstOrNull { data.focus in symbolsOf(listOf(it)) } ?: return ""
    val filter = """["==", ["get", "label"], ${JsonPrimitive(label)}]"""
    val paint = """{"line-color": "${colors.selectedOutline}", "line-width": 4}"""
    return """{"id": "$SELECTED_LAYER", "type": "line", "source": "budynki", "filter": $filter, "paint": $paint},"""
}
