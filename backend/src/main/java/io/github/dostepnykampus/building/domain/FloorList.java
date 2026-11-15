/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import java.util.List;

/**
 * Piętra budynku ze źródłem danych.
 *
 * <p>Wymagania: US16, NFR-40</p>
 *
 * @param source Nazwa źródła danych planów pięter
 * @param floors Piętra posortowane rosnąco według kolejności
 * @since 0.1.0
 */
public record FloorList(String source, List<Floor> floors) {

    /**
     * Nazwa źródła planów pięter z kontraktu (schemat PwrSource), wspólna dla
     * odpowiedzi o piętrach, pomieszczeniach i wyszukiwaniu sali.
     */
    public static final String PWR_SOURCE = "Politechnika Wrocławska";
}
