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

/** Grubość obrysu wybranego budynku w dp, większa niż kontur. Wymagania: US1.1 */
internal const val SELECTED_WIDTH = 4.0
private const val SYMBOLS = "symbols"

private const val MARKS_MIN_ZOOM = 17.0
private const val ANY_ZOOM = 0.0

private val ALL_FEATURES = expression("!", JsonPrimitive(false))
private val NO_FEATURES = expression("!", JsonPrimitive(true))

/** Stan obiektu konturu budynku w źródle [BUILDINGS_SOURCE]: budynek przygaszony obok zaznaczonego albo planu. */
internal const val DIMMED_STATE = "przygaszony"

/** Krycie tła przygaszonego budynku z tokenu `mapa.przygaszenie.tlo_krycie`. Wymagania: US2.8, NFR-16 */
internal const val DIMMED_FILL_OPACITY = 0.4

/** Krycie konturu przygaszonego budynku z tokenu `mapa.przygaszenie.kontur_krycie`. Wymagania: US2.8, NFR-16 */
internal const val DIMMED_OUTLINE_OPACITY = 0.7

/**
 * Krycie tła albo konturu budynku ze stanu obiektu: 0 dla budynku ze stanem [UNDER_PLAN_STATE], bo jego miejsce
 * zajmuje plan piętra; [dimmed] dla budynku ze stanem [DIMMED_STATE]; 1 dla pozostałych. Barwa budynku od stanu nie
 * zależy. Wyrażenie jest stałe, więc zaznaczenie zmienia tylko stan obiektów, bez nowego układu kafelków źródła.
 *
 * Wymagania: US2.8, US16.1, NFR-16, NFR-56
 */
internal fun buildingOpacity(dimmed: Double): JsonElement =
    expression(
        "case",
        featureState(UNDER_PLAN_STATE),
        JsonPrimitive(0.0),
        featureState(DIMMED_STATE),
        JsonPrimitive(dimmed),
        JsonPrimitive(1.0),
    )

private fun featureState(key: String): JsonElement =
    expression("boolean", expression("feature-state", JsonPrimitive(key)), JsonPrimitive(false))

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
 * @property dimmed stan [DIMMED_STATE] konturów źródła [BUILDINGS_SOURCE] po numerze konturu: wszystkie poza
 *   zaznaczonym budynkiem bez wejścia i budynkiem z widocznym planem, gdy któryś z nich jest
 * @property underPlan stan [UNDER_PLAN_STATE] konturów po numerze konturu: tylko budynek z widocznym planem
 * @property selectedFilter filtr warstwy [SELECTED_LAYER]; nie przepuszcza budynku z widocznym planem
 * @property selectedPlanFilter filtr warstwy [SELECTED_PLAN_LAYER]: ściana piętra, gdy zaznaczony budynek ma
 *   widoczny plan
 * @property labelsFilter filtr warstwy [LABELS_LAYER]: bez symbolu budynku z widocznym planem
 * @property entrancesFilter filtr warstwy [ENTRANCES_LAYER]
 * @property entrancesMinZoom najmniejsze powiększenie, przy którym rysuje się warstwa [ENTRANCES_LAYER]
 * @property chosenBarrierFilter filtr warstwy [CHOSEN_BARRIER_LAYER]
 * @property chosenEntrance dane GeoJSON źródła [CHOSEN_SOURCE]
 * @property planFloor piętro planu w warstwach planu; `null` bez planu
 * @property planRoom sala wyróżniona w warstwach z [chosenRoomLayers]; `null` bez wyróżnienia
 * @property barriersFilter filtr warstw [BARRIERS_LAYER] i [BARRIER_WORDS_LAYER]
 * @property besideEntrance kiedy znak bariery przy wejściu stoi obok znaku wejścia: przy tych samych
 *   powiększeniach i budynkach co znaki wejść z [entrancesMinZoom] i [entrancesFilter]
 */
internal data class SelectionLook(
    val dimmed: List<Boolean>,
    val underPlan: List<Boolean>,
    val selectedFilter: JsonElement,
    val selectedPlanFilter: JsonElement,
    val labelsFilter: JsonElement,
    val entrancesFilter: JsonElement,
    val entrancesMinZoom: Double,
    val chosenBarrierFilter: JsonElement,
    val chosenEntrance: JsonElement,
    val planFloor: String?,
    val planRoom: String?,
    val barriersFilter: JsonElement,
    val besideEntrance: BesideEntrance,
)

