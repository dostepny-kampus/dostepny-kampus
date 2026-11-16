/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
import io.github.dostepnykampus.feature.map.campus.BUILDING_DETAILED
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.featureCollection
import io.github.dostepnykampus.feature.map.campus.outlineOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SELECTED_WIDTH = 4.0
private const val SYMBOLS = "symbols"

private const val MARKS_MIN_ZOOM = 17.0
private const val ANY_ZOOM = 0.0

private val ALL_FEATURES = expression("!", JsonPrimitive(false))
private val NO_FEATURES = expression("!", JsonPrimitive(true))

/** Stan obiektu konturu budynku w źródle [BUILDINGS_SOURCE]: kontur przygaszony obok zaznaczonego budynku. */
internal const val DIMMED_STATE = "przygaszony"

/**
 * Barwa konturów ze stanu obiektu: [dimmed] dla konturów ze stanem [DIMMED_STATE], [own] dla pozostałych. Wyrażenie
 * jest stałe, więc zaznaczenie zmienia tylko stan obiektów, bez nowego układu kafelków źródła.
 *
 * Wymagania: US2.8, NFR-56
 */
internal fun tone(own: JsonElement, dimmed: String): JsonElement =
    expression(
        "case",
        expression("boolean", expression("feature-state", JsonPrimitive(DIMMED_STATE)), JsonPrimitive(false)),
        JsonPrimitive(dimmed),
        own,
    )

/**
 * Wartość pola stylu według poziomu budynku: [detailed] dla budynku szczegółowego (z planem pięter), [basic] dla
 * podstawowego.
 *
 * Wymagania: US16.1, NFR-21
 */
internal fun byLevel(detailed: JsonPrimitive, basic: JsonPrimitive): JsonElement =
    expression("case", expression("==", property(BUILDING_DETAILED), JsonPrimitive(true)), detailed, basic)

/** Warstwa grubszego obrysu `pwr_wybrany` z filtrem [filter] z [SelectionLook.selectedFilter]. Wymagania: US1.1 */
internal fun selectedLayer(filter: JsonElement, colors: MapStyleColors): JsonObject =
    lineLayer(
        id = SELECTED_LAYER,
        source = BUILDINGS_SOURCE,
        color = colors.selectedOutline,
        width = SELECTED_WIDTH,
        filter = filter,
    )

/**
 * Pola stylu, które zależą od zaznaczenia budynku, wejścia i bariery; reszta stylu od nich nie zależy.
 *
 * @property dimmed stan [DIMMED_STATE] konturów źródła [BUILDINGS_SOURCE] po numerze konturu
 * @property selectedFilter filtr warstwy [SELECTED_LAYER]
 * @property entrancesFilter filtr warstwy [ENTRANCES_LAYER]
 * @property entrancesMinZoom najmniejsze powiększenie, przy którym rysuje się warstwa [ENTRANCES_LAYER]
 * @property chosenBarrierFilter filtr warstwy [CHOSEN_BARRIER_LAYER]
 * @property chosenEntrance dane GeoJSON źródła [CHOSEN_SOURCE]
 * @property planFloor piętro planu w warstwach planu; `null` bez planu
 * @property barriersFilter filtr warstw [BARRIERS_LAYER] i [BARRIER_WORDS_LAYER]
 */
internal data class SelectionLook(
    val dimmed: List<Boolean>,
    val selectedFilter: JsonElement,
    val entrancesFilter: JsonElement,
    val entrancesMinZoom: Double,
    val chosenBarrierFilter: JsonElement,
    val chosenEntrance: JsonElement,
    val planFloor: String?,
    val barriersFilter: JsonElement,
)

/**
 * Pola stylu dla zaznaczenia [selection] na mapie z danymi [data]. Kontur budynku
 * [MapSelection.building] ma obrys, a przy zaznaczonym budynku bez wejścia pozostałe budynki są przygaszone
 * i znaki wejść ma tylko on, przy każdym powiększeniu. Bez zaznaczenia filtry obrysu i bariery niczego nie
 * przepuszczają, filtr wejść przepuszcza wszystkie od powiększenia 17, a źródło wejścia jest puste. Plan
 * [MapSelection.plan] pokazuje wybrane piętro i bariery tego piętra; dane planu idą osobno przez [showPlanData].
 * Bariery widać według [barriersByZoom], a bariery zaznaczonego budynku przy każdym powiększeniu.
 *
 * Wymagania: US1.1, US1.4, US2.8, US2.1, NFR-15, US1.3
 */
