/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

/**
 * Nazwa źródła danych w podpisie odpowiedzi, wartość jak schemat PwrSource w OpenAPI.
 *
 * <p>Wymagania: NFR-40</p>
 *
 * @since 0.1.0
 */
public enum DataSource {

    /** Plany pięter i wejścia z danych Politechniki Wrocławskiej. */
    PWR("Politechnika Wrocławska");

    /**
     * Nazwa źródła w podpisie.
     */
    private final String label;

    /**
     * Ctor.
     *
     * @param label Nazwa źródła w podpisie
     */
    DataSource(final String label) {
        this.label = label;
    }

    /**
     * Nazwa źródła w podpisie.
     *
     * @return Nazwa źródła
     */
    public String label() {
        return this.label;
    }
}
