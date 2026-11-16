/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/** Warstwa wypełnienia konturów; widok mapy szuka w niej dotkniętego budynku. */
internal const val BUILDINGS_LAYER = "budynki"

/** Warstwa obrysu zaznaczonego budynku. */
internal const val SELECTED_LAYER = "zaznaczenie"

/** Warstwa znaków wszystkich wejść: kształt według oznaczenia. Wymagania: US2.1, NFR-21 */
internal const val ENTRANCES_LAYER = "wejscia"

/** Warstwa pinezki zaznaczonego wejścia, czubkiem w punkcie wejścia. Wymagania: US3.3 */
internal const val ENTRANCE_PIN_LAYER = "wejscie_pinezka"

/** Warstwa podpisu zaznaczonego wejścia: nazwa pod znakiem wejścia. Wymagania: US3.3 */
internal const val ENTRANCE_CAPTION_LAYER = "wejscie_podpis"

/** Obraz pinezki zaznaczonego wejścia, której czubek stoi w punkcie wejścia. Wymagania: US3.3 */
internal const val ENTRANCE_PIN_IMAGE = "wejscie_pinezka"

/** Wysokość i szerokość znaku wejścia w dp, z tokenu `ikony.znak_mapy`. Wymagania: US2.1, NFR-21 */
internal val ENTRANCE_MARK_DP: Int = Ikony.znakMapy.value.toInt()

/** Warstwa wyblaknięcia mapy poza granicą terenu. Wymagania: US1.7, BR-G5 */
internal const val OUTSIDE_LAYER = "teren-poza"

/** Warstwa obwódki pod linią granicy terenu: linia ma kontrast na jasnym i ciemnym podkładzie. Wymagania: NFR-16 */
internal const val EDGE_HALO_LAYER = "teren-granica-obwodka"

/** Warstwa linii granicy terenu. Wymagania: US1.7, BR-G5 */
internal const val EDGE_LAYER = "teren-granica"

/** Warstwa konturów wszystkich budynków. */
internal const val OUTLINES_LAYER = "kontury"

/** Pole barwy wypełnienia warstwy [BUILDINGS_LAYER]. */
internal const val BUILDINGS_FILL = "fill-color"

/** Pole barwy linii warstwy [OUTLINES_LAYER]. */
internal const val OUTLINES_COLOR = "line-color"

internal const val BUILDINGS_SOURCE = BUILDINGS_LAYER
private const val ENTRANCES_SOURCE = ENTRANCES_LAYER

/** Warstwa i źródło podkładu OSM. Wymagania: NFR-39 */
internal const val BASEMAP = "osm"
private const val LABELS = "podpisy"
internal const val LABEL = "label"
private const val STYLE_VERSION = 8
private const val TILE_SIZE = 256
private const val TILE_MAX_ZOOM = 19
private const val TILE_JSON = "2.2.0"
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
 * @property edge linia granicy terenu (`granica_terenu`)
 * @property edgeHalo obwódka linii granicy terenu (`granica_terenu_obwodka`)
 * @property outside kolor kładziony na mapę poza granicą terenu (`background` motywu)
 * @property outsideOpacity krycie koloru [outside] (`mapa.krycie.teren_poza`); 0 zostawia mapę bez wyblaknięcia
 * @property dimmed wypełnienie budynków obok zaznaczonego (`budynek`)
 * @property dimmedOutline kontur budynków obok zaznaczonego (`budynek_kontur`)
 * @property plan kolory planu piętra; bez nich plan ma barwy budynków
 */
data class MapStyleColors(
    val land: String,
    val building: String,
    val buildingOutline: String,
    val selectedOutline: String,
    val isNight: Boolean = false,
    val edge: String = buildingOutline,
    val edgeHalo: String = land,
    val outside: String = land,
    val outsideOpacity: Double = 0.0,
    val dimmed: String = building,
    val dimmedOutline: String = buildingOutline,
    val plan: PlanColors =
        PlanColors(
            classroom = building,
            corridor = land,
            other = dimmed,
            elevator = building,
            toilet = land,
            wall = buildingOutline,
        ),
)

/**
 * Podkład OSM mapy: przy powiększeniu mniejszym niż [minZoom], np. w środku przelotu na inny teren, MapLibre go
 * nie rysuje ani nie pobiera.
 *
 * Wymagania: NFR-39
 *
 * @property bounds prostokąt, w którym są kafelki podkładu
 * @property minZoom najmniejsze powiększenie, przy którym podkład jest rysowany
 */
data class Basemap(val bounds: CampusBounds, val minZoom: Double = 0.0)

/**
 * Styl mapy V1 (ADR-0005) jako tekst JSON: kontury i podpisy ze źródła w aplikacji zawsze, podkład OSM tylko online,
 * tylko w obszarze [basemap] i przygaszony. Zaznaczony budynek [MapSelection.building] ma grubszy obrys i jako jedyny
 * znaki wejść; wejście [MapSelection.entrance] ma pinezkę z podpisem. Poza granicą terenu [MapSelection.boundary]
 * (bez niej poza prostokątem [MapSelection.bounds]) mapa jest wyblakła, a bariery mają znak uciążliwości. Warstwy
 * zaznaczenia są w stylu zawsze, więc style dla różnych zaznaczeń różnią się tylko polami [SelectionLook]
 * z [selectionLook].
 *
 * Wymagania: NFR-10, NFR-39, US1.1, US1.4, US2.8, US2.1, NFR-21, US3.3, NFR-16, US1.7, BR-G5, US1.3
 */
