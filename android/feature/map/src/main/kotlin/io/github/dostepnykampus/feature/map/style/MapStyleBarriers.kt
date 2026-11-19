/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.barrier.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.barrier.BARRIER_FLOOR
import io.github.dostepnykampus.feature.map.barrier.BARRIER_ID
import io.github.dostepnykampus.feature.map.barrier.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.barrier.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.barrier.BARRIER_WORD_OFFSET
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

/** Bok obrazu znaku wybranej bariery w dp: większy znak w obwódce. */
internal const val CHOSEN_BARRIER_DP = 64

/** Powiększenie, od którego obok znaku bariery stoi słowo uciążliwości. */
internal const val BARRIER_WORD_ZOOM = 17

/** Powiększenie, od którego widać „Utrudnienie” i „Tylko z pomocą”; „Blokadę” widać przy każdym. */
internal const val MINOR_BARRIER_ZOOM = 16

private const val WORD_OFFSET_DP = BARRIER_SIGN_DP / 2 + 4

/** Przesunięcie znaku bariery przy wejściu z właściwości punktu; bez niej znak stoi na punkcie. */
private val SIGN_OFFSET = "icon-offset" to offsetOr(BARRIER_OFFSET, numbers(0.0, 0.0))

/** Nazwa obrazu znaku bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun barrierSignImage(severity: Severity): String = "bariera_${severity.name}"

/** Nazwa obrazu wyróżnionego znaku wybranej bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun chosenBarrierImage(severity: Severity): String = "bariera_wybrana_${severity.name}"

/** Nazwa obrazu słowa uciążliwości [severity] obok znaku bariery. */
internal fun barrierWordImage(severity: Severity): String = "bariera_slowo_${severity.name}"

/**
 * Warstwy barier: słowo uciążliwości obok znaku i sam znak. Znak rysuje się zawsze, a jego kształt zależy
 * od uciążliwości, więc znaczenie niesie kształt, a barwa je powtarza. Słowo jest obrazem, bo napisy z czcionek
 * stylu wymagają sieci. Pojawia się od powiększenia [BARRIER_WORD_ZOOM], nie przy krawędzi mapy i tylko wtedy,
 * gdy mieści się bez nakładania. Warstwy stoją pod podpisami budynków, więc przy kolizji zostaje podpis budynku
 * (wiersz o znakach barier w wymaganiach V1). Znak i słowo bariery przy wejściu stoją przesunięte o wartości
 * z właściwości punktu. Filtr [filter] z [SelectionLook.barriersFilter] zostawia bariery
 * piętra planu i bariery widoczne przy bieżącym powiększeniu.
 *
 * Wymagania: US1.3, NFR-21, NFR-20
 */
internal fun barrierLayers(filter: JsonElement): List<JsonObject> {
    val words =
        symbolLayer(
            id = BARRIER_WORDS_LAYER,
            source = BARRIERS_SOURCE,
            image = bySeverity(::barrierWordImage),
            "icon-anchor" to JsonPrimitive("left"),
            "icon-offset" to offsetOr(BARRIER_WORD_OFFSET, numbers(WORD_OFFSET_DP.toDouble(), 0.0)),
            "symbol-avoid-edges" to JsonPrimitive(true),
        )
    val signs =
        symbolLayer(
            id = BARRIERS_LAYER,
            source = BARRIERS_SOURCE,
            image = bySeverity(::barrierSignImage),
            OVERLAP,
            SIGN_OFFSET,
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
 * i rysuje się zawsze.
 *
 * Wymagania: US1.5, NFR-21
 */
internal fun chosenBarrierLayer(filter: JsonElement): JsonObject {
    val sign =
        symbolLayer(
            id = CHOSEN_BARRIER_LAYER,
            source = BARRIERS_SOURCE,
            image = bySeverity(::chosenBarrierImage),
            OVERLAP,
            SIGN_OFFSET,
        )
    return JsonObject(sign + (FILTER to filter))
}

/** Filtr warstwy [CHOSEN_BARRIER_LAYER]: tylko bariera [id]. */
internal fun ofBarrier(id: String): JsonElement = expression("==", property(BARRIER_ID), JsonPrimitive(id))

private fun bySeverity(image: (Severity) -> String): JsonArray {
    val named = Severity.entries.dropLast(n = 1).flatMap { listOf(JsonPrimitive(it.name), JsonPrimitive(image(it))) }
    val other = JsonPrimitive(image(Severity.entries.last()))
    return JsonArray(listOf(JsonPrimitive("match"), property(BARRIER_SEVERITY)) + named + other)
}

private fun offsetOr(key: String, fallback: JsonArray): JsonArray =
    expression("coalesce", property(key), expression("literal", fallback))