internal fun selectionLook(data: CampusMapData, selection: MapSelection): SelectionLook {
    val chosen = data.outlineOf(selection.building)
    val alone = chosen.takeIf { selection.entrance == null }
    return SelectionLook(
        dimmed = data.outlines.map { alone != null && it != alone },
        selectedFilter = chosen?.let(::ofBuildings) ?: NO_FEATURES,
        entrancesFilter = alone?.let(::ofBuildings) ?: ALL_FEATURES,
        entrancesMinZoom = if (alone == null) MARKS_MIN_ZOOM else ANY_ZOOM,
        chosenBarrierFilter = selection.barrier?.let(::ofBarrier) ?: NO_FEATURES,
        chosenEntrance = featureCollection(selection.entrance?.let(::chosenFeatures).orEmpty()),
        planFloor = selection.plan?.floor,
        barriersFilter =
            expression(
                "all",
                barriersOnPlan(building = selection.plan?.building, floor = selection.plan?.floor),
                barriersByZoom(chosen),
            ),
    )
}

/** Styl wczytany na mapie, w którym zmienia się filtry, barwy, dane źródeł i obrazy bez wczytywania stylu. */
internal interface MapStyleTarget {
    /** Ustawia filtr [filter] warstwy [layer]: wyrażenie stylu, także dla stałych (`["!", true]`, `["!", false]`). */
    fun filter(layer: String, filter: JsonElement)

    /** Ustawia pole [property] rysowania warstwy [layer] na [value]: wyrażenie stylu albo stałą. */
    fun paint(layer: String, property: String, value: JsonElement)

    /** Ustawia stan [key] obiektu o identyfikatorze [feature] w źródle [source] na [value]. */
    fun state(source: String, feature: Int, key: String, value: Boolean)

    /** Ustawia najmniejsze powiększenie [zoom], przy którym rysuje się warstwa [layer]. */
    fun minZoom(layer: String, zoom: Double)

    /** Zastępuje dane źródła GeoJSON [source] tekstem [geoJson]. */
    fun data(source: String, geoJson: String)

    /** Dodaje albo zastępuje obraz [name]. */
    fun image(name: String, image: Bitmap)
}

/**
 * Pokazuje zaznaczenie [look] w stylu z [campusMapStyle] i dodaje obrazy zaznaczenia [images]. Po wywołaniu styl
 * rysuje to samo co styl z [campusMapStyle] dla tego zaznaczenia.
 */
internal fun MapStyleTarget.showSelection(look: SelectionLook, images: Map<String, Bitmap> = emptyMap()) {
    images.forEach { (name, image) -> image(name, image) }
    look.dimmed.forEachIndexed { outline, isDimmed ->
        state(source = BUILDINGS_SOURCE, feature = outline, key = DIMMED_STATE, value = isDimmed)
    }
    filter(layer = SELECTED_LAYER, filter = look.selectedFilter)
    filter(layer = ENTRANCES_LAYER, filter = look.entrancesFilter)
    minZoom(layer = ENTRANCES_LAYER, zoom = look.entrancesMinZoom)
    filter(layer = CHOSEN_BARRIER_LAYER, filter = look.chosenBarrierFilter)
    data(source = CHOSEN_SOURCE, geoJson = look.chosenEntrance.asStyleText())
    showPlanFloor(floor = look.planFloor)
    filter(layer = BARRIERS_LAYER, filter = look.barriersFilter)
    filter(layer = BARRIER_WORDS_LAYER, filter = look.barriersFilter)
}

/**
 * Pokazuje podkład terenu [basemap] w stylu z [campusMapStyle]. Po wywołaniu styl rysuje to samo co styl
 * z [campusMapStyle] z tym podkładem, więc zmiana terenu nie wymaga nowego stylu.
 *
 * Wymagania: US1.7, NFR-39
 */
internal fun MapStyleTarget.showBasemap(basemap: Basemap?) {
    val area = basemap?.minZoom ?: 0.0
    BASEMAP_LAYERS.forEach { minZoom(layer = it, zoom = basemapMinZoom(layer = it, area = area)) }
}

/**
 * Filtr obiektów budynków [symbols] po liście `symbols` obiektu, nie po podpisie. Z konturów zostaje kontur
 * tych budynków, a ze znaków wejść ich wejścia, także wejście wspólne z innym budynkiem.
 * Kontur wspólny „C-3/C-4” zostawia wejścia obu budynków. Warunek stoi w wyrażeniu `case`, bo sam
 * operator `any` albo `in` MapLibre czyta jako filtr w dawnym zapisie.
 *
 * Wymagania: US1.4, US2.1
 */
internal fun ofBuildings(symbols: List<String>): JsonElement =
    expression("case", anyOf(symbols), JsonPrimitive(true), JsonPrimitive(false))

private fun anyOf(symbols: List<String>): JsonElement =
    JsonArray(listOf(JsonPrimitive("any")) + symbols.map { expression("in", JsonPrimitive(it), property(SYMBOLS)) })
