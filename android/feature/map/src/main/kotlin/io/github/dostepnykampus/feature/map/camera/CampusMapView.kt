/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.map.style.BasemapColors
import io.github.dostepnykampus.core.map.style.PathColors
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.style.MapStyleColors
import io.github.dostepnykampus.feature.map.style.PlanColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF

/** Widok mapy za interfejsem: ekran i jego testy nie zależą od natywnej biblioteki mapy. */
fun interface CampusMapView {
    /**
     * Mapa [data] z zaznaczeniem [selection] po ruchu [move], z krawędziami zasłoniętymi według [inset]: cel
     * ruchu stoi w środku części odkrytej. Każdy ruch wykonuje raz; zmiana [inset] zmienia tylko dopełnienie
     * mapy i nie przerywa trwającego ruchu. Po geście widok zgłasza nowy widok przez [onCameraIdle].
     * Dotknięcie wewnątrz konturu zgłasza przez [onBuildingsTouch] symbole tego konturu, dotknięcie obok konturów
     * symbole konturów w zasięgu dotknięcia, a dotknięcie pustej mapy pustą listę. Dotknięcie znaku zgłasza
     * przez [onSignTouch] rodzaj znaku i identyfikator jego obiektu i nie zgłasza konturu pod znakiem.
     *
     * Wymagania: US1.5, US2.1, NFR-56
     */
    @Composable
    fun Show(
        data: CampusMapData,
        selection: MapSelection,
        move: CameraTransition,
        inset: MapInset,
        onCameraIdle: (MapCamera) -> Unit,
        onBuildingsTouch: (List<String>) -> Unit,
        onSignTouch: (TouchSign, String) -> Unit,
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
 * Wygląd stylu mapy z palety mapy motywu, z napisami podkładu powiększonymi o [fontScale]. Wymagania: NFR-16, US1.4
 */
@Composable
@ReadOnlyComposable
internal fun MapColors.styleColors(fontScale: Float = 1f): MapStyleColors =
    MapStyleColors(
        land = lad.hex(),
        building = pwr.hex(),
        buildingOutline = pwrKontur.hex(),
        selectedOutline = pwrWybrany.hex(),
        plan =
            PlanColors(
                classroom = pSala.hex(),
                corridor = pKorytarz.hex(),
                other = pInne.hex(),
                elevator = pWinda.hex(),
                toilet = pWc.hex(),
                wall = pKontur.hex(),
                floorWall = pwrKontur.hex(),
                chosen = pWybrana.hex(),
                chosenOutline = pWybranaKontur.hex(),
            ),
        basicBuilding = pwrPodstawowy.hex(),
        basemap =
            BasemapColors(
                green = zielen.hex(),
                water = woda.hex(),
                road = droga.hex(),
                roadCasing = drogaKontur.hex(),
                rail = tory.hex(),
                path = chodnik.hex(),
                building = budynek.hex(),
                buildingOutline = budynekKontur.hex(),
                streetName = napisUlica.hex(),
                waterName = napisWoda.hex(),
                districtName = napisDzielnica.hex(),
                halo = obwodka.hex(),
            ),
        labelSize = (Typografia.napisyMapy.value * fontScale).toDouble(),
        path = PathColors(edge = sciezkaBrzeg.hex(), inside = sciezkaWnetrze.hex()),
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

/** Rozmiar części mapy pod zasłoniętą górą [top] w pikselach. */
internal fun IntSize.belowTop(top: Double): IntSize = IntSize(width, (height - top).toInt().coerceAtLeast(0))

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
