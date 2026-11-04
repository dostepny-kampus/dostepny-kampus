/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Punkt zgłoszenia w WGS84, w kolejności długość, szerokość (US4, US7).
 *
 * @param longitude Długość geograficzna w stopniach, od -180 do 180
 * @param latitude Szerokość geograficzna w stopniach, od -90 do 90
 * @since 0.1.0
 */
public record SubmissionPoint(double longitude, double latitude) {
}
