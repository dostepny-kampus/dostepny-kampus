/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusBounds
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.tan

/** Powiększenie pierwszego widoku: symbole budynków są czytelne (decyzja 80). */
internal const val START_ZOOM = 17.0

/** Powiększenie po zaznaczeniu wejścia: pinezka i sąsiednie wejścia są czytelne (US3.3). */
internal const val ENTRANCE_ZOOM = 19.0

private const val MAX_ZOOM = 20.0
private const val QUARTER_VIEW_DEGREES_AT_ZOOM_0 = 70.0
private const val WORLD_DP_AT_ZOOM_0 = 512.0
private const val FULL_TURN_DEGREES = 360.0

/**
 * Widok mapy V1. Przyciski „Powiększ”, „Pomniejsz” i cztery strzałki zmieniają go bez gestów (NFR-53,
 * WCAG 2.5.1, WCAG 2.5.7).
 *
 * @property center środek widoku
 * @property zoom powiększenie mapy
 * @property bottomInset zasłonięty dół widoku w pikselach, np. pod arkuszem V2; [center] stoi wtedy w środku
 *   części odkrytej
 */
data class MapCamera(val center: GeoPoint, val zoom: Double, val bottomInset: Double = 0.0)

/**
 * Widok pod arkuszem V2 wysokim na [bottomInset] pikseli: budynek [focus] w środku części mapy nad arkuszem,
 * bez zmiany powiększenia, jak w makiecie hi-fi V2. Bez budynku widok zostaje.
 */
internal fun MapCamera.aboveSheet(focus: GeoPoint?, bottomInset: Double): MapCamera =
    focus?.let { copy(center = it, bottomInset = bottomInset) } ?: this

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
 * Granice widoku V1 na mapie o rozmiarze [widthDp] × [heightDp] dp (NFR-39, NFR-53). Te same wartości dostaje
 * MapLibre dla gestów.
 *
 * @property bounds obszar kampusu, poza który środek widoku nie wychodzi
 * @property widthDp szerokość widoku mapy w dp
 * @property heightDp wysokość widoku mapy w dp
 */
internal class MapLimits(val bounds: CampusBounds, val widthDp: Double, val heightDp: Double) {
    /** Najmniejsze powiększenie: cały [bounds] mieści się w widoku, a bardziej oddalić mapy się nie da. */
    val minZoom: Double =
        minOf(
            a = fitZoom(viewDp = widthDp, worldShare = (bounds.east - bounds.west) / FULL_TURN_DEGREES),
            b = fitZoom(viewDp = heightDp, worldShare = (mercatorY(bounds.north) - mercatorY(bounds.south)) / (2 * PI)),
        )

    /**
     * Widok [camera] w granicach: środek w [bounds], powiększenie co najmniej [minZoom]. Strzałki i „Pomniejsz”
     * zatrzymują się na granicy bez komunikatu.
     */
    fun clamp(camera: MapCamera): MapCamera {
        val longitude = camera.center.longitude.coerceIn(minimumValue = bounds.west, maximumValue = bounds.east)
        val latitude = camera.center.latitude.coerceIn(minimumValue = bounds.south, maximumValue = bounds.north)
        val center = GeoPoint(longitude = longitude, latitude = latitude)
        return camera.copy(center = center, zoom = camera.zoom.coerceAtLeast(minZoom))
    }

    private fun fitZoom(viewDp: Double, worldShare: Double): Double = log2(viewDp / (WORLD_DP_AT_ZOOM_0 * worldShare))

    private fun mercatorY(latitude: Double): Double = asinh(tan(Math.toRadians(latitude)))
}

/**
 * Kierunek strzałki przesuwania mapy, np. „Przesuń mapę w górę” (NFR-53).
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
