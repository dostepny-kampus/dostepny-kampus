/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import io.github.dostepnykampus.core.model.GeoPoint

private const val IMPORT_PLACEHOLDER_NAME = "Wejście"

/**
 * Wejście do budynku z oznaczeniem wejścia (`Entrance` z kontraktu API).
 *
 * Wymagania: US17.3, US2.1, US3.3, BR-27
 *
 * @property id identyfikator wejścia
 * @property buildingSymbol symbol budynku, do którego prowadzi wejście
 * @property name opis wejścia na karcie budynku, np. „Wejście bramą od ul. Norwida”; `null`, gdy dane go nie podają
 * @property isMain czy to wejście główne (pole `main` w kontrakcie)
 * @property type rodzaj wejścia z OpenStreetMap (tag `entrance`, np. `main`, `service`, `emergency`); `null`
 *   poza danymi OSM
 * @property location położenie wejścia
 * @property marking oznaczenie wejścia z danych
 * @property assessment dzień i źródło oznaczenia z serwera; `null` dla wejścia z pliku konturów,
 *   który ich nie podaje
 * @property floorId piętro wejścia z listy pięter budynku (`Floor.id`); `null`, gdy dane go nie podają
 * @property approach opis dojścia do wejścia, np. „Do wejścia prowadzą schody.”; pusty, gdy dane go nie podają
 * @property doorWidthCm szerokość drzwi w centymetrach; 0, gdy dane jej nie podają
 */
public data class Entrance(
    val id: String,
    val buildingSymbol: String,
    val name: String?,
    val isMain: Boolean,
    val type: String?,
    val location: GeoPoint,
    val marking: EntranceMarking,
    val assessment: EntranceAssessment? = null,
    val floorId: String? = null,
    val approach: String = "",
    val doorWidthCm: Int = 0,
) {
    /**
     * Czy wejście ma nazwę w danych: niepustą i inną niż „Wejście”, które import nadaje wejściu bez nazwy.
     *
     * Wymagania: BR-25, BR-27
     */
    val hasName: Boolean
        get() = !name.isNullOrBlank() && name != IMPORT_PLACEHOLDER_NAME

    /** Czy rodzaj wejścia kwalifikuje je do pokazania odwiedzającym. Wymagania: BR-25 */
    val isForVisitors: Boolean
        get() =
            when (type) {
                "emergency", "exit", "garage" -> false
                else -> true
            }
}
