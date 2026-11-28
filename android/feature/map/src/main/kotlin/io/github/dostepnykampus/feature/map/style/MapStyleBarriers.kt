/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.map.style.OVERLAP
import io.github.dostepnykampus.core.map.style.expression
import io.github.dostepnykampus.core.map.style.numbers
import io.github.dostepnykampus.core.map.style.property
import io.github.dostepnykampus.core.map.style.symbolLayer
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.campus.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.campus.BARRIER_ENTRANCE
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

/** Źródło stylu z punktami barier. */
internal const val BARRIERS_SOURCE = BARRIERS_LAYER

private const val FILTER = "filter"
private const val LITERAL = "literal"

/** Bok obrazu znaku bariery na mapie w dp. */
internal const val BARRIER_SIGN_DP = 32

/** Bok obrazu znaku bariery bez piętra na planie w dp: mieści dopisek „?” w rogu znaku. */
internal const val BARRIER_NO_FLOOR_DP = 40

/** Powiększenie, od którego obok znaku bariery stoi słowo uciążliwości. */
internal const val BARRIER_WORD_ZOOM = 17

/** Powiększenie, od którego widać „Utrudnienie” i „Tylko z pomocą”; „Blokadę” widać przy każdym. */
internal const val MINOR_BARRIER_ZOOM = 16

private const val WORD_OFFSET_DP = BARRIER_SIGN_DP / 2 + 4
private const val PINNED_BARRIER_DP = 48.0

private const val ICON_OFFSET = "icon-offset"

private const val ICON_IMAGE = "icon-image"

/**
 * Znak bariery obok pinezki [pinnedEntrance], obok znaku wejścia od [minZoom] dla [buildings]
 * (`null` dla wszystkich), albo w punkcie bariery.
 *
 * Wymagania: US2.2, US3.3, NFR-17, NFR-18
 */
internal class BesideEntrance(val minZoom: Double, val buildings: List<String>?, private val pinnedEntrance: String?) {
    /** Czy przy powiększeniu [zoom] znak bariery przy wejściu budynku [building] stoi obok znaku wejścia. */
    fun isBeside(zoom: Double, building: String?): Boolean =
        zoom >= minZoom && (buildings == null || building in buildings)

    /** Poziome przesunięcie znaku po prawej od pinezki, w dp, albo `null` przy innym wejściu. */
    fun pinnedOffset(entrance: String?): Double? =
        PINNED_BARRIER_DP.takeIf { entrance != null && entrance == pinnedEntrance }

    /** Pole `icon-offset` znaku bariery. */
    fun signOffset(): JsonElement = shifted(key = BARRIER_OFFSET, atAnchor = 0.0)

    /** Pole `icon-offset` słowa uciążliwości: obok znaku bariery tam, gdzie stoi znak. */
    fun wordOffset(): JsonElement = shifted(key = BARRIER_WORD_OFFSET, atAnchor = WORD_OFFSET_DP.toDouble())

    /** Przesunięcie podpisu o [gap] dp poniżej środka znaku. */
    fun captionOffset(gap: Double): JsonElement = shifted(key = BARRIER_CAPTION_OFFSET, atAnchor = 0.0, below = gap)

    private fun shifted(key: String, atAnchor: Double, below: Double = 0.0): JsonElement {
        val still = numbers(atAnchor, below)
        val stays = expression(LITERAL, still)
        val moves = offsetOr(key, still)
        val symbols = buildings?.map(::JsonPrimitive)
        val building = expression("to-string", property(BARRIER_BUILDING))
        val byBuilding =
            when {
                symbols == null -> moves
                symbols.isEmpty() -> stays
                else -> expression("match", building, JsonArray(symbols), moves, stays)
            }
        val atPin = expression(LITERAL, numbers(PINNED_BARRIER_DP + atAnchor, below))
        return expression(
            "step",
            expression("zoom"),
            besidePin(base = stays, offset = atPin),
            JsonPrimitive(minZoom),
            besidePin(base = byBuilding, offset = atPin),
        )
    }

    private fun besidePin(base: JsonElement, offset: JsonArray): JsonElement {
        val id = pinnedEntrance ?: return base
        return expression("case", expression("==", property(BARRIER_ENTRANCE), JsonPrimitive(id)), offset, base)
    }
}

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
 * Bariera [barrier] i bariery budynków [buildings] są widoczne przy każdym powiększeniu.
 *
 * Wymagania: US1.3, US10.6, NFR-19
 */
internal fun barriersByZoom(buildings: List<String>?, barrier: String?): JsonElement {
    val blocking = expression("==", property(BARRIER_SEVERITY), JsonPrimitive(Severity.BLOCKING.name))
    val zoomed = expression(">=", expression("zoom"), JsonPrimitive(MINOR_BARRIER_ZOOM))
    val selected = buildings.orEmpty().map { expression("==", property(BARRIER_BUILDING), JsonPrimitive(it)) }
    val chosen = barrier?.let { expression("==", property(BARRIER_ID), JsonPrimitive(it)) }
    return JsonArray(listOf(JsonPrimitive("any"), blocking, zoomed) + selected + listOfNotNull(chosen))
}

/**
 * Ustawia w stylu położenie znaków i słów barier przy wejściach według [beside]; po wywołaniu warstwy barier są
 * takie, jak z [barrierLayers] z tym samym [beside].
 *
 * Wymagania: US2.2, NFR-17
 */
internal fun MapStyleTarget.showBesideEntrance(beside: BesideEntrance) {
    layout(layer = BARRIERS_LAYER, property = ICON_OFFSET, value = beside.signOffset())
    layout(layer = BARRIER_WORDS_LAYER, property = ICON_OFFSET, value = beside.wordOffset())
}

/**
 * Ustawia w stylu obrazy znaków barier przy planie budynku [planBuilding]; po wywołaniu warstwy barier mają
 * takie obrazy, jak z [barrierLayers] dla tego samego planu.
 *
 * Wymagania: US16.1, BR-14
 */
internal fun MapStyleTarget.showPlanBarriers(planBuilding: String?) {
    layout(layer = BARRIERS_LAYER, property = ICON_IMAGE, value = barrierSignsImage(planBuilding))
}

/** Przesunięcie z właściwości [key] punktu, a bez niej [fallback]. */
internal fun offsetOr(key: String, fallback: JsonArray): JsonArray =
    expression("coalesce", property(key), expression(LITERAL, fallback))
