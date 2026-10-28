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
 * @property name opis wejścia na karcie budynku, np. „Wejście bramą od ul. Norwida”
 * @property isMain czy to wejście główne (pole `main` w kontrakcie)
 * @property location położenie wejścia
 * @property marking oznaczenie wejścia z danych
 */
data class Entrance(
    val id: String,
    val buildingSymbol: String,
    val name: String,
    val isMain: Boolean,
    val location: GeoPoint,
    val marking: EntranceMarking,
)
