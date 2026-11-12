/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.data.symbolsOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SELECTED_WIDTH = 4.0
private const val SYMBOLS = "symbols"

/** Podpis konturu budynku [building] wśród [labels]; `null` bez zaznaczenia i dla budynku spoza katalogu. */
internal fun selectedLabel(labels: List<String>, building: String?): String? =
    labels.firstOrNull { building in symbolsOf(listOf(it)) }

/**
 * Barwa konturów przy zaznaczonym konturze [label]: [own] dla niego, [other] dla pozostałych, więc sąsiednie
 * budynki są przygaszone (US2.8, decyzja 153). Bez zaznaczenia wszystkie kontury mają barwę [own].
 */
internal fun tone(label: String?, own: String, other: String): JsonElement =
    label?.let { expression("case", isLabel(it), JsonPrimitive(own), JsonPrimitive(other)) } ?: JsonPrimitive(own)

/** Warstwa grubszego obrysu `pwr_wybrany` konturu o podpisie [label] (US1.1). */
internal fun selectedLayer(label: String, colors: MapStyleColors): JsonObject =
    lineLayer(
        id = SELECTED_LAYER,
        source = BUILDINGS_SOURCE,
        color = colors.selectedOutline,
        width = SELECTED_WIDTH,
        filter = isLabel(label),
    )

/**
 * Filtr znaków wejść: zostają wejścia budynków konturu o podpisie [label], także wejście wspólne z innym
 * budynkiem (US1.4, US2.1, decyzja 153). Kontur wspólny „C-3/C-4” zostawia wejścia obu budynków. Warunek stoi
 * w wyrażeniu `case`, bo sam operator `any` albo `in` MapLibre czyta jako filtr w dawnym zapisie.
 */
internal fun entrancesOf(label: String): JsonElement {
    val symbols = symbolsOf(listOf(label)).map { expression("in", JsonPrimitive(it), property(SYMBOLS)) }
    val any = JsonArray(listOf(JsonPrimitive("any")) + symbols)
    return expression("case", any, JsonPrimitive(true), JsonPrimitive(false))
}

private fun isLabel(label: String): JsonElement = expression("==", property(LABEL), JsonPrimitive(label))
