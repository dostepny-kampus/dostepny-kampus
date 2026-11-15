/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.PlanColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF
private const val NIGHT_LAND_LUMINANCE = 0.5f

/** Widok mapy za interfejsem: ekran i jego testy nie zależą od natywnej biblioteki mapy. */
fun interface CampusMapView {
    /**
     * Mapa [data] z zaznaczeniem [selection] po ruchu [move], z krawędziami zasłoniętymi według [inset]: cel
     * ruchu stoi w środku części odkrytej. Każdy ruch wykonuje raz; zmiana [inset] zmienia tylko dopełnienie
     * mapy i nie przerywa trwającego ruchu. Po geście widok zgłasza nowy widok przez
     * [onCameraIdle], a po dotknięciu konturu albo miejsca obok niego symbole trafionych budynków przez
     * [onBuildingsTouch], a po dotknięciu pustej mapy pustą listę. Dotknięcie znaku bariery zgłasza identyfikator
     * jej zgłoszenia przez [onBarrierTouch] i nie zgłasza konturu pod znakiem.
     *
     * Wymagania: US1.5, NFR-56
     */
    @Composable
    fun Show(
        data: CampusMapData,
        selection: MapSelection,
        move: CameraTransition,
        inset: MapInset,
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

/**
 * Kolory stylu mapy z palety mapy motywu; poza granicą terenu na mapę pada `background` motywu z kryciem
 * `mapa.krycie.teren_poza`.
 *
 * Wymagania: NFR-16, BR-G5
 */
@Composable
@ReadOnlyComposable
internal fun MapColors.styleColors(): MapStyleColors =
    MapStyleColors(
        land = lad.hex(),
        building = pwr.hex(),
        buildingOutline = pwrKontur.hex(),
        selectedOutline = pwrWybrany.hex(),
        isNight = lad.luminance() < NIGHT_LAND_LUMINANCE,
        edge = granicaTerenu.hex(),
        edgeHalo = granicaTerenuObwodka.hex(),
        outside = MaterialTheme.colorScheme.background.hex(),
        outsideOpacity = DostepnyKampusTheme.mapOpacity.terenPoza.toExactDouble(),
        dimmed = budynek.hex(),
        dimmedOutline = budynekKontur.hex(),
        plan =
            PlanColors(
                classroom = pSala.hex(),
                corridor = pKorytarz.hex(),
                other = pInne.hex(),
                elevator = pWinda.hex(),
                toilet = pWc.hex(),
                wall = pKontur.hex(),
            ),
    )

internal fun Color.hex(): String = "#%06X".format(Locale.ROOT, toArgb() and RGB_MASK)

/**
 * Krawędzie mapy zasłonięte w pikselach: [top] przez górny pasek, [bottom] przez arkusz V2. Kamera stawia cel
 * w środku części odkrytej.
 *
 * Wymagania: US2.8, NFR-56
 */
data class MapInset(val top: Double = 0.0, val bottom: Double = 0.0)

/** Dopełnienie kamery MapLibre: lewo, góra, prawo, dół. */
internal fun MapInset.padding(): DoubleArray = doubleArrayOf(0.0, top, 0.0, bottom)

internal fun MapCamera.toPosition(inset: MapInset = MapInset()): CameraPosition =
    CameraPosition
        .Builder()
        .target(LatLng(latitude = center.latitude, longitude = center.longitude))
        .zoom(zoom)
        .padding(inset.padding())
        .build()

internal fun CameraPosition.toCamera(): MapCamera =
    MapCamera(
        center = GeoPoint(longitude = target?.longitude ?: 0.0, latitude = target?.latitude ?: 0.0),
        zoom = zoom,
    )

private fun Float.toExactDouble(): Double = toString().toDouble()
