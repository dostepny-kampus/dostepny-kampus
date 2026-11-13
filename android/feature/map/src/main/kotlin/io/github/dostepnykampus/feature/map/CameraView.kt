/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.navigation.MapFocus

internal val CameraSaver =
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
 * Zapisywany widok mapy dla zaznaczenia [focus]. Nowe wejście zaczyna od nowa. Budynek z poleceniem
 * „Pokaż budynek na mapie” stawia kamerę na swoim punkcie podpisu z danych mapy [layers], dopóki osoba nie ruszy
 * mapy ani widok nie zostanie zapisany; budynek bez polecenia kamery nie rusza. Potem kamera
 * idzie za osobą, także po obrocie ekranu. Wykonane polecenie jest zapamiętane razem
 * z zapisanym widokiem przez jego tożsamość ([MapFocus.Building.command]), więc nowe polecenie, także dla tego
 * samego budynku, znów przesuwa kamerę, niezależnie od tego, czy mapa była między nimi składana.
 *
 * Wymagania: US3.3, US2.9, NFR-53, NFR-22
 */
@Composable
internal fun rememberCameraView(focus: MapFocus?, layers: CampusMapData?): CameraView {
    val entranceId = (focus as? MapFocus.Entrance)?.id
    val show = (focus as? MapFocus.Building)?.takeIf { it.command != null }
    var camera by rememberSaveable(entranceId, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    var applied by rememberSaveable { mutableStateOf<Long?>(null) }
    val target =
        remember(layers, show) { layers?.pointOf(show?.symbol)?.let { MapCamera(center = it, zoom = START_ZOOM) } }
    return CameraView(
        camera = target.takeIf { show?.command != applied } ?: camera,
        keep = { next ->
            camera = next
            show?.command?.let { applied = it }
        },
    )
}
