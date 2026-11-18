/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.feature.map.campus.CampusBounds
import kotlin.math.PI
import kotlin.math.roundToLong

private const val TAP_MILLIS = 300.0
private const val TAP_MARGIN_DP = 16.0
private const val HALF_TURN_DEGREES = 180.0
private const val VISIBLE_TOLERANCE_DP = 0.5

/**
 * Mapa widziana w chwili dotknięcia konturu, w dp od góry wolnej części mapy nad kartą.
 *
 * @property belowDp o ile środek widoku, od którego liczy się dotknięcie, leży niżej niż środek wolnej części
 * @property topDp górny brzeg części mapy, którą osoba widziała przed kartą
 * @property bottomDp dolny brzeg tej części, także pod kartą
 */
internal data class TapSight(val belowDp: Double, val topDp: Double, val bottomDp: Double)

/**
 * Ruch mapy po dotknięciu konturu [outline] przy wolnej części mapy [free] i widoku [from] widzianym jak [sight].
 * Powiększenie zostaje. Kontur cały widoczny w [sight], którego dół zasłoniłaby karta, przesuwa się w górę tak, że
 * dolny brzeg staje 16 dp nad kartą, a górny brzeg zostaje co najmniej 16 dp pod górą wolnej części; tylko w pionie,
 * w 300 ms × [animationScale]. Każdy inny kontur zostawia widok, a karta rusza się w 300 ms × [animationScale].
 *
 * Wymagania: US2.8, US3.2, NFR-56
 */
internal fun tapTransition(
    from: MapCamera,
    outline: CampusBounds,
    free: FreeMap,
    sight: TapSight,
    animationScale: Float,
): CameraTransition {
    val view = from.shiftedNorth(sight.belowDp)
    val lift = view.lift(outline = outline, free = free, sight = sight)
    val millis = (TAP_MILLIS * animationScale).roundToLong()
    val motion = if (lift == 0.0 || millis == 0L) CameraMotion.NONE else CameraMotion.EASE
    return CameraTransition(camera = view.shiftedNorth(-lift), motion = motion, durationMillis = millis)
}

private fun MapCamera.lift(outline: CampusBounds, free: FreeMap, sight: TapSight): Double {
    val perDegree = dpPerDegree(zoom)
    val perRadian = dpPerRadian(zoom)
    val middle = mercatorY(center.latitude)
    val left = free.widthDp / 2 + (outline.west - center.longitude) * perDegree
    val right = free.widthDp / 2 + (outline.east - center.longitude) * perDegree
    val top = free.heightDp / 2 + (middle - mercatorY(outline.north)) * perRadian
    val bottom = free.heightDp / 2 + (middle - mercatorY(outline.south)) * perRadian
    val isSeenAcross = left >= -VISIBLE_TOLERANCE_DP && right <= free.widthDp + VISIBLE_TOLERANCE_DP
    val isSeenAlong = top >= sight.topDp - VISIBLE_TOLERANCE_DP && bottom <= sight.bottomDp + VISIBLE_TOLERANCE_DP
    val isSeen = isSeenAcross && isSeenAlong
    val covered = bottom - (free.heightDp - TAP_MARGIN_DP)
    val room = (top - TAP_MARGIN_DP).coerceAtLeast(minimumValue = 0.0)
    return if (isSeen && covered > VISIBLE_TOLERANCE_DP) minOf(a = covered, b = room) else 0.0
}

private fun dpPerRadian(zoom: Double): Double = dpPerDegree(zoom) * HALF_TURN_DEGREES / PI
