/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.map.data.BARRIER_ID
import io.github.dostepnykampus.feature.map.data.BARRIER_SEVERITY
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

/** Bok obrazu znaku bariery na mapie w dp. */
internal const val BARRIER_SIGN_DP = 32

/** Bok obrazu znaku wybranej bariery w dp: większy znak w obwódce. */
internal const val CHOSEN_BARRIER_DP = 64

/** Powiększenie, od którego obok znaku bariery stoi słowo uciążliwości. */
internal const val BARRIER_WORD_ZOOM = 17

private const val WORD_OFFSET_DP = BARRIER_SIGN_DP / 2 + 4

/** Nazwa obrazu znaku bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun barrierSignImage(severity: Severity): String = "bariera_${severity.name}"

/** Nazwa obrazu wyróżnionego znaku wybranej bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun chosenBarrierImage(severity: Severity): String = "bariera_wybrana_${severity.name}"

/** Nazwa obrazu słowa uciążliwości [severity] obok znaku bariery. */
internal fun barrierWordImage(severity: Severity): String = "bariera_slowo_${severity.name}"

/**
 * Warstwy barier: słowo uciążliwości obok znaku i sam znak. Znak rysuje się zawsze, a jego kształt zależy
 * od uciążliwości, więc znaczenie niesie kształt, nie barwa. Słowo jest obrazem, bo napisy z czcionek
 * stylu wymagają sieci. Pojawia się od powiększenia [BARRIER_WORD_ZOOM], nie przy krawędzi mapy i tylko wtedy,
 * gdy mieści się bez nakładania. Warstwy stoją pod podpisami budynków, więc przy kolizji zostaje podpis budynku
 * (wiersz o znakach barier w wymaganiach V1).
 *
 * Wymagania: US1.3, NFR-21, NFR-20
 */
internal fun barrierLayers(): List<JsonObject> {
    val words =
        symbolLayer(
            id = BARRIER_WORDS_LAYER,
            source = BARRIERS_SOURCE,
            image = bySeverity(::barrierWordImage),
            "icon-anchor" to JsonPrimitive("left"),
            "icon-offset" to numbers(WORD_OFFSET_DP.toDouble(), 0.0),
            "symbol-avoid-edges" to JsonPrimitive(true),
        )
    val signs =
        symbolLayer(id = BARRIERS_LAYER, source = BARRIERS_SOURCE, image = bySeverity(::barrierSignImage), OVERLAP)
    return listOf(JsonObject(words + ("minzoom" to JsonPrimitive(BARRIER_WORD_ZOOM))), signs)
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
        )
    return JsonObject(sign + ("filter" to filter))
}

/** Filtr warstwy [CHOSEN_BARRIER_LAYER]: tylko bariera [id]. */
internal fun ofBarrier(id: String): JsonElement = expression("==", property(BARRIER_ID), JsonPrimitive(id))

private fun bySeverity(image: (Severity) -> String): JsonArray {
    val named = Severity.entries.dropLast(n = 1).flatMap { listOf(JsonPrimitive(it.name), JsonPrimitive(image(it))) }
    val other = JsonPrimitive(image(Severity.entries.last()))
    return JsonArray(listOf(JsonPrimitive("match"), property(BARRIER_SEVERITY)) + named + other)
}
