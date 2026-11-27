/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

private const val FLIGHT_CURVE = 1.42
private const val VISIBLE_MILLIS = 300.0
private const val NEAR_MILLIS = 400.0
private const val FLIGHT_MIN_MILLIS = 400.0
private const val FLIGHT_MAX_MILLIS = 1500.0
private const val FLIGHT_MILLIS_PER_PATH = 250.0
private const val SAME_PLACE_DP = 1e-6

/**
 * Ruch z widoku [from] do widoku terenu [to] w wolnej części mapy [free]: cel w zasięgu jednej szerokości wolnej mapy
 * to przesunięcie w 400 ms, dalszy albo nieznany [from] to przelot z czasem z drogi, najwyżej 1,5 s. Czas jest
 * mnożony przez skalę animacji [animationScale]; 0 daje [CameraMotion.NONE].
 *
 * Wymagania: US1.7, NFR-56
 */
internal fun areaMove(from: MapCamera?, to: MapCamera, free: FreeMap, animationScale: Float): CameraTransition =
    travel(from = from, to = to, free = free, animationScale = animationScale, isVisible = false)

/**
 * Ruch z widoku [from] do [to] w wolnej części mapy [free]: [isVisible] (cel już widoczny) to przesunięcie 300 ms,
 * cel w zasięgu jednej szerokości wolnej mapy to przesunięcie 400 ms, dalszy albo nieznany [from] to przelot z czasem
 * z drogi, najwyżej 1,5 s. Czas jest mnożony przez skalę animacji [animationScale]; 0 daje [CameraMotion.NONE].
 *
 * Wymagania: US2.8, NFR-56
 */
internal fun travel(
    from: MapCamera?,
    to: MapCamera,
    free: FreeMap,
    animationScale: Float,
    isVisible: Boolean,
): CameraTransition {
    val start = from ?: to
    val distance = distanceDp(from = start, to = to)
    val (motion, millis) =
        when {
            animationScale <= 0f -> CameraMotion.NONE to 0.0
            isVisible -> CameraMotion.EASE to VISIBLE_MILLIS
            from != null && distance <= free.widthDp -> CameraMotion.EASE to NEAR_MILLIS
            else -> CameraMotion.FLIGHT to flightMillis(from = start, to = to, free = free, distance = distance)
        }
    return CameraTransition(camera = to, motion = motion, durationMillis = (millis * animationScale).roundToLong())
}

private fun flightMillis(from: MapCamera, to: MapCamera, free: FreeMap, distance: Double): Double {
    val rho2 = FLIGHT_CURVE * FLIGHT_CURVE
    val w0 = maxOf(a = free.widthDp, b = free.heightDp)
    val w1 = w0 / 2.0.pow(to.zoom - from.zoom)
    val path =
        if (distance < SAME_PLACE_DP) {
            abs(ln(w1 / w0)) / FLIGHT_CURVE
        } else {
            val start = (w1 * w1 - w0 * w0 + rho2 * rho2 * distance * distance) / (2 * w0 * rho2 * distance)
            val end = (w1 * w1 - w0 * w0 - rho2 * rho2 * distance * distance) / (2 * w1 * rho2 * distance)
            (ln(sqrt(end * end + 1) - end) - ln(sqrt(start * start + 1) - start)) / FLIGHT_CURVE
        }
    val millis = path * FLIGHT_MILLIS_PER_PATH
    return millis.coerceIn(minimumValue = FLIGHT_MIN_MILLIS, maximumValue = FLIGHT_MAX_MILLIS)
}
