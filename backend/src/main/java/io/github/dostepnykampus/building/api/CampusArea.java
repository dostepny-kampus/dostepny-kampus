/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

/**
 * Teren PWr dla innych modułów: wszystko do 500 m od konturu któregokolwiek
 * budynku PWr, w metrach na ziemi. Poza nim serwer nie przyjmuje
 * zgłoszenia.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface CampusArea {

    /**
     * Najdalsza odległość od konturu budynku PWr w metrach.
     *
     * <p>Wymagania: BR-G5</p>
     */
    double RADIUS_METERS = 500.0;

    /**
     * Mówi, czy punkt leży na terenie PWr.
     *
     * @param lat Szerokość geograficzna w stopniach WGS84
     * @param lon Długość geograficzna w stopniach WGS84
     * @return Prawda, gdy punkt leży najwyżej 500 m od konturu budynku PWr
     */
    boolean covers(double lat, double lon);
}
