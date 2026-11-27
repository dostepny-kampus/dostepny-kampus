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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.github.dostepnykampus.feature.map.campus.CampusMapData

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
        CampusMapView { _: CampusMapData, _, move, inset, frame, _, onBuildingsTouch, _, modifier: Modifier ->
            touch = onBuildingsTouch
            Follow(move = move, inset = inset, frame = frame, modifier = modifier)
        }

    /** Zapomina zapisane ruchy i granice, np. po ustaleniu się widoku startowego. */
    fun clear() {
        moves.clear()
        limits.clear()
    }

    @Composable
    private fun Follow(move: CameraTransition, inset: MapInset, frame: MapFrame, modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        val controller = remember { CameraController(Port(move.camera, inset, density.density)) }
        val currentMove by rememberUpdatedState(move)
        val currentInset by rememberUpdatedState(inset)
        DisposableEffect(controller, move.action) {
            controller.move(currentMove, currentInset)
            onDispose {}
        }
        DisposableEffect(controller, frame.restLimits()) {
            controller.limit(frame.restLimits())
            onDispose {}
        }
        DisposableEffect(controller, inset) {
            controller.pad(inset)
            onDispose {}
        }
        Box(modifier)
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
