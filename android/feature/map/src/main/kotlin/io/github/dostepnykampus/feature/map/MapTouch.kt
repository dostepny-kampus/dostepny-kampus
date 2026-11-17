/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.barrier.BARRIER_ID
import io.github.dostepnykampus.feature.map.style.BARRIERS_LAYER

/**
 * Znak mapy V1, który reaguje na dotknięcie, z warstwą [layer] i właściwością [idKey] z identyfikatorem obiektu.
 * Kolejność wpisów to pierwszeństwo przy kilku znakach w zasięgu dotknięcia.
 *
 * Wymagania: US1.5, NFR-17
 */
internal enum class TouchSign(val layer: String, val idKey: String) {
    /** Znak bariery ze zgłoszenia przyjętego. */
    BARRIER(layer = BARRIERS_LAYER, idKey = BARRIER_ID),
}

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
 * Pierwszy znak w kolejności [TouchSign], dla którego [signsNear] zwraca identyfikator w zasięgu dotknięcia;
 * bez znaku kontury z [outlines]. Znaków po trafionym ani konturów przy trafionym znaku nie sprawdza.
 *
 * Wymagania: US1.5, US2, NFR-17
 */
internal fun chooseTouch(signsNear: (TouchSign) -> List<String>, outlines: OutlinesQuery): MapTouch =
    TouchSign.entries.firstNotNullOfOrNull { sign ->
        signsNear(sign).firstOrNull()?.let { id -> MapTouch.Sign(sign = sign, id = id) }
    } ?: MapTouch.Outlines(hit = outlines.hit(), near = outlines.near)
