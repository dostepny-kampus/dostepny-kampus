/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.campus.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.campus.BARRIER_FLOOR
import io.github.dostepnykampus.feature.map.campus.BARRIER_ID
import io.github.dostepnykampus.feature.map.campus.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.campus.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.campus.BARRIER_WORD_OFFSET
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Warstwa stylu ze znakami uciążliwości barier. Wymagania: US1.3, NFR-21 */
internal const val BARRIERS_LAYER = "bariery"

/** Warstwa stylu ze słowami uciążliwości obok znaków barier. Wymagania: US1.3, NFR-20 */
internal const val BARRIER_WORDS_LAYER = "bariery_slowa"

/** Warstwa stylu z wyróżnionym znakiem wybranej bariery. Wymagania: US1.5, NFR-21 */
internal const val CHOSEN_BARRIER_LAYER = "bariera_wybrana"

/** Źródło stylu z punktami barier. */
internal const val BARRIERS_SOURCE = BARRIERS_LAYER

private const val FILTER = "filter"

/** Bok obrazu znaku bariery na mapie w dp. */
internal const val BARRIER_SIGN_DP = 32

/** Bok obrazu znaku bariery bez piętra na planie w dp: mieści dopisek „?” w rogu znaku. */
internal const val BARRIER_NO_FLOOR_DP = 40

/** Bok obrazu znaku wybranej bariery w dp: większy znak w obwódce. */
internal const val CHOSEN_BARRIER_DP = 64

/** Powiększenie, od którego obok znaku bariery stoi słowo uciążliwości. */
internal const val BARRIER_WORD_ZOOM = 17

/** Powiększenie, od którego widać „Utrudnienie” i „Tylko z pomocą”; „Blokadę” widać przy każdym. */
internal const val MINOR_BARRIER_ZOOM = 16

private const val WORD_OFFSET_DP = BARRIER_SIGN_DP / 2 + 4

private const val ICON_OFFSET = "icon-offset"

private const val ICON_IMAGE = "icon-image"

/**
 * Warunki, przy których znak bariery przy wejściu stoi obok znaku wejścia, a nie w punkcie wejścia: te same, przy
 * których widać znak wejścia, czyli od powiększenia [minZoom] i tylko przy wejściach budynków [buildings]; `null`
 * przy wejściach wszystkich budynków.
 *
 * Wymagania: US2.2, NFR-17
 */
internal class BesideEntrance(val minZoom: Double, val buildings: List<String>?) {
    /** Czy przy powiększeniu [zoom] znak bariery przy wejściu budynku [building] stoi obok znaku wejścia. */
    fun isBeside(zoom: Double, building: String?): Boolean =
        zoom >= minZoom && (buildings == null || building in buildings)

    /** Pole `icon-offset` znaku bariery: przesunięcie z punktu obok widocznego znaku wejścia, inaczej zero. */
    fun signOffset(): JsonElement = shifted(key = BARRIER_OFFSET, still = numbers(0.0, 0.0))

    /** Pole `icon-offset` słowa uciążliwości: obok znaku bariery tam, gdzie stoi znak. */
    fun wordOffset(): JsonElement = shifted(key = BARRIER_WORD_OFFSET, still = numbers(WORD_OFFSET_DP.toDouble(), 0.0))

    private fun shifted(key: String, still: JsonArray): JsonElement {
        val stays = expression("literal", still)
        val moves = offsetOr(key, still)
        val symbols = buildings?.map(::JsonPrimitive)
        val building = expression("to-string", property(BARRIER_BUILDING))
        val byBuilding =
            when {
                symbols == null -> moves
                symbols.isEmpty() -> stays
                else -> expression("match", building, JsonArray(symbols), moves, stays)
            }
        return expression("step", expression("zoom"), stays, JsonPrimitive(minZoom), byBuilding)
    }
}

/** Nazwa obrazu znaku bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun barrierSignImage(severity: Severity): String = "bariera_${severity.name}"

/** Nazwa obrazu znaku bariery o uciążliwości [severity] bez piętra na planie. Wymagania: US16.1, BR-14 */
internal fun barrierNoFloorImage(severity: Severity): String = "bariera_bez_pietra_${severity.name}"

