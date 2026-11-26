/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port odczytu znaczników do postępu inwentaryzacji.
 *
 * <p>Wymagania: US11.1, US11.3, BR-20</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface ProgressRepository {

    /**
     * Odczytuje znaczniki, których nie cofnięto.
     *
     * @return Znaczniki według budynku, daty utworzenia i identyfikatora
     */
    List<ActiveMarker> activeMarkers();

    /**
     * Dane aktywnego znacznika.
     *
     * @param id Identyfikator znacznika
     * @param place Budynek albo piętro
     * @param created Chwila utworzenia
     * @param author Identyfikator autora
     * @since 0.1.0
     */
    record ActiveMarker(UUID id, MarkerPlace place, Instant created, String author) {
    }
}
