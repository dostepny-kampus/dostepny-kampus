/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
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
     * Ruch nad arkuszem [above], a gdy arkusz właśnie zniknął ([wasCovered], ale nie [isCovered]), ten sam obraz
     * mapy pod paskiem wysokim na [bar]: środek niżej o połowę paska i zasłoniętego dołu, bez ruchu kamery. Ten
     * widok trafia do [view]; gdy [view] już go ma albo arkusza nie było, wynik to `null`.
     */
    @Composable
    fun rememberHeld(
        above: CameraTransition?,
        isCovered: Boolean,
        wasCovered: Boolean,
        bar: Dp,
        view: CameraView,
    ): CameraTransition? {
        val left = remember(isCovered) { camera?.shiftedNorth(-(bar + covered).value / 2.0).takeIf { wasCovered } }
        SideEffect { if (!isCovered) left?.let(view.keep) }
        val still = left?.takeIf { !isCovered && it != view.camera }
        return above ?: still?.let { CameraTransition(camera = it, motion = CameraMotion.NONE, durationMillis = 0) }
    }
}
