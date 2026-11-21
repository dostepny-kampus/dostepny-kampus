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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ostatni widok podany mapie i zasłonięty wtedy dół mapy: początek ruchu do następnego wybranego budynku i obraz
 * mapy po zamknięciu karty V2 albo innego arkusza nad mapą. Nie jest stanem kompozycji.
 *
 * Wymagania: US2.8, NFR-56
 */
internal class ShownCamera {
    private var camera: MapCamera? = null
    private var covered = 0.dp

    /** Widok, w którym mapa ostatnio stanęła; `null`, dopóki nie stanęła ani razu. */
    var rest: MapCamera? = null
        private set

    /** Ostatni widok albo [fallback], gdy mapa jeszcze nic nie pokazała. */
    fun startOr(fallback: MapCamera?): MapCamera? = camera ?: fallback

    /** Zapis [keep] widoku, w którym mapa stanęła, który zapamiętuje go też jako [rest]. */
    fun settling(keep: (MapCamera) -> Unit): (MapCamera) -> Unit =
        { view ->
            rest = view
            keep(view)
        }

    /** Zapisuje widok [shown] z dołem mapy zasłoniętym na [covered]; 0, gdy widok nie stoi nad arkuszem. */
    fun keep(shown: MapCamera?, covered: Dp) {
        camera = shown
        this.covered = covered
    }

    /**
     * Widok mapy do pokazania bez arkusza: zapisany w [view], a w chwili zniknięcia arkusza ([wasCovered], ale nie
     * [isCovered]) ten sam obraz mapy pod paskiem wysokim na [bar] (0, gdy pasek był już nad arkuszem), czyli ostatni
     * widok ze środkiem niżej o połowę paska i zasłoniętego dołu. Ten widok raz trafia do [view]. Gdy zaznaczenie
     * samo wyznacza widok ([CameraView.hasTarget]), zostaje widok z [view].
     */
    @Composable
    fun rememberView(isCovered: Boolean, wasCovered: Boolean, bar: Dp, view: CameraView): MapCamera? {
        val isClosed = wasCovered && !isCovered && !view.hasTarget
        val left = remember(isCovered) { camera?.shiftedNorth(-(bar + covered).value / 2.0).takeIf { isClosed } }
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
