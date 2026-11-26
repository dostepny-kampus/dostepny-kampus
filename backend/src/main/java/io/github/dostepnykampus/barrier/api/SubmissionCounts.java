/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.api;

import java.util.Map;

/**
 * Liczby zgłoszeń przypisanych do budynków.
 *
 * <p>Wymagania: US11.2, BR-21</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface SubmissionCounts {

    /**
     * Liczy zgłoszenia w statusach potrzebnych do postępu inwentaryzacji.
     *
     * @return Liczniki według symbolu, bez budynków bez takich zgłoszeń
     */
    Map<String, Counts> byBuilding();

    /**
     * Zgłoszenia budynku według statusu.
     *
     * @param pending Wysłane i w weryfikacji
     * @param accepted Przyjęte
     * @since 0.1.0
     */
    record Counts(int pending, int accepted) {
    }
}
