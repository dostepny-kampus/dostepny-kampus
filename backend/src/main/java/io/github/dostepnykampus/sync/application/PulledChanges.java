/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import java.time.Instant;
import java.util.List;

/**
 * Zmiany od kursora i kursor następnego pobrania.
 *
 * <p>Wymagania: US15.1, US15.2</p>
 *
 * @param cursor Chwila najpóźniejszej zwróconej zmiany, bez zmian kursor z żądania
 * @param markers Znaczniki pięter zmienione od kursora
 * @since 0.1.0
 */
public record PulledChanges(Instant cursor, List<PulledMarker> markers) {

    /**
     * Kopiuje listę, żeby rekord był niezmienny.
     *
     * @param cursor Chwila najpóźniejszej zwróconej zmiany, bez zmian kursor z żądania
     * @param markers Znaczniki pięter zmienione od kursora
     */
    public PulledChanges {
        markers = List.copyOf(markers);
    }
}
