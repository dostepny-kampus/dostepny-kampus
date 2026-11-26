/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.api;

import java.time.Instant;
import java.util.List;

/**
 * Zmiany znaczników pięter do pobrania przez telefon.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface MarkerChanges {

    /**
     * Znaczniki wszystkich Zbierających zmienione od {@code since} włącznie, także cofnięte.
     *
     * @param since Chwila, od której liczą się zmiany
     * @param reader Identyfikator konta z tokenu
     * @return Znaczniki z oznaczeniem własnych
     */
    List<ChangedMarker> changedSince(Instant since, String reader);
}
