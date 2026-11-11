/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

/** Warstwa przyciemnienia obszaru poza terenem (US1.7, BR-G5). */
internal const val OUTSIDE_LAYER = "teren-poza"

/** Warstwa linii granicy terenu (US1.7, BR-G5). */
internal const val EDGE_LAYER = "teren-granica"

internal const val BUILDINGS_SOURCE = BUILDINGS_LAYER
private const val ENTRANCES_SOURCE = ENTRANCES_LAYER
private const val BASEMAP = "osm"
private const val LABELS = "podpisy"
internal const val LABEL = "label"
private const val STYLE_VERSION = 8
private const val TILE_SIZE = 256
private const val TILE_MAX_ZOOM = 19
private const val OUTLINE_WIDTH = 1.5

/** Kafelki podkładu z serwerów OSM, pobierane tylko online i bez zapisu do użytku offline (ADR-0005). */
internal const val OSM_TILES = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

/**
 * Kolory stylu mapy z tokenów `mapa.kolory` jako `#RRGGBB`.
 *
 * @property land tło lądu (`lad`)
 * @property building wypełnienie budynków PWr (`pwr`)
 * @property buildingOutline kontur budynków PWr (`pwr_kontur`)
 * @property selectedOutline obrys zaznaczonego budynku (`pwr_wybrany`), grubszy niż kontur
 * @property isNight motyw ciemny: podkład jest wtedy odwrócony, żeby mapa nie raziła na ciemnym ekranie
 * @property ink linia granicy terenu (`tusz`)
 * @property dimmed wypełnienie budynków obok zaznaczonego (`budynek`)
 * @property dimmedOutline kontur budynków obok zaznaczonego (`budynek_kontur`)
 */
data class MapStyleColors(
    val land: String,
    val building: String,
    val buildingOutline: String,
    val selectedOutline: String,
    val isNight: Boolean = false,
    val ink: String = buildingOutline,
    val dimmed: String = building,
    val dimmedOutline: String = buildingOutline,
)

/**
 * Styl mapy V1 (ADR-0005). Kontury i podpisy ze źródła w aplikacji są zawsze, a podkład kafelków OSM tylko
 * online. Bez sieci warstwa podkładu jest pusta i dalej widać kontury na tle lądu (NFR-10, NFR-39). Podpis konturu
 * to obraz o nazwie z właściwości `label`, który dodaje widok mapy, bo napisy z czcionek stylu wymagają sieci.
 * Obraz stoi w jednym punkcie wewnątrz konturu, więc każdy kontur ma jeden podpis przy każdym przybliżeniu.
 * Podpisy się nie nakładają (reguła 4 wymagań wspólnych). Kontur budynku [MapSelection.building] ma grubszy obrys
 * `pwr_wybrany` (US1.1). Pozostałe budynki są wtedy przygaszone, ale zachowują podpisy, a znaki wejść ma tylko
 * zaznaczony budynek (US1.4, US2.8, decyzja 153). Bez zaznaczonego budynku każde wejście
 * z [CampusMapData.entrances] ma znak o kształcie według oznaczenia, bez filtra, więc zaznaczenie wejścia nie
 * chowa pozostałych (US2.1, NFR-21). Wejście [MapSelection.entrance] ma ponadto pinezkę czubkiem w swoim
 * punkcie i pod znakiem podpis z nazwą (US3.3, decyzja 134).
 * Podkład OSM obejmuje tylko obszar kampusu, więc kafelków spoza niego mapa nie pobiera (NFR-39). Obrys
 * zaznaczonego budynku obejmuje także kontur wspólny „C-3/C-4”; bez zaznaczenia nie ma tej warstwy. Obraz znaku
 * zależy od oznaczenia, więc kształt niesie znaczenie, nie barwa (NFR-21); brak filtra i nakładanie są
 * dozwolone, bo znaki zostają przy zaznaczonym wejściu (decyzja 134 pkt 2). Zaznaczone wejście to źródło z jednym
 * punktem o właściwościach `label` (nazwa) i `image` (obraz podpisu). Pinezka stoi czubkiem (dół obrazu)
 * w punkcie wejścia, a podpis z nazwą pod znakiem, w odsunięciu `CAPTION_OFFSET_DP` w dp (US3.3): tuż pod
 * znakiem, którego środek stoi w tym punkcie; obie warstwy rysują się zawsze. Podkład ma przygaszone barwy
 * i prześwituje przez niego tło lądu, więc budynki PWr i znaki są na mapie najwyraźniejsze, a zieleń, woda i ulice
 * zostają rozpoznawalne (makieta hi-fi V1, NFR-16). W motywie ciemnym jasność kafelków jest odwrócona.
 * Obszar poza terenem [MapSelection.bounds] jest przyciemniony, a granicę terenu pokazuje linia (US1.7, BR-G5).
 */
