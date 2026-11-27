/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.CameraView
import io.github.dostepnykampus.feature.map.camera.ENTRANCE_ZOOM
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.ShownCamera
import io.github.dostepnykampus.feature.map.camera.areaMove

/**
 * Widok bez arkusza: widok zapisany [shown], a bez niego wejście [entrance] albo widok startowy [start], w granicach
 * [limits]; `null` bez żadnego z nich.
 */
internal fun uncoveredView(shown: MapCamera?, entrance: GeoPoint?, start: GeoPoint?, limits: MapLimits?): MapCamera? {
    val own = shown ?: entrance?.let { MapCamera(center = it, zoom = ENTRANCE_ZOOM) }
    return (own ?: start?.let { MapCamera(center = it, zoom = START_ZOOM) })?.let { limits?.clamp(it) ?: it }
}

/**
 * Ruch do widoku wybranego terenu [target] według drogi ([areaMove]) w wolnej części mapy [free] przy skali animacji
 * [scale]; `null` bez celu albo gdy widok [view] to nie lądowanie na terenie, także tuż po zamknięciu arkusza
 * ([closed]).
 */
internal fun ShownCamera.landingMove(
    target: MapCamera?,
    view: CameraView,
    closed: MapCamera?,
    free: FreeMap,
    scale: Float,
): CameraTransition? =
    target?.takeIf { view.isLanding && closed == null }?.let { to ->
        areaMove(from = originOf(to), to = to, free = free, animationScale = scale)
    }
