/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import io.github.dostepnykampus.inspection.api.ChangedMarker;
import io.github.dostepnykampus.inspection.api.MarkerChanges;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pobranie zmian do telefonu: zmiany od kursora z modułów właścicieli danych.
 *
 * <p>TODO(#3851): zgłoszenia w pobraniu zmian.</p>
 *
 * <p>TODO(#3852): wejścia ze zmienionym oznaczeniem w pobraniu zmian.</p>
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-20</p>
 *
 * @since 0.1.0
 */
@Service
public class ChangeFeed {

    /** Zmiany znaczników pięter. */
    private final MarkerChanges markers;

    ChangeFeed(final MarkerChanges markers) {
        this.markers = markers;
    }

    /**
     * Zmiany od kursora {@code since} włącznie. Znaczniki dostaje tylko Zbierający, inni pustą
     * listę. Kursor wyniku to chwila najpóźniejszej zwróconej zmiany, a bez zmian {@code since}.
     *
     * @param since Kursor z poprzedniego pobrania
     * @param reader Identyfikator konta z tokenu
     * @param collector Czy konto ma rolę Zbierającego
     * @return Zmiany i kursor następnego pobrania
     */
    @Transactional(readOnly = true)
    public PulledChanges pull(final Instant since, final String reader, final boolean collector) {
        List<ChangedMarker> changed = List.of();
        if (collector) {
            changed = this.markers.changedSince(since, reader);
        }
        return new PulledChanges(
            changed.stream().map(ChangedMarker::changed).max(Comparator.naturalOrder())
                .orElse(since),
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
