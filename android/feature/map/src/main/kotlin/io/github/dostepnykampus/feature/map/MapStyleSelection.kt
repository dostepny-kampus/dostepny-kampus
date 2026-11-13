/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import android.graphics.Bitmap
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.data.featureCollection
import io.github.dostepnykampus.feature.map.data.outlineOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SELECTED_WIDTH = 4.0
private const val SYMBOLS = "symbols"

/**
 * Barwa konturów przy zaznaczonym konturze budynków [outline]: [own] dla niego, [other] dla pozostałych, więc
 * sąsiednie budynki są przygaszone (US2.8, decyzja 153). Bez zaznaczenia wszystkie kontury mają barwę [own].
 */
internal fun tone(outline: List<String>?, own: String, other: String): JsonElement =
    outline?.let { expression("case", anyOf(it), JsonPrimitive(own), JsonPrimitive(other)) } ?: JsonPrimitive(own)

/** Warstwa grubszego obrysu `pwr_wybrany` z filtrem [filter] z [SelectionLook.selectedFilter] (US1.1). */
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
 * @property buildingsFill barwa wypełnienia warstwy [BUILDINGS_LAYER]
 * @property outlinesColor barwa linii warstwy [OUTLINES_LAYER]
 * @property selectedFilter filtr warstwy [SELECTED_LAYER]
 * @property entrancesFilter filtr warstwy [ENTRANCES_LAYER]
 * @property chosenBarrierFilter filtr warstwy [CHOSEN_BARRIER_LAYER]
 * @property chosenEntrance dane GeoJSON źródła [CHOSEN_SOURCE]
 */
internal data class SelectionLook(
    val buildingsFill: JsonElement,
    val outlinesColor: JsonElement,
    val selectedFilter: JsonElement,
    val entrancesFilter: JsonElement,
    val chosenBarrierFilter: JsonElement,
    val chosenEntrance: JsonElement,
)

/**
 * Pola stylu dla zaznaczenia [selection] na mapie z danymi [data] i barwami [colors]. Kontur budynku
 * [MapSelection.building] ma obrys, a przy zaznaczonym budynku bez wejścia pozostałe budynki są przygaszone
 * i znaki wejść ma tylko on (US1.1, US1.4, US2.8). Bez zaznaczenia filtry obrysu i bariery niczego nie
 * przepuszczają, filtr wejść przepuszcza wszystkie, a źródło wejścia jest puste (US2.1, NFR-15).
 */
internal fun selectionLook(data: CampusMapData, selection: MapSelection, colors: MapStyleColors): SelectionLook {
    val chosen = data.outlineOf(selection.building)
    val alone = chosen.takeIf { selection.entrance == null }
    return SelectionLook(
        buildingsFill = tone(alone, own = colors.building, other = colors.dimmed),
        outlinesColor = tone(alone, own = colors.buildingOutline, other = colors.dimmedOutline),
        selectedFilter = chosen?.let(::ofBuildings) ?: JsonPrimitive(false),
        entrancesFilter = alone?.let(::ofBuildings) ?: JsonPrimitive(true),
        chosenBarrierFilter = selection.barrier?.let(::ofBarrier) ?: JsonPrimitive(false),
        chosenEntrance = featureCollection(selection.entrance?.let(::chosenFeatures).orEmpty()),
    )
}

/**
 * Styl wczytany na mapie, w którym zmienia się filtry, barwy, dane źródeł i obrazy bez wczytywania stylu.
 * TODO(#816): widok mapy MapLibre jeszcze go nie używa.
 */
internal interface MapStyleTarget {
    /** Ustawia filtr [filter] (wyrażenie JSON) warstwy [layer]. */
    fun filter(layer: String, filter: String)

    /** Ustawia pole [property] rysowania warstwy [layer] na wyrażenie JSON [value]. */
    fun paint(layer: String, property: String, value: String)

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
    paint(BUILDINGS_LAYER, BUILDINGS_FILL, look.buildingsFill.toString())
    paint(OUTLINES_LAYER, OUTLINES_COLOR, look.outlinesColor.toString())
    filter(SELECTED_LAYER, look.selectedFilter.toString())
    filter(ENTRANCES_LAYER, look.entrancesFilter.toString())
    filter(CHOSEN_BARRIER_LAYER, look.chosenBarrierFilter.toString())
    data(CHOSEN_SOURCE, look.chosenEntrance.toString())
}

/**
 * Filtr obiektów budynków [symbols] po liście `symbols` obiektu, nie po podpisie. Z konturów zostaje kontur
 * tych budynków, a ze znaków wejść ich wejścia, także wejście wspólne z innym budynkiem (US1.4, US2.1, decyzja
 * 153). Kontur wspólny „C-3/C-4” zostawia wejścia obu budynków. Warunek stoi w wyrażeniu `case`, bo sam
 * operator `any` albo `in` MapLibre czyta jako filtr w dawnym zapisie.
 */
internal fun ofBuildings(symbols: List<String>): JsonElement =
    expression("case", anyOf(symbols), JsonPrimitive(true), JsonPrimitive(false))

private fun anyOf(symbols: List<String>): JsonElement =
    JsonArray(listOf(JsonPrimitive("any")) + symbols.map { expression("in", JsonPrimitive(it), property(SYMBOLS)) })
