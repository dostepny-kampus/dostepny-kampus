/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import org.jspecify.annotations.Nullable;

/**
 * Szczegóły bariery zgłoszenia.
 *
 * <p>Uciążliwość poza zakresem 1–3 albo opis dłuższy niż 2000 znaków
 * kończy się niekontrolowanym wyjątkiem {@link InvalidSubmission}; brak
 * wartości (null) jest poprawny.</p>
 *
 * <p>Wymagania: US7, US7.3, BR-19</p>
 *
 * @param group Grupa bariery albo null, gdy nie podano
 * @param type Typ bariery TB-1…TB-29 albo null, gdy nie podano
 * @param severity Uciążliwość od 1 do 3 albo null, gdy nie podano
 * @param description Opis bariery albo null, gdy nie podano
 * @param removed Czy zgłoszenie dotyczy usuniętej bariery
 * @since 0.1.0
 */
public record BarrierDetails(
    @Nullable BarrierGroup group,
    @Nullable BarrierType type,
    @Nullable Integer severity,
    @Nullable String description,
    boolean removed
) {

    /**
     * Szczegóły z uciążliwością 1–3 i opisem do 2000 znaków albo bez nich.
     */
    public BarrierDetails {
        if (severity != null && (severity < 1 || severity > 3)) {
            throw new InvalidSubmission("Uciążliwość spoza zakresu 1–3");
        }
        if (description != null && description.length() > 2000) {
            throw new InvalidSubmission("Opis dłuższy niż 2000 znaków");
        }
    }
}
