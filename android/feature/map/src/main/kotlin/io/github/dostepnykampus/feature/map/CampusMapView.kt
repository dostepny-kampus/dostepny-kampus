/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapSelection
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF
private const val NIGHT_LAND_LUMINANCE = 0.5f

/** Widok mapy za interfejsem: ekran i jego testy nie zależą od natywnej biblioteki mapy. */
fun interface CampusMapView {
    /**
     * Mapa [data] z zaznaczeniem [selection] w widoku [camera]. Po geście widok zgłasza nowy widok przez
     * [onCameraIdle], a po dotknięciu konturu albo miejsca obok niego symbole trafionych budynków przez
     * [onBuildingsTouch], a po dotknięciu pustej mapy pustą listę. Dotknięcie znaku bariery zgłasza identyfikator
     * jej zgłoszenia przez [onBarrierTouch] i nie zgłasza konturu pod znakiem.
     *
     * Wymagania: US1.5
     */
    @Composable
    fun Show(
        data: CampusMapData,
        selection: MapSelection,
        camera: MapCamera,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        onBarrierTouch: (String) -> Unit,
        modifier: Modifier,
    )
}

/**
 * Granice widoku mapy o rozmiarze [size] w pikselach w obszarze [bounds]; `null` bez obszaru.
 *
 * Wymagania: NFR-39, NFR-53
 */
internal fun Density.mapLimits(bounds: CampusBounds?, size: IntSize): MapLimits? =
    bounds?.let { area ->
        val width = size.width.toDp().value
        val height = size.height.toDp().value
        MapLimits(area, widthDp = width.toDouble(), heightDp = height.toDouble())
    }

internal fun MapColors.styleColors(): MapStyleColors =
    MapStyleColors(
        land = lad.hex(),
        building = pwr.hex(),
        buildingOutline = pwrKontur.hex(),
        selectedOutline = pwrWybrany.hex(),
        isNight = lad.luminance() < NIGHT_LAND_LUMINANCE,
        ink = tusz.hex(),
        dimmed = budynek.hex(),
        dimmedOutline = budynekKontur.hex(),
    )

internal fun Color.hex(): String = "#%06X".format(Locale.ROOT, toArgb() and RGB_MASK)

internal fun MapCamera.toPosition(): CameraPosition =
    CameraPosition
        .Builder()
        .target(LatLng(latitude = center.latitude, longitude = center.longitude))
        .zoom(zoom)
        .padding(doubleArrayOf(0.0, 0.0, 0.0, bottomInset))
        .build()

internal fun CameraPosition.toCamera(): MapCamera =
    MapCamera(
        center = GeoPoint(longitude = target?.longitude ?: 0.0, latitude = target?.latitude ?: 0.0),
        zoom = zoom,
        bottomInset = padding?.lastOrNull() ?: 0.0,
    )
