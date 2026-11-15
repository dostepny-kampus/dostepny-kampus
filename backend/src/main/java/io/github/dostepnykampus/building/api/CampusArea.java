/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

/**
 * Teren PWr dla innych modułów: suma pięciu terenów uczelni, każdy ograniczony
 * zamkniętą granicą z zapasem wokół budynków i obszarów uczelni. Punkt na linii
 * granicy należy do terenu. Poza terenem serwer nie przyjmuje zgłoszenia.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface CampusArea {

    /**
     * Mówi, czy punkt leży na terenie PWr.
     *
     * @param lat Szerokość geograficzna w stopniach WGS84
     * @param lon Długość geograficzna w stopniach WGS84
     * @return Prawda, gdy punkt leży wewnątrz granicy któregoś terenu PWr albo na niej
     */
    boolean covers(double lat, double lon);
}
