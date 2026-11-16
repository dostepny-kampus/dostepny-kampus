/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.feature.map.campus.LABEL_SEPARATOR
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Stan obiektu konturu budynku w źródle [BUILDINGS_SOURCE]: budynek pod widocznym planem piętra. */
internal const val UNDER_PLAN_STATE = "pod planem"

/** Warstwa grubszego obrysu `pwr_wybrany` po ścianie piętra wybranego budynku z widocznym planem. */
internal const val SELECTED_PLAN_LAYER = "zaznaczenie_plan"

/**
 * Krycie tła i konturu budynku ze stanu obiektu: budynek ze stanem [UNDER_PLAN_STATE] jest niewidoczny, bo jego
 * miejsce zajmuje plan piętra. Wyrażenie jest stałe, jak barwa z [tone].
 *
 * Wymagania: US16.1
 */
internal fun opacityUnderPlan(): JsonElement =
    expression(
        "case",
        expression("boolean", expression("feature-state", JsonPrimitive(UNDER_PLAN_STATE)), JsonPrimitive(false)),
        JsonPrimitive(0.0),
        JsonPrimitive(1.0),
    )

/** Warstwa [SELECTED_PLAN_LAYER] z filtrem [filter] z [SelectionLook.selectedPlanFilter]. Wymagania: US1.1, US16.1 */
internal fun selectedPlanLayer(filter: JsonElement, colors: MapStyleColors): JsonObject =
    lineLayer(
        id = SELECTED_PLAN_LAYER,
        source = PLAN_SOURCE,
        color = colors.selectedOutline,
        width = SELECTED_WIDTH,
        filter = filter,
    )

/** Filtr warstwy symboli bez symbolu konturu budynków [symbols], np. pod jego planem piętra. Wymagania: US16.1 */
internal fun withoutLabel(symbols: List<String>): JsonElement =
    expression("!=", property(LABEL), JsonPrimitive(symbols.joinToString(separator = LABEL_SEPARATOR)))

/** Filtr ściany piętra [floor] z danych planu; bez piętra nie przepuszcza niczego. Wymagania: US16.1 */
internal fun planOutlineFilter(floor: String?): JsonElement = planFilter(floor = floor, kind = "outline")
