/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.inspection;

/**
 * Zapis znaczników pięter obejrzanych.
 *
 * <p>Wymagania: US15.1, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface InspectionMarkers {

    /**
     * Zapisuje znacznik; znacznik o identyfikatorze, który już jest, zostaje bez zmian.
     *
     * @param marker Stawiany znacznik
     * @param author Identyfikator konta osoby, która go stawia
     */
    void set(NewMarker marker, String author);
}
