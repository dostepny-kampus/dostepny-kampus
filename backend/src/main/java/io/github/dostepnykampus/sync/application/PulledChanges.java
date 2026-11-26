/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import io.github.dostepnykampus.shared.json.RawJson;
import java.time.Instant;
import java.util.List;

/**
 * Zmiany od kursora i kursor następnego pobrania.
 *
 * <p>Wymagania: US15.1, US15.2, US8.1, US17.2</p>
 *
 * @param cursor Chwila pobrania cofnięta o margines, nie wcześniejsza niż kursor z żądania
 * @param submissions Zgłoszenia zmienione od kursora w kształcie schematu Submission
 * @param entrances Wejścia z oznaczeniem z obchodu w kształcie schematu Entrance
 * @param markers Znaczniki pięter zmienione od kursora
 * @since 0.1.0
 */
public record PulledChanges(
    Instant cursor, List<RawJson> submissions, List<RawJson> entrances,
    List<PulledMarker> markers
) {

    /**
     * Kopiuje listy, żeby rekord był niezmienny.
     *
     * @param cursor Chwila pobrania cofnięta o margines, nie wcześniejsza niż kursor z żądania
     * @param submissions Zgłoszenia zmienione od kursora w kształcie schematu Submission
     * @param entrances Wejścia z oznaczeniem z obchodu w kształcie schematu Entrance
     * @param markers Znaczniki pięter zmienione od kursora
     */
    public PulledChanges {
        submissions = List.copyOf(submissions);
        entrances = List.copyOf(entrances);
        markers = List.copyOf(markers);
    }
}
