/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection

/**
 * Ruch kamery wykonany przez udawaną mapę: rodzaj [motion], czas [millis] oraz obraz mapy przed ruchem [before]
 * i po nim [after]. Obraz to punkt w środku całego widoku mapy z powiększeniem, więc samo dopełnienie kamery,
 * które zostawia obraz, nie jest ruchem obrazu.
 */
internal data class CameraCall(
    val motion: CameraMotion,
    val millis: Long,
    val before: MapCamera,
    val after: MapCamera,
) {
    /** Obraz mapy się zmienił. */
    val isVisible: Boolean get() = !before.isSameView(after)
}

/**
 * Widok zastępczy mapy z prawdziwym [CameraController]: ruch, granice i dopełnienie idą do sterownika tak jak
 * w widoku MapLibre, a udawana mapa zapisuje każdy ruch w [moves] i granice w [limits]. Ruch kończy się w celu
 * w następnym obiegu wątku głównego. [touch] dotyka konturów budynków o podanych symbolach.
 */
internal class RecordedCamera {
    val moves = mutableListOf<CameraCall>()
    val limits = mutableListOf<MapLimits?>()
    var touch: (List<String>) -> Unit = {}
        private set

    val view =
        CampusMapView { _: CampusMapData, selection, move, inset, _, onBuildingsTouch, _, modifier: Modifier ->
            touch = onBuildingsTouch
            Follow(selection = selection, move = move, inset = inset, modifier = modifier)
        }

    /** Zapomina zapisane ruchy i granice, np. po ustaleniu się widoku startowego. */
    fun clear() {
        moves.clear()
        limits.clear()
    }

    @Composable
    private fun Follow(
        selection: MapSelection,
        move: CameraTransition,
        inset: MapInset,
        modifier: Modifier = Modifier,
    ) {
        val density = LocalDensity.current
        var size by remember { mutableStateOf(IntSize.Zero) }
        val bounds =
            remember(selection.bounds, size, density, inset.top) {
                density.mapLimits(selection.bounds, size.belowTop(inset.top)).takeIf { size != IntSize.Zero }
            }
        val controller = remember { CameraController(Port(move.camera, inset, density.density)) }
        val currentInset by rememberUpdatedState(inset)
        DisposableEffect(controller, move) {
            controller.move(move, currentInset)
            onDispose {}
        }
        DisposableEffect(controller, bounds) {
            controller.limit(bounds)
            onDispose {}
        }
        DisposableEffect(controller, inset) {
            controller.pad(inset)
            onDispose {}
        }
        Box(modifier.onSizeChanged { size = it })
    }

    private inner class Port(
        override var camera: MapCamera,
        override var inset: MapInset,
        private val pxPerDp: Float,
    ) : CameraPort {
        private val main = Handler(Looper.getMainLooper())

        override fun lift() = Unit

        override fun move(camera: MapCamera, inset: MapInset, motion: CameraMotion, millis: Long, end: () -> Unit) {
            val before = picture()
            this.camera = camera
            this.inset = inset
            moves += CameraCall(motion = motion, millis = millis, before = before, after = picture())
            main.post(end)
        }

        override fun rest(limits: MapLimits?) {
            this@RecordedCamera.limits += limits
        }

        override fun post(block: () -> Unit) {
            main.post(block)
        }

        private fun picture(): MapCamera = camera.shiftedNorth(-(inset.bottom - inset.top) / 2.0 / pxPerDp)
    }
}
