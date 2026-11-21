/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import io.github.dostepnykampus.feature.map.camera.CameraView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.ShownCamera
import io.github.dostepnykampus.feature.map.camera.rememberClosedView

/**
 * Widok kamery widziany pod arkuszem i widok z chwili zamknięcia arkusza, gdy mapę zasłania arkusz V2
 * ([isCovered]) albo inny arkusz wysoki na [sheet] pikseli; po zamknięciu kamera ma dopełnienie [next].
 *
 * Wymagania: NFR-56
 */
@Composable
internal fun ShownCamera.rememberSeen(
    isCovered: Boolean,
    next: MapInset,
    sheet: Int,
    view: CameraView,
): Pair<MapCamera?, MapCamera?> {
    val isHidden = isCovered || sheet > 0
    val isHiddenBefore = rememberPrevious(isHidden)
    val pxPerDp = LocalDensity.current.density
    val seen =
        rememberView(isCovered = isHidden, wasCovered = isHiddenBefore, next = next, pxPerDp = pxPerDp, view = view)
    return seen to rememberClosedView(isCovered = isHidden, wasCovered = isHiddenBefore, seen = seen)
}

/** Wartość [value] z poprzedniej udanej kompozycji; w pierwszej kompozycji sama [value]. */
@Composable
internal fun <T> rememberPrevious(value: T): T {
    val previous = remember { PreviousValue(value) }
    val before = previous.last
    SideEffect { previous.keep(value) }
    return before
}

private class PreviousValue<T>(initial: T) {
    var last: T = initial
        private set

    fun keep(current: T) {
        last = current
    }
}
