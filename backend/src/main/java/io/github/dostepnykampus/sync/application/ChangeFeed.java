/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import io.github.dostepnykampus.barrier.api.ChangedSubmission;
import io.github.dostepnykampus.barrier.api.SubmissionChanges;
import io.github.dostepnykampus.inspection.api.ChangedMarker;
import io.github.dostepnykampus.inspection.api.MarkerChanges;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pobranie zmian do telefonu: zmiany od kursora z modułów właścicieli danych.
 *
 * <p>TODO(#3852): wejścia ze zmienionym oznaczeniem w pobraniu zmian.</p>
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, US8.1, US1.3, US10.6, BR-20</p>
 *
 * @since 0.1.0
 */
@Service
public class ChangeFeed {

    /** Zmiany zgłoszeń. */
    private final SubmissionChanges submissions;

    /** Zmiany znaczników pięter. */
    private final MarkerChanges markers;

    ChangeFeed(final SubmissionChanges submissions, final MarkerChanges markers) {
        this.submissions = submissions;
        this.markers = markers;
    }

    /**
     * Zmiany od kursora {@code since} włącznie: własne zgłoszenia konta oraz cudze przyjęte
     * i archiwalne, a znaczniki tylko dla Zbierającego, inni dostają pustą listę. Kursor wyniku to
     * chwila najpóźniejszej zwróconej zmiany, a bez zmian {@code since}.
     *
     * @param since Kursor z poprzedniego pobrania
     * @param reader Identyfikator konta z tokenu
     * @param collector Czy konto ma rolę Zbierającego
     * @return Zmiany i kursor następnego pobrania
     */
    @Transactional(readOnly = true)
    public PulledChanges pull(final Instant since, final String reader, final boolean collector) {
        final List<ChangedSubmission> reports = this.submissions.changedSince(since, reader);
        List<ChangedMarker> changed = List.of();
        if (collector) {
            changed = this.markers.changedSince(since, reader);
        }
        return new PulledChanges(
            Stream.concat(
                reports.stream().map(ChangedSubmission::changed),
                changed.stream().map(ChangedMarker::changed)
            ).max(Comparator.naturalOrder()).orElse(since),
            reports.stream().map(ChangedSubmission::submission).toList(),
            changed.stream().map(ChangeFeed::pulled).toList()
        );
    }

    private static PulledMarker pulled(final ChangedMarker marker) {
        return new PulledMarker(
            marker.id(), marker.buildingSymbol(), marker.floorId(), marker.created(),
            marker.revoked(), marker.mine()
        );
    }
}
