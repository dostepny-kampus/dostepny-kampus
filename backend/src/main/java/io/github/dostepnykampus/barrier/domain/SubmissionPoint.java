/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Punkt zgłoszenia w WGS84, w kolejności długość, szerokość (US4, US7).
 *
 * <p>Każda droga utworzenia punktu przyjmuje tylko skończone współrzędne
 * w domkniętym zakresie WGS84; poza nim rzuca {@link InvalidSubmission}.</p>
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
        SubmissionPoint.requireWithin(longitude, 180.0, "Długość");
        SubmissionPoint.requireWithin(latitude, 90.0, "Szerokość");
    }

    private static void requireWithin(final double value, final double limit, final String name) {
        if (Double.isNaN(value) || Math.abs(value) > limit) {
            throw new InvalidSubmission(
                String.format("%s %s jest poza zakresem WGS84 punktu zgłoszenia", name, value)
            );
        }
    }
}
