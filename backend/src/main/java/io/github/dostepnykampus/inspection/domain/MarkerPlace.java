/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

/**
 * Miejsce znacznika: cały budynek podstawowy albo piętro budynku szczegółowego.
 *
 * <p>Wymagania: US15.1, BR-20</p>
 *
 * @since 0.1.0
 */
public sealed interface MarkerPlace permits MarkerPlace.Building, MarkerPlace.Floor {

    /**
     * Symbol budynku znacznika.
     *
     * @return Symbol budynku
     */
    String building();

    /**
     * Cały budynek podstawowy, który nie ma pięter.
     *
     * @param building Symbol budynku
     * @since 0.1.0
     */
    record Building(String building) implements MarkerPlace {
    }

    /**
     * Piętro budynku szczegółowego.
     *
     * @param building Symbol budynku
     * @param floor Identyfikator piętra w budynku
     * @since 0.1.0
     */
    record Floor(String building, String floor) implements MarkerPlace {
    }
}
