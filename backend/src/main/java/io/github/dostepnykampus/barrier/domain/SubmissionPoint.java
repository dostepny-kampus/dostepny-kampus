/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import io.github.dostepnykampus.shared.geo.Wgs84;

/**
 * Punkt zgłoszenia w WGS84, w kolejności długość, szerokość.
 *
 * <p>Każda droga utworzenia punktu przyjmuje tylko skończone współrzędne
 * w domkniętym zakresie WGS84 ({@link Wgs84}); poza nim rzuca
 * {@link InvalidSubmission}.</p>
 *
 * <p>Wymagania: US4, US7</p>
 *
 * @param longitude Długość geograficzna w stopniach, od -180 do 180
 * @param latitude Szerokość geograficzna w stopniach, od -90 do 90
 * @since 0.1.0
 */
public record SubmissionPoint(double longitude, double latitude) {

    /**
     * Punkt o skończonych współrzędnych w domkniętym zakresie WGS84.
     *
     * @throws InvalidSubmission Gdy współrzędna jest NaN, nieskończona
     *  albo poza zakresem
     */
    public SubmissionPoint {
        SubmissionPoint.requireWithin(longitude, Wgs84.LONGITUDE, "Długość");
        SubmissionPoint.requireWithin(latitude, Wgs84.LATITUDE, "Szerokość");
    }

    private static void requireWithin(final double value, final Wgs84 axis, final String name) {
        if (!axis.holds(value)) {
            throw new InvalidSubmission(
                String.format("%s %s jest poza zakresem WGS84 punktu zgłoszenia", name, value)
            );
        }
    }
}
