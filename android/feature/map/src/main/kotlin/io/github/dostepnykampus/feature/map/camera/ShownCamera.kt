/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Ostatni widok podany mapie i zasłonięty wtedy dół mapy: początek ruchu do następnego wybranego budynku i obraz
 * mapy po zamknięciu karty V2 albo innego arkusza nad mapą. Nie jest stanem kompozycji.
 *
 * Wymagania: US2.8, NFR-56
 */
internal class ShownCamera {
    private var camera: MapCamera? = null
    private var origin: MapCamera? = null
    private var inset = MapInset()

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

    /**
     * Widok mapy do pokazania bez arkusza: zapisany w [view], a w chwili zniknięcia arkusza ([wasCovered], ale nie
     * [isCovered]) ten sam obraz mapy pod nowym dopełnieniem [next] przy [pxPerDp] pikselach na dp ([keptUnder]).
     * Ten widok raz trafia do [view]. Gdy zaznaczenie samo wyznacza widok ([CameraView.hasTarget]), zostaje widok
     * z [view].
     */
    @Composable
    fun rememberView(
        isCovered: Boolean,
        wasCovered: Boolean,
        next: MapInset,
        pxPerDp: Float,
        view: CameraView,
    ): MapCamera? {
        val isClosed = wasCovered && !isCovered && !view.hasTarget
        val left =
            remember(isCovered) { camera?.takeIf { isClosed }?.keptUnder(from = inset, to = next, pxPerDp = pxPerDp) }
        val keep by rememberUpdatedState(view.keep)
        var isKept by remember(isCovered) { mutableStateOf(false) }
        DisposableEffect(left) {
            left?.let(keep)
            isKept = true
            onDispose {}
        }
        return left.takeUnless { isKept } ?: view.camera
    }
}

/**
 * Ten sam obraz mapy po zmianie dopełnienia kamery z [from] na [to] (piksele, [pxPerDp] pikseli na dp): środek
 * widoku idzie na południe o połowę przyrostu górnego dopełnienia i o połowę ubytku dolnego.
 *
 * Wymagania: US2.8, US18.1, NFR-56
 */
internal fun MapCamera.keptUnder(from: MapInset, to: MapInset, pxPerDp: Float): MapCamera =
    shiftedNorth(-(to.top - from.top - (to.bottom - from.bottom)) / 2.0 / pxPerDp)

/**
 * Widok [seen] z chwili zniknięcia arkusza ([wasCovered], ale nie [isCovered]); zostaje do następnej zmiany
 * zasłonięcia, a bez zamknięcia jest `null`.
 *
 * Wymagania: NFR-56
 */
@Composable
internal fun rememberClosedView(isCovered: Boolean, wasCovered: Boolean, seen: MapCamera?): MapCamera? =
    remember(isCovered) { seen.takeIf { wasCovered && !isCovered } }

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