/**
 * Pola stylu dla zaznaczenia [selection] na mapie z danymi [data]. Budynek [MapSelection.building] ma obrys,
 * a bez wejścia jako jedyny znaki wejść przy każdym powiększeniu; bez zaznaczenia znaki są od powiększenia 17. Plan
 * [MapSelection.plan] pokazuje wybrane piętro i bariery tego piętra zamiast budynku z OpenStreetMap; dane planu
 * idą osobno przez [showPlanData]. Przygaszenie i obrysy opisuje [SelectionLook]. Bariery widać według
 * [barriersByZoom], a bariery zaznaczonego budynku przy każdym powiększeniu.
 *
 * Wymagania: US1.1, US1.4, US2.8, US2.1, NFR-15, US1.3, US16.1
 */
internal fun selectionLook(data: CampusMapData, selection: MapSelection): SelectionLook {
    val chosen = data.outlineOf(selection.building)
    val alone = chosen.takeIf { selection.entrance == null }
    val planned = selection.plan?.let { data.outlineOf(it.building) }
    val kept = listOfNotNull(alone, planned)
    val entrancesZoom = if (alone == null) MARKS_MIN_ZOOM else ANY_ZOOM
    return SelectionLook(
        dimmed = data.outlines.map { kept.isNotEmpty() && it !in kept },
        underPlan = data.outlines.map { it == planned },
        selectedFilter = chosen?.takeIf { it != planned }?.let(::ofBuildings) ?: NO_FEATURES,
        selectedPlanFilter =
            selection.plan?.takeIf { chosen != null && chosen == planned }?.let { planOutlineFilter(it.floor) }
                ?: NO_FEATURES,
        labelsFilter = planned?.let(::withoutLabel) ?: ALL_FEATURES,
        entrancesFilter = alone?.let(::ofBuildings) ?: ALL_FEATURES,
        entrancesMinZoom = entrancesZoom,
        chosenBarrierFilter = selection.barrier?.let(::ofBarrier) ?: NO_FEATURES,
        chosenEntrance = featureCollection(selection.entrance?.let(::chosenFeatures).orEmpty()),
        planFloor = selection.plan?.floor,
        planRoom = selection.plan?.room,
        barriersFilter =
            expression(
                "all",
                barriersOnPlan(building = selection.plan?.building, floor = selection.plan?.floor),
                barriersByZoom(chosen),
            ),
        besideEntrance = BesideEntrance(minZoom = entrancesZoom, buildings = alone),
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

    /** Ustawia pole [property] układu warstwy [layer] na [value]: wyrażenie stylu albo stałą. */
    fun layout(layer: String, property: String, value: JsonElement)

    /** Ustawia najmniejsze powiększenie [zoom], przy którym rysuje się warstwa [layer]. */
    fun minZoom(layer: String, zoom: Double)

    /** Zastępuje dane źródła GeoJSON [source] tekstem [geoJson]. */
    fun data(source: String, geoJson: String)

    /** Dodaje albo zastępuje obraz [name]. */
    fun image(name: String, image: Bitmap)

    /** Zmienia krycie warstwy [layer] na [opacity] w ciągu [millis] ms; 0 ms od razu. */
    fun fade(layer: String, opacity: Float, millis: Long)
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
    look.underPlan.forEachIndexed { outline, isUnder ->
        state(source = BUILDINGS_SOURCE, feature = outline, key = UNDER_PLAN_STATE, value = isUnder)
    }
    filter(layer = SELECTED_LAYER, filter = look.selectedFilter)
    filter(layer = SELECTED_PLAN_LAYER, filter = look.selectedPlanFilter)
    filter(layer = LABELS_LAYER, filter = look.labelsFilter)
    filter(layer = ENTRANCES_LAYER, filter = look.entrancesFilter)
    minZoom(layer = ENTRANCES_LAYER, zoom = look.entrancesMinZoom)
    filter(layer = CHOSEN_BARRIER_LAYER, filter = look.chosenBarrierFilter)
    data(source = CHOSEN_SOURCE, geoJson = look.chosenEntrance.asStyleText())
    showPlanFloor(floor = look.planFloor)
    showChosenRoom(floor = look.planFloor, room = look.planRoom)
    filter(layer = BARRIERS_LAYER, filter = look.barriersFilter)
    filter(layer = BARRIER_WORDS_LAYER, filter = look.barriersFilter)
    showBesideEntrance(look.besideEntrance)
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

/** Ukrywa podkład do wczytania jego kafelków; odsłania go [revealBasemap]. Wymagania: US1.8, NFR-56 */
internal fun MapStyleTarget.hideBasemap() {
    BASEMAP_LAYERS.forEach { fade(layer = it, opacity = 0f, millis = 0L) }
}

/** Odsłania podkład ukryty przez [hideBasemap], przenikaniem w [millis] ms. Wymagania: US1.8, NFR-56 */
internal fun MapStyleTarget.revealBasemap(millis: Long) {
    BASEMAP_LAYERS.forEach { fade(layer = it, opacity = 1f, millis = millis) }
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
