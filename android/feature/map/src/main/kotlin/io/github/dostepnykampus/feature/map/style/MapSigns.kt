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
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.Severity
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
