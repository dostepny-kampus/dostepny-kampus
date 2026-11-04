/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import org.jspecify.annotations.Nullable;

/**
 * Szczegóły bariery zgłoszenia (US7).
 *
 * @param group Grupa bariery albo null, gdy nie podano
 * @param type Typ bariery TB-1…TB-29 albo null, gdy nie podano
 * @param severity Uciążliwość od 1 do 3 albo null, gdy nie podano
 * @param description Opis bariery albo null, gdy nie podano (US7.3)
 * @param removed Czy zgłoszenie dotyczy usuniętej bariery (BR-19)
 * @since 0.1.0
 */
public record BarrierDetails(
    @Nullable BarrierGroup group,
    @Nullable BarrierType type,
    @Nullable Integer severity,
    @Nullable String description,
    boolean removed
) {
}
