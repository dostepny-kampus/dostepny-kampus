/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.data.CampusMapData

/** Warstwa wypełnienia konturów; widok mapy szuka w niej dotkniętego budynku. */
internal const val BUILDINGS_LAYER = "budynki"

/** Kafelki podkładu z serwerów OSM, pobierane tylko online i bez zapisu do użytku offline (ADR-0005). */
internal const val OSM_TILES = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

/**
 * Kolory stylu mapy z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property land tło lądu (`lad`)
 * @property building wypełnienie budynków PWr (`pwr`)
 * @property buildingOutline kontur budynków PWr (`pwr_kontur`)
 */
data class MapStyleColors(val land: String, val building: String, val buildingOutline: String)

/**
 * Styl mapy V1 (ADR-0005). Kontury i podpisy ze źródła w aplikacji są zawsze, a podkład kafelków OSM tylko
 * online. Bez sieci warstwa podkładu jest pusta i dalej widać kontury na tle lądu (NFR-10, NFR-39). Podpis konturu
 * to obraz o nazwie z właściwości `label`, który dodaje widok mapy, bo napisy z czcionek stylu wymagają sieci.
 * Obraz stoi w jednym punkcie wewnątrz konturu, więc każdy kontur ma jeden podpis przy każdym przybliżeniu.
 * Podpisy się nie nakładają (reguła 4 wymagań wspólnych).
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
        |    {"id": "podpisy", "type": "symbol", "source": "podpisy", "layout": {"icon-image": ["get", "label"]}}
        |  ]
        |}
    """.trimMargin()
