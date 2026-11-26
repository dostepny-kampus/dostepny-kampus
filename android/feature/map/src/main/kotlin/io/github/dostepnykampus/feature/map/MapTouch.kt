/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.TouchSign
import kotlin.math.abs

/**
 * Zapytania o numery konturów budynków przy dotknięciu: [hit] pod palcem, [near] w zasięgu dotknięcia.
 *
 * Wymagania: US2, NFR-17
 */
internal data class OutlinesQuery(val hit: () -> List<Int>, val near: () -> List<Int>)

/**
 * Obiekt wybrany dotknięciem mapy V1.
 *
 * Wymagania: US1.5, US2, NFR-17
 */
internal sealed interface MapTouch {
    /** Znak [sign] obiektu o identyfikatorze [id]. */
    data class Sign(val sign: TouchSign, val id: String) : MapTouch

    /** Numery konturów [hit] pod palcem; [near] zwraca numery konturów w zasięgu dotknięcia przy wywołaniu. */
    data class Outlines(val hit: List<Int>, val near: () -> List<Int>) : MapTouch
}

/**
 * Znak, którego obraz jest pod palcem według [signsUnder], pierwszy w kolejności [TouchSign]. Bez obrazu pod palcem
 * pierwszy znak w tej kolejności, dla którego [signsNear] zwraca identyfikator w zasięgu dotknięcia, a bez znaku
 * kontury z [outlines]. Znaków po trafionym ani konturów przy trafionym znaku nie sprawdza.
 *
 * Wymagania: US1.5, US2, US2.1, NFR-17
 */
internal fun chooseTouch(
    signsUnder: (TouchSign) -> List<String>,
    signsNear: (TouchSign) -> List<String>,
    outlines: OutlinesQuery,
): MapTouch =
    signsUnder(TouchSign.ROOM_STEP).firstOrNull()?.let { MapTouch.Sign(sign = TouchSign.ROOM_STEP, id = it) }
        ?: firstSign { sign -> if (sign == TouchSign.ROOM_STEP) emptyList() else signsUnder(sign) }
        ?: firstSign(signsNear)
        ?: signsUnder(TouchSign.ROOM).firstOrNull()?.let { MapTouch.Sign(sign = TouchSign.ROOM, id = it) }
        ?: MapTouch.Outlines(hit = outlines.hit(), near = outlines.near)

private fun firstSign(ids: (TouchSign) -> List<String>): MapTouch.Sign? =
    TouchSign.entries.asSequence().filterNot { it == TouchSign.ROOM }.firstNotNullOfOrNull { sign ->
        ids(sign).firstOrNull()?.let { id -> MapTouch.Sign(sign = sign, id = id) }
    }

/**
 * Znak [id], którego punkt leży [dx] w poziomie i [dy] w pionie od palca, w pikselach ekranu.
 *
 * Wymagania: NFR-17
 */
internal data class SignPoint(val id: String, val dx: Float, val dy: Float)

/**
 * Identyfikatory znaków w zasięgu dotknięcia: najpierw obrazy pod palcem [under], potem znaki z [near], których
 * punkt leży najwyżej [reach] od palca w poziomie i w pionie. Obszar dotyku znaku to więc kwadrat o boku dwa razy
 * [reach] wokół jego punktu, a przy większym obrazie sam obraz.
 *
 * Wymagania: US1.5, US2.1, US16.1, NFR-17
 */
internal fun signsInReach(under: List<String>, near: List<SignPoint>, reach: Float): List<String> =
    (under + near.filter { abs(it.dx) <= reach && abs(it.dy) <= reach }.map { it.id }).distinct()
