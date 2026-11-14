/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.Floor;

/**
 * Piętro budynku w odpowiedzi HTTP, zgodne ze schematem Floor.
 *
 * <p>Wymagania: US16</p>
 *
 * @param id Identyfikator piętra
 * @param label Etykieta piętra z danych PWr
 * @param order Kolejność piętra w budynku; większa liczba to wyższe piętro
 * @since 0.1.0
 */
public record FloorResponse(String id, String label, int order) {

    /**
     * Odpowiedź z piętra budynku.
     *
     * @param floor Piętro budynku
     */
    public FloorResponse(final Floor floor) {
        this(floor.id(), floor.label(), floor.order());
    }
}
