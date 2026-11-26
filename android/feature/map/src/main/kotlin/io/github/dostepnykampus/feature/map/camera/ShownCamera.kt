/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity

/**
 * Ostatni widok podany mapie i zasłonięty wtedy dół mapy: początek ruchu do następnego wybranego budynku, a dla
 * [rememberSeen] obraz mapy w chwili zamknięcia karty V2 albo innego arkusza nad mapą. Nie jest stanem kompozycji.
 *
 * Wymagania: US2.8, NFR-56
 */
internal class ShownCamera {
    /** Ostatni widok podany mapie; `null`, gdy mapa jeszcze nic nie pokazała. */
    var camera: MapCamera? = null
        private set

    /** Dopełnienie kamery przy [camera] w pikselach. */
    var inset: MapInset = MapInset()
        private set

    private var origin: MapCamera? = null

    /** Ostatni widok albo [fallback], gdy mapa jeszcze nic nie pokazała. */
    fun startOr(fallback: MapCamera?): MapCamera? = camera ?: fallback

    /** Widok, z którego mapa rusza do [target]: ostatni podany widok, a gdy to już [target], widok sprzed niego. */
    fun originOf(target: MapCamera): MapCamera? = if (target == camera) origin else camera

    /**
     * Ruch bez arkusza do [target] ([uncoveredMove]) z widoku startowego ([originOf]); przy [isJump] skok bez ruchu
     * na ekranie, jak do obrazu z chwili zamknięcia [closed]. `null` bez celu.
     */
    fun moveTo(target: MapCamera?, closed: MapCamera?, free: FreeMap, isJump: Boolean = false): CameraTransition? =
        target?.uncoveredMove(closed = target.takeIf { isJump } ?: closed, from = originOf(target), free = free)

    /** Zapisuje widok [shown] podany mapie z dopełnieniem kamery [inset]. */
    fun keep(shown: MapCamera?, inset: MapInset) {
        if (shown != camera) origin = camera
        camera = shown
        this.inset = inset
    }
}

/**
 * Ruch do widoku bez arkusza: do obrazu mapy z chwili zamknięcia arkusza [closed] skok, który zmienia cel
 * i dopełnienie w jednej klatce bez ruchu na ekranie. Do innego widoku ruch według reguły ruchu do budynku: cel
 * w zasięgu jednego ekranu wolnej mapy [free] od widoku [from] to przesunięcie w 300 ms, bez oddalania mapy po
 * drodze; dalszy cel albo nieznany [from] to przelot przycisków.
 *
 * Wymagania: US2.8, US18.1, US18.2, NFR-56
 */
internal fun MapCamera.uncoveredMove(closed: MapCamera?, from: MapCamera?, free: FreeMap): CameraTransition {
    val isNear = from != null && distanceDp(from = from, to = this) <= free.widthDp
    return when {
        closed?.isSameView(this) == true -> CameraTransition(this, CameraMotion.NONE, 0L)
        isNear -> CameraTransition(this, CameraMotion.EASE, CONTROL_MILLIS)
        else -> byControl()
    }
}

/**
 * Widok kamery widziany pod arkuszem i widok z chwili zamknięcia arkusza, gdy mapę zasłania arkusz V2
 * ([isCovered]) albo inny arkusz wysoki na [sheet] pikseli; po zamknięciu kamera ma dopełnienie [next]. Widok
 * z chwili zamknięcia ([closingView]) raz trafia do [CameraView.keep] i zostaje do następnej zmiany zasłonięcia.
 *
 * Wymagania: US2.8, US18.1, NFR-56
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
    val left =
        remember(isHidden) {
            closingView(
                wasCovered = isHiddenBefore,
                isCovered = isHidden,
                hasTarget = view.hasTarget,
                to = next,
                pxPerDp = pxPerDp,
            )
        }
    val keep by rememberUpdatedState(view.keep)
    var isKept by remember(isHidden) { mutableStateOf(false) }
    DisposableEffect(left) {
        left?.let(keep)
        isKept = true
        onDispose {}
    }
    val seen = left.takeUnless { isKept } ?: view.camera
    val closed = remember(isHidden) { seen.takeIf { isHiddenBefore && !isHidden } }
    return seen to closed
}

/**
 * Obraz mapy z chwili zamknięcia arkusza ([wasCovered], ale nie [isCovered]): ostatni widok [ShownCamera.camera]
 * pod nowym dopełnieniem [to] przy [pxPerDp] pikselach na dp ([keptUnder]). `null` bez zamknięcia, bez widoku albo
 * gdy zaznaczenie samo wyznacza widok ([hasTarget]).
 *
 * Wymagania: US2.8, US18.1, NFR-56
 */
internal fun ShownCamera.closingView(
    wasCovered: Boolean,
    isCovered: Boolean,
    hasTarget: Boolean,
    to: MapInset,
    pxPerDp: Float,
): MapCamera? =
    camera
        ?.takeIf { wasCovered && !isCovered && !hasTarget }
        ?.keptUnder(from = inset, to = to, pxPerDp = pxPerDp)

/**
 * Ten sam obraz mapy po zmianie dopełnienia kamery z [from] na [to] (piksele, [pxPerDp] pikseli na dp): środek
 * widoku idzie na południe o połowę przyrostu górnego dopełnienia i o połowę ubytku dolnego.
 *
 * Wymagania: US2.8, US18.1, NFR-56
 */
internal fun MapCamera.keptUnder(from: MapInset, to: MapInset, pxPerDp: Float): MapCamera =
    shiftedNorth(-(to.top - from.top - (to.bottom - from.bottom)) / 2.0 / pxPerDp)

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
