/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.EnumSet;
import java.util.Set;

/**
 * Zgłoszenie podobne do sprawdzanego: w promieniu 10 m, z dowolnego piętra.
 * O duplikacie decyduje Weryfikator, system niczego nie łączy.
 *
 * <p>Wymagania: US10.1, BR-13</p>
 *
 * @param submission Podobne zgłoszenie
 * @param distanceMeters Odległość od sprawdzanego zgłoszenia w metrach
 * @since 0.1.0
 */
public record SimilarSubmission(Submission submission, double distanceMeters) {

    /**
     * Promień podobnych zgłoszeń w metrach, włącznie.
     *
     * <p>Wymagania: BR-13</p>
     */
    public static final double RADIUS_METERS = 10;

    /**
     * Statusy podobnych zgłoszeń; odrzucone i archiwalne są pomijane.
     *
     * <p>Wymagania: US10.1</p>
     */
    public static final Set<SubmissionStatus> STATUSES = EnumSet.of(
        SubmissionStatus.SENT, SubmissionStatus.IN_REVIEW, SubmissionStatus.ACCEPTED
    );
}
