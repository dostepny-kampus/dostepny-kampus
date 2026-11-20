/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusMapData
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
 * zapisuje widok po ruchu kamery. [shown] to symbol budynku, gdy [camera] stoi na jego punkcie podpisu po
 * poleceniu „Pokaż budynek na mapie”, a nie na widoku zapisanym. [hasTarget] jest prawdą, gdy zaznaczenie samo
 * wyznacza widok: polecenie „Pokaż budynek na mapie” czeka albo wejście nie ma jeszcze zapisanego widoku.
 */
internal data class CameraView(
    val camera: MapCamera?,
    val keep: (MapCamera) -> Unit,
    val shown: String? = null,
    val hasTarget: Boolean = false,
)

/**
 * Zapisywany widok mapy dla [focus]. Nowe wejście zaczyna od nowa, a polecenie budynku kieruje kamerę do
 * punktu podpisu z [layers]. Po wykonaniu polecenia kamera idzie za osobą, także po obrocie ekranu.
 * Tożsamość [MapFocus.Building.command] pozwala ponownie wybrać ten sam budynek. [roomCamera] zapisuje widok
 * od razu po przesunięciu do sali, dzięki czemu zostaje po zamknięciu karty.
 *
 * Wymagania: US3.3, US2.9, NFR-53, NFR-22
 */
@Composable
internal fun rememberCameraView(focus: MapFocus?, layers: CampusMapData?, roomCamera: MapCamera? = null): CameraView {
    val entranceId = (focus as? MapFocus.Entrance)?.id
    val show = (focus as? MapFocus.Building)?.takeIf { it.command != null }
    var camera by rememberSaveable(entranceId, stateSaver = CameraSaver) { mutableStateOf<MapCamera?>(null) }
    var applied by rememberSaveable { mutableStateOf<Long?>(null) }
    val target =
        remember(layers, show, roomCamera) {
            if (show?.room == null) {
                layers?.pointOf(show?.symbol)?.let { MapCamera(center = it, zoom = START_ZOOM) }
            } else {
                roomCamera
            }
        }
    val isPending = target != null && show?.command != applied
    if (isPending && show?.room != null) {
        SideEffect { camera = target }
    }
    return CameraView(
        camera = target.takeIf { isPending } ?: camera,
        keep = { next ->
            camera = next
            show?.command?.let { applied = it }
        },
        shown = show?.symbol.takeIf { isPending && show?.room == null },
        hasTarget = isPending || (entranceId != null && camera == null),
    )
}

/** Punkt podpisu budynku o symbolu [symbol] z gotowych danych mapy; `null`, gdy dane go nie znają. */
internal fun CampusMapData.pointOf(symbol: String?): GeoPoint? = symbol?.let(labelPointsBySymbol::get)
