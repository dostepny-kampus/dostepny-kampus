/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import io.github.dostepnykampus.shared.json.RawJson;
import java.time.Instant;
import java.util.List;

/**
 * Wejścia z oznaczeniem zmienionym na obchodzie od kursora, do pobrania przez telefon.
 *
 * <p>Wymagania: US17.2</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EntranceChanges {

    /**
     * Wejścia, których oznaczenie zmieniło przyjęte zgłoszenie z obchodu od {@code since}
     * włącznie, w kształcie schematu Entrance kontraktu.
     *
     * @param since Chwila, od której liczą się zmiany
     * @return Wejścia od najdawniej zmienionego
     */
    List<RawJson> markedSince(Instant since);
}
