/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import android.graphics.Bitmap
import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.designsystem.marking.mapSeverityImage
import io.github.dostepnykampus.core.map.style.expression
import io.github.dostepnykampus.core.map.style.property
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.campus.BARRIER_BUILDING
import io.github.dostepnykampus.feature.map.campus.BARRIER_FLOOR
import io.github.dostepnykampus.feature.map.campus.BARRIER_SEVERITY
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.ceil

/**
 * Znak oznaczenia [marking] na mapie i w legendzie, w barwach znaków mapy z [colors], przy gęstości [density].
 * „Brak danych” to cichy znak wielkości tokenu `ikony.znak_mapy_brak_danych`, pozostałe mają [ENTRANCE_MARK_DP].
 *
 * Wymagania: US2.1, US1.4, NFR-21
 */
internal fun entranceSign(marking: EntranceMarking, colors: MapColors, density: Float): Bitmap {
    val dp = if (marking == EntranceMarking.NO_DATA) Ikony.znakMapyBrakDanych.value else ENTRANCE_MARK_DP.toFloat()
    return mapMarkingImage(marking = marking, sizePx = ceil(dp * density).toInt(), colors = colors)
}

/**
 * Znak uciążliwości [severity] na mapie i w legendzie: kształt w barwach znaków mapy z [colors], wielkości
 * [BARRIER_SIGN_DP] przy gęstości [density].
 *
 * Wymagania: US1.3, NFR-21
 */
internal fun barrierSign(severity: Severity, colors: MapColors, density: Float): Bitmap =
    mapSeverityImage(severity = severity, sizePx = ceil(BARRIER_SIGN_DP * density).toInt(), colors = colors)

/** Nazwa obrazu znaku bariery o uciążliwości [severity]. Wymagania: NFR-21 */
internal fun barrierSignImage(severity: Severity): String = "bariera_${severity.name}"

/** Nazwa obrazu znaku bariery o uciążliwości [severity] bez piętra na planie. Wymagania: US16.1, BR-14 */
internal fun barrierNoFloorImage(severity: Severity): String = "bariera_bez_pietra_${severity.name}"

/** Nazwa obrazu słowa uciążliwości [severity] obok znaku bariery. */
internal fun barrierWordImage(severity: Severity): String = "bariera_slowo_${severity.name}"

/** Wyrażenie obrazu warstwy [BARRIERS_LAYER] dla planu budynku [planBuilding]. Wymagania: US16.1, BR-14 */
internal fun barrierSignsImage(planBuilding: String?): JsonElement =
    barrierImage(planBuilding = planBuilding, regular = ::barrierSignImage, noFloor = ::barrierNoFloorImage)

private fun barrierImage(
    planBuilding: String?,
    regular: (Severity) -> String,
    noFloor: (Severity) -> String,
): JsonElement {
    val base = bySeverity(regular)
    if (planBuilding == null) return base
    val unknownFloor =
        expression(
            "all",
            expression("==", property(BARRIER_BUILDING), JsonPrimitive(planBuilding)),
            expression("!", expression("has", JsonPrimitive(BARRIER_FLOOR))),
        )
    return expression("case", unknownFloor, bySeverity(noFloor), base)
}

/** Wyrażenie wyboru obrazu [image] według uciążliwości punktu. Wymagania: NFR-21 */
internal fun bySeverity(image: (Severity) -> String): JsonArray {
    val named = Severity.entries.dropLast(n = 1).flatMap { listOf(JsonPrimitive(it.name), JsonPrimitive(image(it))) }
    val other = JsonPrimitive(image(Severity.entries.last()))
    return JsonArray(listOf(JsonPrimitive("match"), property(BARRIER_SEVERITY)) + named + other)
}
