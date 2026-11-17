/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import kotlin.math.PI
import kotlin.math.roundToLong

private const val TAP_MILLIS = 300.0
private const val TAP_MARGIN_DP = 16.0
private const val HALF_TURN_DEGREES = 180.0
private const val VISIBLE_TOLERANCE_DP = 0.5

/**
 * Ruch mapy po dotknięciu konturu [outline] na widocznej mapie. Widok [from] dotyczy części mapy, której środek leży
 * [belowDp] niżej niż środek wolnej części [free]: pod paskiem przed otwarciem karty, 0 przy otwartej karcie. Kontur
 * cały widoczny w wolnej części z marginesem 16 dp: kamera bez ruchu, karta w 300 ms × [animationScale]. Kontur
 * zasłonięty albo za brzegiem: najmniejsze przesunięcie w 300 ms, w tym samym powiększeniu. Kontur większy niż
 * wolna część: oddalenie do zmieszczenia.
 *
 * Wymagania: US2.8, US3.2, NFR-56
 */
internal fun tapTransition(
    from: MapCamera,
    outline: CampusBounds,
    free: FreeMap,
    belowDp: Double,
    animationScale: Float,
): CameraTransition {
    val view = from.shiftedNorth(belowDp)
    val fits = fittingZoom(outline, free)
    val target =
        if (view.zoom > fits) centered(outline = outline, zoom = fits, free = free) else view.revealing(outline, free)
    val millis = (TAP_MILLIS * animationScale).roundToLong()
    val motion = if (target == null || millis == 0L) CameraMotion.NONE else CameraMotion.EASE
    return CameraTransition(camera = target ?: view, motion = motion, durationMillis = millis)
}

private fun MapCamera.shiftedNorth(dp: Double): MapCamera {
    if (dp == 0.0) return this
    val y = mercatorY(center.latitude) + dp / dpPerRadian(zoom)
    return copy(center = center.copy(latitude = latitudeOf(y)))
}

private fun MapCamera.revealing(outline: CampusBounds, free: FreeMap): MapCamera? {
    val perDegree = dpPerDegree(zoom)
    val perRadian = dpPerRadian(zoom)
    val middle = mercatorY(center.latitude)
    val halfWidth = free.widthDp / 2 - TAP_MARGIN_DP
    val halfHeight = free.heightDp / 2 - TAP_MARGIN_DP
    val east =
        overflow(
            low = (outline.west - center.longitude) * perDegree,
            high = (outline.east - center.longitude) * perDegree,
            min = -halfWidth,
            max = halfWidth - free.endDp,
        )
    val south =
        overflow(
            low = (middle - mercatorY(outline.north)) * perRadian,
            high = (middle - mercatorY(outline.south)) * perRadian,
            min = -halfHeight,
            max = halfHeight,
        )
    if (east == 0.0 && south == 0.0) return null
    val shifted =
        GeoPoint(longitude = center.longitude + east / perDegree, latitude = latitudeOf(middle - south / perRadian))
    return copy(center = shifted)
}

private fun overflow(low: Double, high: Double, min: Double, max: Double): Double =
    when {
        low < min - VISIBLE_TOLERANCE_DP -> low - min
        high > max + VISIBLE_TOLERANCE_DP -> high - max
        else -> 0.0
    }

private fun dpPerRadian(zoom: Double): Double = dpPerDegree(zoom) * HALF_TURN_DEGREES / PI
