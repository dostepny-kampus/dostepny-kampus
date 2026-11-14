/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

/**
 * Grupa funkcji pomieszczenia z kontraktu API.
 *
 * <p>Wymagania: US16.1, US18.1</p>
 *
 * @since 0.1.0
 */
public enum RoomCategory {

    /**
     * Sala dydaktyczna, laboratorium, pracownia komputerowa.
     */
    CLASSROOM,

    /**
     * Sala wykładowa i aula.
     */
    LECTURE_HALL,

    /**
     * Korytarz.
     */
    CORRIDOR,

    /**
     * Klatka schodowa i schody poza nią.
     */
    STAIRCASE,

    /**
     * Winda.
     */
    ELEVATOR,

    /**
     * Toaleta dla osób z niepełnosprawnością.
     */
    ACCESSIBLE_TOILET,

    /**
     * Pozostałe toalety.
     */
    TOILET,

    /**
     * Dziekanat, biuro, gabinet, pomieszczenie administracyjne.
     */
    OFFICE,

    /**
     * Wszystko inne.
     */
    OTHER
}
