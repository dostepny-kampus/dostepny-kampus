/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.core.model.GeoPoint

private val CameraSaver =
    listSaver<MapCamera?, Double>(
        save = { camera -> camera?.run { listOf(center.longitude, center.latitude, zoom) }.orEmpty() },
        restore = { saved ->
            saved.takeIf { it.isNotEmpty() }?.run { MapCamera(GeoPoint(longitude = get(0), latitude = get(1)), get(2)) }
        },
    )

/**
 * Widok mapy V1 do pokazania: [camera] albo `null`, gdy mapa zaczyna od widoku startowego lub wejścia; [keep]
 * zapisuje widok po ruchu kamery.
 */
internal data class CameraView(val camera: MapCamera?, val keep: (MapCamera) -> Unit)

/**
 * Zapisywany widok mapy. Nowe wejście [entranceId] zaczyna od nowa (US3.3). Polecenie „Pokaż budynek na mapie”
 * dla [show] stawia kamerę na [showPoint], dopóki osoba nie ruszy mapy ani widok nie zostanie zapisany; potem
 * kamera idzie za osobą, także po obrocie ekranu (US2.9, NFR-53, NFR-22). Bez polecenia stan polecenia się
 * zeruje, więc następne, także dla tego samego budynku, znów przesuwa kamerę.
 */
@Composable
internal fun rememberCameraView(entranceId: String?, show: String?, showPoint: GeoPoint?): CameraView {
    var camera by rememberSaveable(entranceId, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    var applied by rememberSaveable { mutableStateOf<String?>(null) }
    SideEffect { if (show == null) applied = null }
    val target = remember(showPoint) { showPoint?.let { point -> MapCamera(center = point, zoom = START_ZOOM) } }
    return CameraView(
        camera = target.takeIf { show != applied } ?: camera,
        keep = { next ->
            camera = next
            applied = show
        },
    )
}
