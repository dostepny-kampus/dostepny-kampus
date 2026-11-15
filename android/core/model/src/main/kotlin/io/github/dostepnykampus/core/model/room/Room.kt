/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.room

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoPoint

/**
 * Grupa funkcji pomieszczenia wyznaczona przez serwer z funkcji PWr (`RoomCategory` z kontraktu API).
 *
 * Wymagania: US16.1, US18.1
 */
public enum class RoomCategory {
    /** Sala dydaktyczna, laboratorium, pracownia komputerowa. */
    CLASSROOM,

    /** Sala wykładowa i aula. */
    LECTURE_HALL,

    /** Korytarz. */
    CORRIDOR,

    /** Klatka schodowa i schody. */
    STAIRCASE,

    /** Winda. */
    ELEVATOR,

    /** Toaleta dla osób z niepełnosprawnością. */
    ACCESSIBLE_TOILET,

    /** Pozostałe toalety. */
    TOILET,

    /** Dziekanat, biuro, gabinet. */
    OFFICE,

    /** Wszystko inne. */
    OTHER,
}

/**
 * Pomieszczenie z planu piętra (`Room` z kontraktu API); grupę, punkt podpisu i znacznik podpisu liczy serwer.
 *
 * Wymagania: US16.1
 *
 * @property id identyfikator pomieszczenia
 * @property number numer z danych PWr; `null`, gdy plan go nie podaje
 * @property function funkcja z danych PWr, np. „Winda”
 * @property category grupa funkcji
 * @property geometry kontur pomieszczenia
 * @property labelPoint punkt wewnątrz pomieszczenia na numer i znacznik wyniku
 * @property isLabelled czy numer jest podpisany na planie (pole `labelled`)
 * @property liftId winda łącząca piętra, wspólna dla pomieszczeń jednego szybu; `null` poza windą
 * @property liftFloors identyfikatory pięter windy od najniższego; pusta poza windą
 */
public data class Room(
    val id: String,
    val number: String?,
    val function: String,
    val category: RoomCategory,
    val geometry: GeoArea,
    val labelPoint: GeoPoint,
    val isLabelled: Boolean,
    val liftId: String? = null,
    val liftFloors: List<String> = emptyList(),
)

/**
 * Pomieszczenia piętra ze źródłem danych (`RoomList`).
 *
 * Wymagania: US16.1, NFR-40
 *
 * @property source źródło do podpisu planu, „Politechnika Wrocławska”
 * @property floorOutline obrys piętra rysowany jako ściana zewnętrzna; bez wielokątów, gdy piętro nie ma pomieszczeń
 * @property rooms pomieszczenia piętra
 */
public data class RoomList(val source: String, val floorOutline: GeoArea, val rooms: List<Room>)
