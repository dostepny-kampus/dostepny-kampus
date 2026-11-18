/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import io.github.dostepnykampus.core.model.GeoPoint

/**
 * Wyjście ewakuacyjne budynku z warstwy wejść planów PWr (`EvacuationExit` z kontraktu API); nie jest wejściem.
 *
 * Wymagania: US2.6, BR-27
 *
 * @property id identyfikator wyjścia
 * @property location położenie wyjścia
 * @property description opis wyjścia, np. „Wyjście ewakuacyjne z pomieszczenia 41”; pusty, gdy dane go nie podają
 * @property doorWidthCm szerokość drzwi w centymetrach; 0, gdy dane jej nie podają
 */
public data class EvacuationExit(
    val id: String,
    val location: GeoPoint,
    val description: String = "",
    val doorWidthCm: Int = 0,
)