fun campusMapStyle(
    data: CampusMapData,
    selection: MapSelection,
    colors: MapStyleColors,
    basemap: Basemap? = selection.bounds?.let { Basemap(bounds = it) },
): String {
    val look = selectionLook(data, selection, colors)
    val style =
        buildJsonObject {
            put(key = "version", value = STYLE_VERSION)
            val all = sources(data = data, selection = selection, look = look, basemap = basemap)
            put(key = "sources", element = all)
            val list = layers(look = look, colors = colors, basemap = basemap)
            put(key = "layers", element = list)
        }
    return style.toString()
}

/** Tekst JSON fragmentu stylu [this], bo MapLibre przyjmuje wyrażenia warstw tylko jako tekst. */
internal fun JsonElement.asStyleText(): String = toString()

/** Budowniczy stylu MapLibre z tekstu [style] z [campusMapStyle]; MapLibre przyjmuje styl tylko jako tekst. */
internal fun styleBuilder(style: String): Style.Builder = Style.Builder().fromJson(style)

/** Nazwa obrazu znaku wejścia o oznaczeniu [marking]. Wymagania: US2.1, NFR-21 */
internal fun entranceMarkImage(marking: EntranceMarking): String = "wejscie_${marking.name}"

/** Nazwa obrazu podpisu zaznaczonego wejścia o nazwie [label]; przedrostek odróżnia go od podpisów budynków. */
internal fun entranceCaptionImage(label: String): String = "wejscie: $label"

private fun sources(data: CampusMapData, selection: MapSelection, look: SelectionLook, basemap: Basemap?): JsonObject =
    buildJsonObject {
        put(key = BASEMAP, element = basemapSource(basemap?.bounds))
        put(key = BUILDINGS_SOURCE, element = geoJsonSource(data.buildings))
        put(key = LABELS, element = geoJsonSource(data.labelPoints))
        put(key = ENTRANCES_SOURCE, element = geoJsonSource(data.entrances))
        put(key = BARRIERS_SOURCE, element = geoJsonSource(data.barriers))
        put(key = AREA_SOURCE, element = geoJsonSource(areaOf(selection)))
        put(key = CHOSEN_SOURCE, element = geoJsonSource(look.chosenEntrance))
        put(key = PLAN_SOURCE, element = geoJsonSource(look.plan))
    }

private fun layers(look: SelectionLook, colors: MapStyleColors, basemap: Basemap?): JsonArray =
    buildJsonArray {
        add(styleLayer(id = "lad", type = "background") { paint("background-color" to JsonPrimitive(colors.land)) })
        add(
            styleLayer(id = BASEMAP, type = "raster", source = BASEMAP) {
                put(key = "minzoom", value = basemap?.minZoom ?: 0.0)
                put(key = "paint", element = basemapPaint(colors))
            },
        )
        add(
            styleLayer(id = BUILDINGS_LAYER, type = "fill", source = BUILDINGS_SOURCE) {
                paint(BUILDINGS_FILL to look.buildingsFill)
            },
        )
        add(
            styleLayer(id = OUTLINES_LAYER, type = "line", source = BUILDINGS_SOURCE) {
                paint(OUTLINES_COLOR to look.outlinesColor, "line-width" to JsonPrimitive(OUTLINE_WIDTH))
            },
        )
        planLayers(colors.plan, look.planFloor).forEach { add(it) }
        add(selectedLayer(look.selectedFilter, colors))
        areaLayers(colors).forEach { add(it) }
        add(marksLayer(look.entrancesFilter, look.entrancesMinZoom))
        barrierLayers(look.barriersFilter).forEach { add(it) }
        add(symbolLayer(id = LABELS, source = LABELS, image = property(LABEL)))
        add(chosenBarrierLayer(look.chosenBarrierFilter))
        chosenLayers().forEach { add(it) }
    }

/**
 * Źródło kafelków podkładu takie jak w [campusMapStyle], z kafelkami tylko w prostokącie [bounds] (bez niego
 * z całego świata), do wymiany źródła w wczytanym stylu. Wymagania: NFR-39
 */
internal fun basemapTiles(bounds: CampusBounds?): RasterSource {
    val tiles = TileSet(TILE_JSON, OSM_TILES)
    tiles.setMaxZoom(TILE_MAX_ZOOM.toFloat())
    bounds?.let { area ->
        tiles.setBounds(
            LatLngBounds.from(latNorth = area.north, lonEast = area.east, latSouth = area.south, lonWest = area.west),
        )
    }
    return RasterSource(BASEMAP, tiles, TILE_SIZE)
}

/** Źródło kafelków podkładu w stylu JSON, z kafelkami tylko w prostokącie [bounds]; `null`: cały świat. */
internal fun basemapSource(bounds: CampusBounds?): JsonObject =
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

private fun marksLayer(filter: JsonElement, minZoom: Double): JsonObject {
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
    return JsonObject(marks + ("filter" to filter) + ("minzoom" to JsonPrimitive(minZoom)))
}