/** Nazwa obrazu wyróżnionego znaku wybranej bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun chosenBarrierImage(severity: Severity): String = "bariera_wybrana_${severity.name}"

/** Nazwa obrazu wyróżnionego znaku bariery [severity] bez piętra na planie. Wymagania: US16.1, BR-14 */
internal fun chosenNoFloorImage(severity: Severity): String = "bariera_wybrana_bez_pietra_${severity.name}"

/** Nazwa obrazu słowa uciążliwości [severity] obok znaku bariery. */
internal fun barrierWordImage(severity: Severity): String = "bariera_slowo_${severity.name}"

/**
 * Warstwy barier: słowo uciążliwości obok znaku i sam znak. Znak rysuje się zawsze, a jego kształt zależy
 * od uciążliwości, więc znaczenie niesie kształt, a barwa je powtarza. Słowo jest obrazem, bo napisy z czcionek
 * stylu wymagają sieci. Pojawia się od powiększenia [BARRIER_WORD_ZOOM], nie przy krawędzi mapy i tylko wtedy,
 * gdy mieści się bez nakładania. Znak i słowo przy wejściu przesuwa [beside], filtr [filter] zostawia widoczne
 * bariery, a przy widocznym planie budynku [planBuilding] bariera bez piętra ma znak z „?”.
 *
 * Wymagania: US1.3, NFR-21, NFR-20, US2.2, US16.1, BR-14
 */
internal fun barrierLayers(
    filter: JsonElement,
    beside: BesideEntrance,
    planBuilding: String? = null,
): List<JsonObject> {
    val words =
        symbolLayer(
            id = BARRIER_WORDS_LAYER,
            source = BARRIERS_SOURCE,
            image = bySeverity(::barrierWordImage),
            "icon-anchor" to JsonPrimitive("left"),
            ICON_OFFSET to beside.wordOffset(),
            "symbol-avoid-edges" to JsonPrimitive(true),
        )
    val signs =
        symbolLayer(
            id = BARRIERS_LAYER,
            source = BARRIERS_SOURCE,
            image = barrierSignsImage(planBuilding),
            OVERLAP,
            ICON_OFFSET to beside.signOffset(),
        )
    return listOf(
        JsonObject(words + ("minzoom" to JsonPrimitive(BARRIER_WORD_ZOOM)) + (FILTER to filter)),
        JsonObject(signs + (FILTER to filter)),
    )
}

/**
 * Filtr warstw barier przy planie piętra [floor] budynku [building]: bariery tego budynku z innego piętra znikają,
 * a bariery bez piętra i spoza budynku zostają. Bez planu (`null`) przepuszcza wszystkie.
 *
 * Wymagania: US16.1, BR-14
 */
internal fun barriersOnPlan(building: String?, floor: String?): JsonElement =
    expression(
        "any",
        expression("!=", property(BARRIER_BUILDING), JsonPrimitive(building.orEmpty())),
        expression("!", expression("has", JsonPrimitive(BARRIER_FLOOR))),
        expression("==", property(BARRIER_FLOOR), JsonPrimitive(floor.orEmpty())),
        JsonPrimitive(building == null),
    )

/**
 * Filtr warstw barier według powiększenia: „Blokada” przy każdym, pozostałe uciążliwości od [MINOR_BARRIER_ZOOM].
 * Bariery budynków [buildings] (konturu zaznaczonego budynku) przechodzą przy każdym powiększeniu; `null` bez
 * zaznaczenia.
 *
 * Wymagania: US1.3, US10.6, NFR-19
 */
internal fun barriersByZoom(buildings: List<String>?): JsonElement {
    val blocking = expression("==", property(BARRIER_SEVERITY), JsonPrimitive(Severity.BLOCKING.name))
    val zoomed = expression(">=", expression("zoom"), JsonPrimitive(MINOR_BARRIER_ZOOM))
    val selected = buildings.orEmpty().map { expression("==", property(BARRIER_BUILDING), JsonPrimitive(it)) }
    return JsonArray(listOf(JsonPrimitive("any"), blocking, zoomed) + selected)
}

/**
 * Warstwa wybranej bariery z filtrem [filter] z [SelectionLook.chosenBarrierFilter]: jej znak jest większy
 * i w obwódce, więc wyróżnia go rozmiar i kształt, a nie sama barwa. Warstwa stoi nad pozostałymi znakami
 * i rysuje się zawsze; znak stoi tam, gdzie znak tej bariery według [beside], a dla bariery bez piętra na planie
 * budynku [planBuilding] ma znak z „?”.
 *
 * Wymagania: US1.5, NFR-21, US2.2, US16.1, BR-14
 */
