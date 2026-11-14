/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import java.util.Optional;

/**
 * Kontury budynków dla innych modułów, np. budynek zgłoszenia szybkiego
 * ustalany przez serwer.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Outlines {

    /**
     * Zwraca budynek, w którego konturze leży punkt.
     *
     * <p>Wymagania: BR-G2</p>
     *
     * @param lat Szerokość geograficzna w stopniach WGS84
     * @param lon Długość geograficzna w stopniach WGS84
     * @return Symbol budynku; brak oznacza punkt poza budynkiem
     */
    Optional<String> containing(double lat, double lon);
}
