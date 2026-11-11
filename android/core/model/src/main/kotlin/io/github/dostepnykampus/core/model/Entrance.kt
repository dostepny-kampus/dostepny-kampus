/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Wejście do budynku z oznaczeniem wejścia (`Entrance` z kontraktu API).
 *
 * @property id identyfikator wejścia
 * @property buildingSymbol symbol budynku, do którego prowadzi wejście
 * @property name opis wejścia na karcie budynku, np. „Wejście bramą od ul. Norwida”; `null`, gdy dane go nie podają
 * @property isMain czy to wejście główne (pole `main` w kontrakcie)
 * @property type rodzaj wejścia z OpenStreetMap (tag `entrance`, np. `main`, `service`, `emergency`); `null`
 *   poza danymi OSM
 * @property location położenie wejścia
 * @property marking oznaczenie wejścia z danych
 * @property assessment dzień i źródło oznaczenia z serwera (US17.3); `null` dla wejścia z pliku konturów,
 *   który ich nie podaje
 */
data class Entrance(
    val id: String,
    val buildingSymbol: String,
    val name: String?,
    val isMain: Boolean,
    val type: String?,
    val location: GeoPoint,
    val marking: EntranceMarking,
    val assessment: EntranceAssessment? = null,
) {
    /** Czy rodzaj wejścia kwalifikuje je do pokazania odwiedzającym (BR-25). */
    val isForVisitors: Boolean
        get() =
            when (type) {
                "emergency", "exit", "garage" -> false
                else -> true
            }
}
