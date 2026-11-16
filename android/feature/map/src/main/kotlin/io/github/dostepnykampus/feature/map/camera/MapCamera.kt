/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.style.Basemap
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** Powiększenie pierwszego widoku: symbole budynków są czytelne. */
internal const val START_ZOOM = 17.0

/** Powiększenie po zaznaczeniu wejścia: pinezka i sąsiednie wejścia są czytelne. Wymagania: US3.3 */
internal const val ENTRANCE_ZOOM = 19.0

private const val MAX_ZOOM = 20.0
private const val QUARTER_VIEW_DEGREES_AT_ZOOM_0 = 70.0
private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0
private const val BASEMAP_ZOOM_MARGIN = 0.5

/**
 * Widok mapy V1. Przyciski „Powiększ”, „Pomniejsz” i cztery strzałki zmieniają go bez gestów (WCAG 2.5.1, WCAG 2.5.7).
 *
 * Wymagania: NFR-53
 *
 * @property center środek widoku
 * @property zoom powiększenie mapy; pod arkuszem V2 [center] stoi w środku części odkrytej
 */
data class MapCamera(val center: GeoPoint, val zoom: Double)

/**
 * Wolna część mapy nad arkuszem V2, w dp.
 *
 * @property widthDp szerokość mapy
 * @property heightDp wysokość mapy nad arkuszem
 * @property endDp szerokość pasa przy prawym brzegu zajętego przez przyciski mapy; 0 bez przycisków
 * @property marginDp odstęp wpisanego budynku od brzegów wolnej części
 */
internal data class FreeMap(val widthDp: Double, val heightDp: Double, val endDp: Double, val marginDp: Double) {
    /** Czy w wolnej części stoją przyciski mapy: mają tam zarezerwowany pas [endDp]. Wymagania: NFR-53 */
    val hasRoomForControls: Boolean get() = endDp > 0
}

/** Widok po „Powiększ”: o jeden poziom, najwyżej do powiększenia planu budynku. */
internal fun MapCamera.zoomedIn(): MapCamera = copy(zoom = (zoom + 1).coerceAtMost(MAX_ZOOM))

/** Widok po „Pomniejsz”: o jeden poziom; najmniejsze powiększenie obszaru kampusu daje [MapLimits.clamp]. */
internal fun MapCamera.zoomedOut(): MapCamera = copy(zoom = zoom - 1)

/**
 * Widok po strzałce [direction]: przy każdym powiększeniu o około ćwierć szerokości widoku telefonu. Krok
 * w pionie jest krótszy o cosinus szerokości (rzut Merkatora), więc w obu kierunkach ma tę samą długość.
 */
internal fun MapCamera.moved(direction: PanDirection): MapCamera {
    val step = QUARTER_VIEW_DEGREES_AT_ZOOM_0 / 2.0.pow(zoom)
    val latitudeStep = step * cos(Math.toRadians(center.latitude))
    return copy(
        center =
            GeoPoint(
                longitude = center.longitude + direction.east * step,
                latitude = center.latitude + direction.north * latitudeStep,
            ),
    )
}

/**
 * Granice widoku V1 na mapie o rozmiarze [widthDp] × [heightDp] dp: cały widok, nie tylko jego środek, leży
 * w prostokącie [area]. Te same wartości dostaje MapLibre dla gestów.
 *
 * Wymagania: US1.7, NFR-39, NFR-53
 *
 * @property bounds prostokąt terenu (granica terenu z zapasem)
 * @property widthDp szerokość widoku mapy w dp
 * @property heightDp wysokość widoku mapy w dp
 */
internal class MapLimits(val bounds: CampusBounds, val widthDp: Double, val heightDp: Double) {
    /** Prostokąt widoku: sam [bounds], bez rozszerzania do proporcji widoku. */
    val area: CampusBounds get() = bounds

    /**
     * Najmniejsze powiększenie: [area] wypełnia widok w krótszym wymiarze, więc widok nie wychodzi poza [area],
     * a wzdłuż dłuższego wymiaru mapę się przesuwa.
     */
    val minZoom: Double =
        maxOf(
            a = fitZoom(viewDp = widthDp, worldShare = (area.east - area.west) / FULL_TURN_DEGREES),
            b = fitZoom(viewDp = heightDp, worldShare = (mercatorY(area.north) - mercatorY(area.south)) / (2 * PI)),
        )

