/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.inspection.api.ChangedMarker;
import io.github.dostepnykampus.inspection.api.MarkerChanges;
import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.SyncedMarker;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Zmiany znaczników pięter dla innych modułów: jeden odczyt z bazy przetłumaczony na typy
 * {@code api}; transakcję daje wywołujący.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3</p>
 *
 * @since 0.1.0
 */
@Component
public final class MarkerFeed implements MarkerChanges {

    /** Odczyt znaczników. */
    private final MarkerRepository repository;

    MarkerFeed(final MarkerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ChangedMarker> changedSince(final Instant since, final String reader) {
        return this.repository.changedSince(since, reader).stream()
            .map(MarkerFeed::changed).toList();
    }

    private static ChangedMarker changed(final SyncedMarker marker) {
        final MarkerPlace place = marker.place();
        String floor = null;
        if (place instanceof MarkerPlace.Floor on) {
            floor = on.floor();
        }
        return new ChangedMarker(
            marker.id(), place.building(), floor, marker.created(), marker.changed(),
            marker.revoked(), marker.mine()
        );
    }
}
