/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.symbolsOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

/** Warstwa wypełnienia konturów; widok mapy szuka w niej dotkniętego budynku. */
internal const val BUILDINGS_LAYER = "budynki"

/** Warstwa obrysu zaznaczonego budynku. */
internal const val SELECTED_LAYER = "zaznaczenie"

/** Warstwa znaków wszystkich wejść: kształt według oznaczenia (US2.1, NFR-21). */
internal const val ENTRANCES_LAYER = "wejscia"

/** Warstwa pinezki zaznaczonego wejścia, czubkiem w punkcie wejścia (US3.3). */
internal const val ENTRANCE_PIN_LAYER = "wejscie_pinezka"

/** Warstwa podpisu zaznaczonego wejścia: nazwa pod znakiem wejścia (US3.3). */
internal const val ENTRANCE_CAPTION_LAYER = "wejscie_podpis"

/** Obraz pinezki zaznaczonego wejścia, której czubek stoi w punkcie wejścia (US3.3). */
internal const val ENTRANCE_PIN_IMAGE = "wejscie_pinezka"

/** Wysokość i szerokość znaku wejścia w dp (US2.1, NFR-21). */
internal const val ENTRANCE_MARK_DP = 28

// Odsunięcie podpisu od punktu wejścia w dp (US3.3): tuż pod znakiem, którego środek stoi w tym punkcie.
private const val CAPTION_OFFSET_DP = ENTRANCE_MARK_DP / 2 + 2

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
 * `pwr_wybrany` (US1.1). Każde wejście z [CampusMapData.entrances] ma znak o kształcie według oznaczenia, bez
 * filtra, więc zaznaczenie nie chowa pozostałych (US2.1, NFR-21). Wejście [CampusMapData.selectedEntrance] ma
 * ponadto pinezkę czubkiem w swoim punkcie i pod znakiem podpis z nazwą (US3.3, decyzja 134).
 */
fun campusMapStyle(data: CampusMapData, colors: MapStyleColors): String =
    """
        |{
        |  "version": 8,
        |  "sources": {
        |    "osm": {"type": "raster", "tiles": ["$OSM_TILES"], "tileSize": 256, "maxzoom": 19${basemapBounds(data)}},
        |    "budynki": {"type": "geojson", "data": ${data.buildings}},
        |    "podpisy": {"type": "geojson", "data": ${data.labelPoints}},
        |    "wejscia": {"type": "geojson", "data": ${data.entrances}}${chosenSource(data)}
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
        |    ${marksLayer()},
        |    {"id": "podpisy", "type": "symbol", "source": "podpisy", "layout": {"icon-image": ["get", "label"]}}
        |    ${chosenLayers(data)}
        |  ]
        |}
    """.trimMargin()

/** Nazwa obrazu znaku wejścia o oznaczeniu [marking] (US2.1, NFR-21). */
internal fun entranceMarkImage(marking: EntranceMarking): String = "wejscie_${marking.name}"

/** Nazwa obrazu podpisu zaznaczonego wejścia o nazwie [label]; przedrostek odróżnia go od podpisów budynków. */
internal fun entranceCaptionImage(label: String): String = "wejscie: $label"

// Podkład tylko w obszarze kampusu, więc kafelków spoza niego mapa nie pobiera (NFR-39).
private fun basemapBounds(data: CampusMapData): String {
    val bounds = data.bounds ?: return ""
    val edges = listOf(bounds.west, bounds.south, bounds.east, bounds.north)
    return ", \"bounds\": ${JsonArray(edges.map { JsonPrimitive(it) })}"
}

// Obrys konturu z budynkiem zaznaczonym, także konturu wspólnego „C-3/C-4”; bez zaznaczenia brak warstwy.
private fun selectedLayer(data: CampusMapData, colors: MapStyleColors): String {
    val label = data.labels.firstOrNull { data.focus in symbolsOf(listOf(it)) } ?: return ""
    val filter = """["==", ["get", "label"], ${JsonPrimitive(label)}]"""
    val paint = """{"line-color": "${colors.selectedOutline}", "line-width": 4}"""
    return """{"id": "$SELECTED_LAYER", "type": "line", "source": "budynki", "filter": $filter, "paint": $paint},"""
}

// Znak każdego wejścia z danych; obraz zależy od oznaczenia, więc kształt niesie znaczenie, nie barwa (NFR-21).
// Brak filtra i nakładanie dozwolone: znaki zostają przy zaznaczeniu (decyzja 134 pkt 2).
private fun marksLayer(): String {
    val (accessible, inaccessible, noData) = EntranceMarking.entries.map(::entranceMarkImage)
    val image =
        """["match", ["get", "marking"], "ACCESSIBLE", "$accessible", """ +
            """"INACCESSIBLE", "$inaccessible", "$noData"]"""
    return """{"id": "$ENTRANCES_LAYER", "type": "symbol", "source": "wejscia", """ +
        """"layout": {"icon-image": $image, "icon-allow-overlap": true}}"""
}

// Źródło z jednym punktem, wejściem zaznaczonym; właściwości: `label` (nazwa) i `image` (obraz podpisu).
private fun chosenSource(data: CampusMapData): String {
    val chosen = data.selectedEntrance ?: return ""
    val properties =
        buildJsonObject {
            put(key = "label", value = chosen.label)
            put(key = "image", value = entranceCaptionImage(chosen.label))
        }
    val point = Point(Position(longitude = chosen.location.longitude, latitude = chosen.location.latitude))
    val json = GeoJsonObject.toJson(FeatureCollection(listOf(Feature(point, properties))))
    return """, "wejscie": {"type": "geojson", "data": $json}"""
}

// Pinezka czubkiem (dół obrazu) w punkcie wejścia i pod znakiem podpis z nazwą; obie warstwy rysują się zawsze.
private fun chosenLayers(data: CampusMapData): String {
    if (data.selectedEntrance == null) return ""
    val pin =
        """{"id": "$ENTRANCE_PIN_LAYER", "type": "symbol", "source": "wejscie", "layout": """ +
            """{"icon-image": "$ENTRANCE_PIN_IMAGE", "icon-anchor": "bottom", "icon-allow-overlap": true}}"""
    val caption =
        """{"id": "$ENTRANCE_CAPTION_LAYER", "type": "symbol", "source": "wejscie", "layout": """ +
            """{"icon-image": ["get", "image"], "icon-anchor": "top", "icon-offset": [0, $CAPTION_OFFSET_DP], """ +
            """"icon-allow-overlap": true}}"""
    return ", $pin, $caption"
}