internal fun chosenBarrierLayer(
    filter: JsonElement,
    beside: BesideEntrance,
    planBuilding: String? = null,
): JsonObject {
    val sign =
        symbolLayer(
            id = CHOSEN_BARRIER_LAYER,
            source = BARRIERS_SOURCE,
            image = chosenSignsImage(planBuilding),
            OVERLAP,
            ICON_OFFSET to beside.signOffset(),
        )
    return JsonObject(sign + (FILTER to filter))
}

/**
 * Ustawia w stylu położenie znaków i słów barier przy wejściach według [beside]; po wywołaniu warstwy barier są
 * takie, jak z [barrierLayers] i [chosenBarrierLayer] z tym samym [beside].
 *
 * Wymagania: US2.2, NFR-17
 */
internal fun MapStyleTarget.showBesideEntrance(beside: BesideEntrance) {
    layout(layer = BARRIERS_LAYER, property = ICON_OFFSET, value = beside.signOffset())
    layout(layer = CHOSEN_BARRIER_LAYER, property = ICON_OFFSET, value = beside.signOffset())
    layout(layer = BARRIER_WORDS_LAYER, property = ICON_OFFSET, value = beside.wordOffset())
}

/**
 * Ustawia w stylu obrazy znaków barier przy planie budynku [planBuilding]; po wywołaniu warstwy barier mają
 * takie obrazy, jak z [barrierLayers] i [chosenBarrierLayer] dla tego samego planu.
 *
 * Wymagania: US16.1, BR-14
 */
internal fun MapStyleTarget.showPlanBarriers(planBuilding: String?) {
    layout(layer = BARRIERS_LAYER, property = ICON_IMAGE, value = barrierSignsImage(planBuilding))
    layout(layer = CHOSEN_BARRIER_LAYER, property = ICON_IMAGE, value = chosenSignsImage(planBuilding))
}

/** Wyrażenie obrazu warstwy [BARRIERS_LAYER] dla planu budynku [planBuilding]. Wymagania: US16.1, BR-14 */
internal fun barrierSignsImage(planBuilding: String?): JsonElement =
    barrierImage(planBuilding = planBuilding, regular = ::barrierSignImage, noFloor = ::barrierNoFloorImage)

/** Wyrażenie obrazu warstwy [CHOSEN_BARRIER_LAYER] dla planu budynku [planBuilding]. Wymagania: US16.1, BR-14 */
internal fun chosenSignsImage(planBuilding: String?): JsonElement =
    barrierImage(planBuilding = planBuilding, regular = ::chosenBarrierImage, noFloor = ::chosenNoFloorImage)

/** Filtr warstwy [CHOSEN_BARRIER_LAYER]: tylko bariera [id]. */
internal fun ofBarrier(id: String): JsonElement = expression("==", property(BARRIER_ID), JsonPrimitive(id))

private fun barrierImage(
    planBuilding: String?,
    regular: (Severity) -> String,
    noFloor: (Severity) -> String,
): JsonElement {
    val base = bySeverity(regular)
    if (planBuilding == null) return base
    val isUnknownFloor =
        expression(
            "all",
            expression("==", property(BARRIER_BUILDING), JsonPrimitive(planBuilding)),
            expression("!", expression("has", JsonPrimitive(BARRIER_FLOOR))),
        )
    return expression("case", isUnknownFloor, bySeverity(noFloor), base)
}

private fun bySeverity(image: (Severity) -> String): JsonArray {
    val named = Severity.entries.dropLast(n = 1).flatMap { listOf(JsonPrimitive(it.name), JsonPrimitive(image(it))) }
    val other = JsonPrimitive(image(Severity.entries.last()))
    return JsonArray(listOf(JsonPrimitive("match"), property(BARRIER_SEVERITY)) + named + other)
}

/** Przesunięcie z właściwości [key] punktu, a bez niej [fallback]. */
internal fun offsetOr(key: String, fallback: JsonArray): JsonArray =
    expression("coalesce", property(key), expression("literal", fallback))
