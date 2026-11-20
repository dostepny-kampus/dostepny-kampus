/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.camera.CameraView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.ShownCamera
import io.github.dostepnykampus.feature.map.camera.rememberClosedView

/**
 * Widok kamery widziany pod arkuszem i widok z chwili zamknięcia arkusza, gdy mapę zasłania arkusz V2
 * ([isCovered], przedtem [wasCovered] pod paskiem [top]) albo inny arkusz wysoki na [sheet] pikseli.
 *
 * Wymagania: NFR-56
 */
@Composable
internal fun ShownCamera.rememberSeen(
    isCovered: Boolean,
    wasCovered: Boolean,
    top: Dp,
    sheet: Int,
    view: CameraView,
): Pair<MapCamera?, MapCamera?> {
    val isHidden = isCovered || sheet > 0
    val isHiddenBefore = rememberPrevious(isHidden)
    val bar = if (wasCovered) top else 0.dp
    val seen = rememberView(isCovered = isHidden, wasCovered = isHiddenBefore, bar = bar, view = view)
    return seen to rememberClosedView(isCovered = isHidden, wasCovered = isHiddenBefore, seen = seen)
}

/**
 * Zasłonięty dół mapy zapisany z widokiem: nad kartą V2 jej wysokość [card], pod kartą V2 bez ruchu kamery 0,
 * a pod innym arkuszem jego wysokość [sheet] w pikselach.
 *
 * Wymagania: NFR-56
 */
internal fun Density.keptCover(isAboveCard: Boolean, isCovered: Boolean, card: Dp, sheet: Int): Dp =
    when {
        isAboveCard -> card
        isCovered -> 0.dp
        else -> sheet.toDp()
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
