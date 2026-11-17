/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.camera.CameraView
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.shiftedNorth

/**
 * Ostatni widok podany mapie i zasłonięty wtedy dół mapy: początek ruchu do następnego wybranego budynku i obraz
 * mapy po zamknięciu arkusza V2. Nie jest stanem kompozycji.
 *
 * Wymagania: US2.8, NFR-56
 */
internal class ShownCamera {
    private var camera: MapCamera? = null
    private var covered = 0.dp

    /** Ostatni widok albo [fallback], gdy mapa jeszcze nic nie pokazała. */
    fun startOr(fallback: MapCamera?): MapCamera? = camera ?: fallback

    /** Zapisuje widok [shown]; dół mapy jest zasłonięty na [covered] tylko wtedy, gdy widok stoi nad arkuszem. */
    fun keep(shown: MapCamera?, covered: Dp, isAbove: Boolean) {
        camera = shown
        this.covered = if (isAbove) covered else 0.dp
    }

    /**
     * Ten sam obraz mapy po zniknięciu arkusza ([wasCovered], ale nie [isCovered]) dla mapy pod paskiem wysokim na
     * [bar]: ostatni widok ze środkiem niżej o połowę paska i zasłoniętego dołu. Ten widok raz trafia do [view],
     * potem wynik to `null`; `null` także poza zamknięciem arkusza i wtedy, gdy zaznaczenie samo wyznacza widok
     * ([CameraView.hasTarget]).
     */
    @Composable
    fun rememberLeft(isCovered: Boolean, wasCovered: Boolean, bar: Dp, view: CameraView): MapCamera? {
        val isClosed = wasCovered && !isCovered && !view.hasTarget
        val left = remember(isCovered) { camera?.shiftedNorth(-(bar + covered).value / 2.0).takeIf { isClosed } }
        val keep by rememberUpdatedState(view.keep)
        var isKept by remember(isCovered) { mutableStateOf(false) }
        LaunchedEffect(left) {
            left?.let(keep)
            isKept = true
        }
        return left.takeUnless { isKept }
    }
}
