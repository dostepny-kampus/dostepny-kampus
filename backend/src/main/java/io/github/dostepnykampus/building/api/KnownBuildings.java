/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

/**
 * Budynki PWr znane serwerowi dla innych modułów, np. kontrola budynku zgłoszenia
 * podanego przez klienta.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface KnownBuildings {

    /**
     * Mówi, czy budynek o symbolu istnieje.
     *
     * @param symbol Symbol budynku
     * @return Prawda, gdy budynek istnieje
     */
    boolean known(String symbol);
}