    /** Prostokąt podkładu: [area], bo widok z niego nie wychodzi. Wymagania: NFR-39 */
    val tileBounds: CampusBounds get() = area

    /**
     * Podkład terenu od pół poziomu poniżej [minZoom]. Przelot na inny teren oddala mapę
     * bardziej, więc w jego środku podkładu nie ma, a przy zejściu do celu wraca.
     *
     * Wymagania: NFR-39
     */
    val basemap: Basemap get() = Basemap(minZoom = minZoom - BASEMAP_ZOOM_MARGIN)

    /**
     * Prostokąt, w którym leży środek odkrytej części mapy o rozmiarze [viewWidthDp] × [viewHeightDp] dp przy
     * powiększeniu [zoom]: [area] pomniejszony o pół tej części. Część większa niż [area] daje sam środek [area].
     */
    fun centerBounds(zoom: Double, viewWidthDp: Double = widthDp, viewHeightDp: Double = heightDp): CampusBounds {
        val world = WORLD_DP_AT_ZOOM_0 * 2.0.pow(zoom)
        val halfLongitude = viewWidthDp / 2 / world * FULL_TURN_DEGREES
        val halfY = viewHeightDp / 2 / world * (2 * PI)
        val middleLongitude = (area.west + area.east) / 2
        val middleY = (mercatorY(area.south) + mercatorY(area.north)) / 2
        val west = minOf(a = area.west + halfLongitude, b = middleLongitude)
        val east = maxOf(a = area.east - halfLongitude, b = middleLongitude)
        val south = minOf(a = mercatorY(area.south) + halfY, b = middleY)
        val north = maxOf(a = mercatorY(area.north) - halfY, b = middleY)
        return CampusBounds(west = west, south = latitudeOf(south), east = east, north = latitudeOf(north))
    }

    /**
     * Widok [camera] w granicach dla odkrytej części mapy [viewWidthDp] × [viewHeightDp] dp: powiększenie co najmniej
     * [minZoom], środek w [centerBounds]. Strzałki i „Pomniejsz” zatrzymują się na brzegu bez komunikatu.
     */
    fun clamp(camera: MapCamera, viewWidthDp: Double = widthDp, viewHeightDp: Double = heightDp): MapCamera {
        val zoom = camera.zoom.coerceAtLeast(minZoom)
        val box = centerBounds(zoom = zoom, viewWidthDp = viewWidthDp, viewHeightDp = viewHeightDp)
        val longitude = camera.center.longitude.coerceIn(minimumValue = box.west, maximumValue = box.east)
        val latitude = camera.center.latitude.coerceIn(minimumValue = box.south, maximumValue = box.north)
        return MapCamera(center = GeoPoint(longitude = longitude, latitude = latitude), zoom = zoom)
    }
}

/** Powiększenie, przy którym część świata [worldShare] (0–1) zajmuje [viewDp] dp widoku. */
internal fun fitZoom(viewDp: Double, worldShare: Double): Double = log2(viewDp / (WORLD_DP_AT_ZOOM_0 * worldShare))

/** Współrzędna pionowa szerokości [latitude] w rzucie Merkatora, w radianach. */
internal fun mercatorY(latitude: Double): Double = asinh(tan(Math.toRadians(latitude)))

/** Szerokość geograficzna w stopniach dla współrzędnej pionowej [y] rzutu Merkatora w radianach. */
internal fun latitudeOf(y: Double): Double = Math.toDegrees(atan(sinh(y)))

/**
 * Kierunek strzałki przesuwania mapy, np. „Przesuń mapę w górę”.
 *
 * Wymagania: NFR-53
 *
 * @property east przesunięcie środka na wschód: 1, na zachód: −1
 * @property north przesunięcie środka na północ: 1, na południe: −1
 */
internal enum class PanDirection(val east: Int, val north: Int) {
    UP(east = 0, north = 1),
    LEFT(east = -1, north = 0),
    RIGHT(east = 1, north = 0),
    DOWN(east = 0, north = -1),
}
