/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import kotlin.math.cos
import kotlin.math.pow

/** Powiększenie pierwszego widoku: symbole budynków są czytelne (decyzja 80). */
internal const val START_ZOOM = 17.0

private const val MIN_ZOOM = 12.0
private const val MAX_ZOOM = 20.0
private const val QUARTER_VIEW_DEGREES_AT_ZOOM_0 = 70.0

/**
 * Widok mapy V1. Przyciski „Powiększ”, „Pomniejsz” i cztery strzałki zmieniają go bez gestów (NFR-53,
 * WCAG 2.5.1, WCAG 2.5.7).
 *
 * @property center środek widoku
 * @property zoom powiększenie mapy
 */
data class MapCamera(val center: GeoPoint, val zoom: Double)

/** Widok po „Powiększ”: o jeden poziom, najwyżej do powiększenia planu budynku. */
internal fun MapCamera.zoomedIn(): MapCamera = copy(zoom = (zoom + 1).coerceAtMost(MAX_ZOOM))

/** Widok po „Pomniejsz”: o jeden poziom, najmniej do widoku całego miasta. */
internal fun MapCamera.zoomedOut(): MapCamera = copy(zoom = (zoom - 1).coerceAtLeast(MIN_ZOOM))

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