fun campusMapStyle(data: CampusMapData, selection: MapSelection, colors: MapStyleColors): String {
    val style =
        buildJsonObject {
            put(key = "version", value = STYLE_VERSION)
            put(key = "sources", element = sources(data, selection))
            put(key = "layers", element = layers(data.labels, selection, colors))
        }
    return style.toString()
}

/** Nazwa obrazu znaku wejścia o oznaczeniu [marking] (US2.1, NFR-21). */
internal fun entranceMarkImage(marking: EntranceMarking): String = "wejscie_${marking.name}"

/** Nazwa obrazu podpisu zaznaczonego wejścia o nazwie [label]; przedrostek odróżnia go od podpisów budynków. */
internal fun entranceCaptionImage(label: String): String = "wejscie: $label"

private fun sources(data: CampusMapData, selection: MapSelection): JsonObject =
    buildJsonObject {
        put(key = BASEMAP, element = basemapSource(selection.bounds))
        put(key = BUILDINGS_SOURCE, element = geoJsonSource(Json.parseToJsonElement(data.buildings)))
        put(key = LABELS, element = geoJsonSource(Json.parseToJsonElement(data.labelPoints)))
        put(key = ENTRANCES_SOURCE, element = geoJsonSource(Json.parseToJsonElement(data.entrances)))
        selection.bounds?.let {
            put(key = AREA_SOURCE, element = geoJsonSource(featureCollection(areaFeatures(it))))
        }
        selection.entrance?.let {
            put(key = CHOSEN_SOURCE, element = geoJsonSource(featureCollection(chosenFeatures(it))))
        }
    }

private fun layers(labels: List<String>, selection: MapSelection, colors: MapStyleColors): JsonArray =
    buildJsonArray {
        add(styleLayer(id = "lad", type = "background") { paint("background-color" to JsonPrimitive(colors.land)) })
        add(
            styleLayer(id = BASEMAP, type = "raster", source = BASEMAP) {
                put(key = "paint", element = basemapPaint(colors))
            },
        )
        val chosen = selectedLabel(labels, selection.building)
        val alone = chosen.takeIf { selection.entrance == null }
        add(
            styleLayer(id = BUILDINGS_LAYER, type = "fill", source = BUILDINGS_SOURCE) {
                paint("fill-color" to tone(alone, own = colors.building, other = colors.dimmed))
            },
        )
        add(
            styleLayer(id = "kontury", type = "line", source = BUILDINGS_SOURCE) {
                val outline = tone(alone, own = colors.buildingOutline, other = colors.dimmedOutline)
                paint("line-color" to outline, "line-width" to JsonPrimitive(OUTLINE_WIDTH))
            },
        )
        chosen?.let { add(selectedLayer(it, colors)) }
        add(marksLayer(filter = alone?.let(::entrancesOf)))
        add(symbolLayer(id = LABELS, source = LABELS, image = property(LABEL)))
        if (selection.bounds != null) areaLayers(colors).forEach { add(it) }
        if (selection.entrance != null) chosenLayers().forEach { add(it) }
    }

private fun basemapSource(bounds: CampusBounds?): JsonObject =
    buildJsonObject {
        put(key = "type", value = "raster")
        put(key = "tiles", element = JsonArray(listOf(JsonPrimitive(OSM_TILES))))
        put(key = "tileSize", value = TILE_SIZE)
        put(key = "maxzoom", value = TILE_MAX_ZOOM)
        bounds?.let { put(key = "bounds", element = numbers(it.west, it.south, it.east, it.north)) }
    }

private fun basemapPaint(colors: MapStyleColors): JsonObject =
    buildJsonObject {
        if (colors.isNight) {
            put(key = "raster-opacity", value = 0.75)
            put(key = "raster-saturation", value = -0.6)
            put(key = "raster-brightness-min", value = 0.3)
            put(key = "raster-brightness-max", value = 0.06)
        } else {
            put(key = "raster-opacity", value = 0.72)
            put(key = "raster-saturation", value = -0.4)
            put(key = "raster-contrast", value = -0.05)
        }
    }

private fun marksLayer(filter: JsonElement?): JsonObject {
    val (accessible, inaccessible, noData) = EntranceMarking.entries.map { JsonPrimitive(entranceMarkImage(it)) }
    val image =
        expression(
            "match",
            property("marking"),
            JsonPrimitive(EntranceMarking.ACCESSIBLE.name),
            accessible,
            JsonPrimitive(EntranceMarking.INACCESSIBLE.name),
            inaccessible,
            noData,
        )
    val marks = symbolLayer(id = ENTRANCES_LAYER, source = ENTRANCES_SOURCE, image = image, OVERLAP)
    return filter?.let { JsonObject(marks + ("filter" to it)) } ?: marks
}
