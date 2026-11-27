/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.pow

private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0

/** Czas ruchu po przycisku mapy i ruchu bez arkusza w zasięgu ekranu w ms. */
internal const val CONTROL_MILLIS = 300L

private const val APPROACH_MAX_ZOOM = 19.0

/**
 * Rodzaj ruchu mapy do wybranego budynku.
 *
 * Wymagania: US2.8, NFR-56
 */
enum class CameraMotion {
    /**
     * Bez ruchu kamery: od razu stan końcowy. Karta i pasek ruszają się w czasie ruchu: 0 przy systemowym
     * „Usuń animacje”, 300 ms po wyborze z listy budynków ([placed]).
     */
    NONE,

    /** Przesunięcie, z ewentualną zmianą powiększenia, po krzywej MapLibre. */
    EASE,

    /** Przelot: oddalenie, przesunięcie i przybliżenie jednym ruchem (van Wijk–Nuij). */
    FLIGHT,
}

/**
 * Ruch mapy: widok docelowy [camera] dla wolnej części mapy (dopełnienie arkusza ustawia wołający), rodzaj ruchu
 * [motion], jego czas [durationMillis] i numer czynności [action]. Widok mapy wykonuje ruch raz na numer czynności;
 * ruch z tym samym numerem nie rusza kamery ponownie.
 *
 * Wymagania: US2.8, NFR-56
 */
data class CameraTransition(
    val camera: MapCamera,
    val motion: CameraMotion,
    val durationMillis: Long,
    val action: Long = 0L,
)

/**
 * Ruch [move] z numerem czynności: inny ruch niż w poprzedniej kompozycji dostaje nowy numer, ten sam ruch
 * zachowuje numer. `null` bez ruchu.
 *
 * Wymagania: US2.8, NFR-56
 */
@Composable
internal fun rememberAction(move: CameraTransition?): CameraTransition? {
    val counter = remember { ActionCounter() }
    val action = remember(move) { counter.next() }
    return move?.copy(action = action)
}

private class ActionCounter {
    private var last = 0L

    fun next(): Long = ++last
}

/** Ten sam ruch z widokiem w granicy terenu [limits] dla wolnej części mapy [free]. Wymagania: US1.7, US2.8 */
internal fun CameraTransition.within(limits: MapLimits?, free: FreeMap): CameraTransition =
    limits?.let { copy(camera = it.clamp(camera, viewWidthDp = free.widthDp, viewHeightDp = free.heightDp)) } ?: this

/** Ruch do widoku po przycisku mapy albo odtworzeniu widoku: przelot w 300 ms. Wymagania: NFR-53 */
internal fun MapCamera.byControl(): CameraTransition =
    CameraTransition(camera = this, motion = CameraMotion.FLIGHT, durationMillis = CONTROL_MILLIS)

/**
 * Ruch mapy z widoku [from] do budynku o prostokącie [outline] w wolnej części mapy [free]: rodzaj i czas według
 * położenia budynku względem widoku, czas mnożony przez skalę animacji [animationScale] (0 daje
 * [CameraMotion.NONE]). Kontur kończy na połowie wolnej mapy, najwyżej przy powiększeniu 19; większe powiększenie
 * [from], a przy [keepsZoom] każde, zostaje. Kontur, który się nie mieści, oddala mapę do zmieszczenia.
 *
 * Wymagania: US2.8, US3.2, NFR-56
 */
internal fun cameraTransition(
    from: MapCamera,
    outline: CampusBounds,
    free: FreeMap,
    animationScale: Float,
    keepsZoom: Boolean = false,
): CameraTransition {
    val fits = fittingZoom(outline, free)
    val approach = (fits - 1).coerceAtMost(maximumValue = APPROACH_MAX_ZOOM)
    val zoom = (if (keepsZoom) from.zoom else maxOf(a = from.zoom, b = approach)).coerceAtMost(fits)
    val camera = centered(outline, zoom, free)
    val isShown = isVisible(outline, from, free)
    return travel(from = from, to = camera, free = free, animationScale = animationScale, isVisible = isShown)
}

/**
 * Widok, w którym prostokąt [outline] mieści się w wolnej części mapy [free] z marginesem, poza pasem przycisków
 * mapy, w powiększeniu najwyżej [maxZoom].
 *
 * Wymagania: US1.7, BR-G5
 */
