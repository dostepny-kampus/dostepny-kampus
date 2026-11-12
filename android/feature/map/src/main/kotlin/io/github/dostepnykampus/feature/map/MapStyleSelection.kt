/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

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

/** Warstwa grubszego obrysu `pwr_wybrany` konturu budynków [outline] (US1.1). */
internal fun selectedLayer(outline: List<String>, colors: MapStyleColors): JsonObject =
    lineLayer(
        id = SELECTED_LAYER,
        source = BUILDINGS_SOURCE,
        color = colors.selectedOutline,
        width = SELECTED_WIDTH,
        filter = ofBuildings(outline),
    )

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
