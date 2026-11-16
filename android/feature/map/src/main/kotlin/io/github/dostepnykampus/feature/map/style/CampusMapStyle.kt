/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.android.maps.Style

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

/** Warstwa konturów wszystkich budynków. */
internal const val OUTLINES_LAYER = "kontury"

/** Pole barwy wypełnienia warstwy [BUILDINGS_LAYER]. */
internal const val BUILDINGS_FILL = "fill-color"

/** Pole barwy linii warstwy [OUTLINES_LAYER]. */
internal const val OUTLINES_COLOR = "line-color"

internal const val BUILDINGS_SOURCE = BUILDINGS_LAYER
internal const val ENTRANCES_SOURCE = ENTRANCES_LAYER

private const val LABELS = "podpisy"
internal const val LABELS_SOURCE = LABELS

/** Warstwa symboli budynków. Wymagania: US1, US16.1 */
internal const val LABELS_LAYER = LABELS
internal const val LABEL = "label"
private const val STYLE_VERSION = 8

/** Grubość konturu budynku szczegółowego (z planem pięter) w dp: gruby kontur. Wymagania: US16.1, NFR-21 */
internal const val DETAILED_OUTLINE_DP = 2.4

/** Grubość konturu budynku podstawowego (bez planu pięter) w dp: cienki ciągły kontur. Wymagania: US16.1, NFR-21 */
internal const val BASIC_OUTLINE_DP = 1.0

private val OUTLINE_WIDTH =
    byLevel(detailed = JsonPrimitive(DETAILED_OUTLINE_DP), basic = JsonPrimitive(BASIC_OUTLINE_DP))

/**
 * Wygląd stylu mapy: kolory z tokenów `mapa.kolory` jako `#RRGGBB` i rozmiar napisów podkładu.
 *
 * @property land tło lądu (`lad`)
 * @property building wypełnienie budynków szczegółowych, z planem pięter (`pwr`)
 * @property buildingOutline kontur budynków PWr (`pwr_kontur`)
 * @property selectedOutline obrys zaznaczonego budynku (`pwr_wybrany`), grubszy niż kontur
 * @property dimmed wypełnienie budynków obok zaznaczonego (`budynek`)
 * @property dimmedOutline kontur budynków obok zaznaczonego (`budynek_kontur`)
 * @property plan kolory planu piętra; bez nich plan ma barwy budynków
 * @property basemap kolory podkładu wektorowego; bez nich mapa nie ma podkładu, tylko tło [land]
 * @property labelSize wysokość napisów podkładu w dp mapy, z tokenu `mapa.napisy_sp` i skali czcionki
 * @property basicBuilding wypełnienie budynków podstawowych, bez planu pięter (`pwr_podstawowy`); bez niego budynki
 *   obu poziomów mają barwę [building]
 */
data class MapStyleColors(
    val land: String,
    val building: String,
    val buildingOutline: String,
    val selectedOutline: String,
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
    val basemap: BasemapColors? = null,
    val labelSize: Double = Typografia.napisyMapy.value.toDouble(),
    val basicBuilding: String = building,
)

/**
 * Podkład mapy terenu: przy powiększeniu mniejszym niż [minZoom], np. w środku przelotu na inny teren, MapLibre go
 * nie rysuje ani nie pobiera. Kafelki dalej niż prostokąt terenu nie są potrzebne, bo widok z niego nie wychodzi.
 *
 * Wymagania: NFR-39
 *
 * @property minZoom najmniejsze powiększenie, przy którym podkład jest rysowany
 */
data class Basemap(val minZoom: Double = 0.0)

/**
 * Styl mapy V1 (ADR-0005, ADR-0014) jako tekst JSON: kontury i podpisy ze źródła w aplikacji zawsze, podkład
 * wektorowy w kolorach tokenów tylko online i od powiększenia [basemap]. Zaznaczony budynek [MapSelection.building]
 * ma grubszy obrys i jako jedyny znaki wejść; wejście [MapSelection.entrance] ma pinezkę z podpisem, a bariery mają
 * znak uciążliwości. Warstwy zaznaczenia są w stylu zawsze, więc style dla różnych zaznaczeń różnią się tylko polami
 * [SelectionLook] z [selectionLook].
 *
 * Wymagania: NFR-10, NFR-39, US1.1, US1.4, US2.8, US2.1, NFR-21, US3.3, NFR-16, US1.3
 */
fun campusMapStyle(
    data: CampusMapData,
    selection: MapSelection,
    colors: MapStyleColors,
    basemap: Basemap? = selection.bounds?.let { Basemap() },
): String {
    val look = selectionLook(data, selection)
    val style =
        buildJsonObject {
            put(key = "version", value = STYLE_VERSION)
            if (colors.basemap != null) put(key = "glyphs", value = BASEMAP_GLYPHS)
            val all = sources(data = data, look = look, plan = planFeatures(selection.plan?.rooms.orEmpty()))
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

private fun sources(data: CampusMapData, look: SelectionLook, plan: JsonElement): JsonObject =
    buildJsonObject {
        put(key = BASEMAP, element = basemapSource())
        put(key = BUILDINGS_SOURCE, element = geoJsonSource(data.buildings))
        put(key = LABELS_SOURCE, element = geoJsonSource(data.labelPoints))
        put(key = ENTRANCES_SOURCE, element = geoJsonSource(data.entrances))
        put(key = BARRIERS_SOURCE, element = geoJsonSource(data.barriers))
        put(key = CHOSEN_SOURCE, element = geoJsonSource(look.chosenEntrance))
        put(key = PLAN_SOURCE, element = geoJsonSource(plan))
    }

private fun layers(look: SelectionLook, colors: MapStyleColors, basemap: Basemap?): JsonArray =
    buildJsonArray {
        add(styleLayer(id = "lad", type = "background") { paint("background-color" to JsonPrimitive(colors.land)) })
        colors.basemap?.let { palette ->
            val minZoom = basemap?.minZoom ?: 0.0
            basemapLayers(colors = palette, minZoom = minZoom, textSize = colors.labelSize).forEach(::add)
        }
        add(
            styleLayer(id = BUILDINGS_LAYER, type = "fill", source = BUILDINGS_SOURCE) {
                val fill =
                    byLevel(detailed = JsonPrimitive(colors.building), basic = JsonPrimitive(colors.basicBuilding))
                paint(BUILDINGS_FILL to tone(own = fill, dimmed = colors.dimmed), "fill-opacity" to opacityUnderPlan())
            },
        )
        add(
            styleLayer(id = OUTLINES_LAYER, type = "line", source = BUILDINGS_SOURCE) {
                val color = tone(own = JsonPrimitive(colors.buildingOutline), dimmed = colors.dimmedOutline)
                paint(OUTLINES_COLOR to color, "line-width" to OUTLINE_WIDTH, "line-opacity" to opacityUnderPlan())
            },
        )
        planLayers(colors.plan, look.planFloor).forEach { add(it) }
        add(selectedLayer(look.selectedFilter, colors))
        add(selectedPlanLayer(look.selectedPlanFilter, colors))
        add(marksLayer(look.entrancesFilter, look.entrancesMinZoom))
        barrierLayers(look.barriersFilter).forEach { add(it) }
        val labels = symbolLayer(id = LABELS_LAYER, source = LABELS_SOURCE, image = property(LABEL))
        add(JsonObject(labels + ("filter" to look.labelsFilter)))
        add(chosenBarrierLayer(look.chosenBarrierFilter))
        chosenLayers().forEach { add(it) }
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