internal fun fitted(outline: CampusBounds, free: FreeMap, maxZoom: Double): MapCamera =
    centered(outline = outline, zoom = fittingZoom(outline, free).coerceAtMost(maximumValue = maxZoom), free = free)

/** Powiększenie, przy którym [outline] mieści się w wolnej części [free] z marginesem, poza pasem przycisków. */
internal fun fittingZoom(outline: CampusBounds, free: FreeMap): Double {
    val width = (free.widthDp - free.endDp - 2 * free.marginDp).coerceAtLeast(minimumValue = 1.0)
    val height = (free.heightDp - 2 * free.marginDp).coerceAtLeast(minimumValue = 1.0)
    return minOf(
        a = fitZoom(viewDp = width, worldShare = (outline.east - outline.west) / FULL_TURN_DEGREES),
        b = fitZoom(viewDp = height, worldShare = (mercatorY(outline.north) - mercatorY(outline.south)) / (2 * PI)),
    )
}

/** Widok w powiększeniu [zoom] z [outline] na środku wolnej części [free], obok pasa przycisków. */
internal fun centered(outline: CampusBounds, zoom: Double, free: FreeMap): MapCamera {
    val behindControls = free.endDp / 2 / dpPerDegree(zoom)
    val center =
        GeoPoint(
            longitude = (outline.west + outline.east) / 2 + behindControls,
            latitude = (outline.south + outline.north) / 2,
        )
    return MapCamera(center = center, zoom = zoom)
}

/**
 * Ten widok przesunięty w poziomie tak, że prostokąt [outline] stoi na środku części mapy szerokiej na [widthDp]
 * obok pasa szerokiego na [startDp] przy lewym brzegu; prostokąt szerszy niż ta część zaczyna się tuż za pasem.
 * Powiększenie i szerokość geograficzna środka zostają.
 *
 * Wymagania: US16.1, US1.3, US1.4, NFR-17
 */
internal fun MapCamera.besideStart(outline: CampusBounds, widthDp: Double, startDp: Double): MapCamera {
    val perDegree = dpPerDegree(zoom)
    val spare = widthDp - startDp - (outline.east - outline.west) * perDegree
    val westDp = startDp + (spare / 2).coerceAtLeast(minimumValue = 0.0)
    return copy(center = center.copy(longitude = outline.west + (widthDp / 2 - westDp) / perDegree))
}

private fun isVisible(outline: CampusBounds, view: MapCamera, free: FreeMap): Boolean {
    val perDegree = dpPerDegree(view.zoom)
    val perRadian = WORLD_DP_AT_ZOOM_0 * 2.0.pow(view.zoom) / (2 * PI)
    val middle = mercatorY(view.center.latitude)
    val left = (outline.west - view.center.longitude) * perDegree
    val right = (outline.east - view.center.longitude) * perDegree
    val top = (middle - mercatorY(outline.north)) * perRadian
    val bottom = (middle - mercatorY(outline.south)) * perRadian
    val halfWidth = free.widthDp / 2 - free.marginDp
    val halfHeight = free.heightDp / 2 - free.marginDp
    return left >= -halfWidth && right <= halfWidth - free.endDp && top >= -halfHeight && bottom <= halfHeight
}

/** Odległość w dp między środkami widoków [from] i [to] przy powiększeniu [from]. */
internal fun distanceDp(from: MapCamera, to: MapCamera): Double {
    val east = (to.center.longitude - from.center.longitude) * dpPerDegree(from.zoom)
    val perRadian = WORLD_DP_AT_ZOOM_0 * 2.0.pow(from.zoom) / (2 * PI)
    val south = (mercatorY(from.center.latitude) - mercatorY(to.center.latitude)) * perRadian
    return hypot(x = east, y = south)
}

/** Dp na stopień długości geograficznej przy powiększeniu [zoom]. */
internal fun dpPerDegree(zoom: Double): Double = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom) / FULL_TURN_DEGREES

/** Stała nazwa sekcji śladu ruchu kamery tego rodzaju, bez danych osobowych. Wymagania: NFR-57 */
internal val CameraMotion.traceSection: String
    get() =
        when (this) {
            CameraMotion.NONE -> "Kamera: bez ruchu"
            CameraMotion.EASE -> "Kamera: przesunięcie"
            CameraMotion.FLIGHT -> "Kamera: przelot"
        }
