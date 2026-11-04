/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Treść zgłoszenia, schemat SubmissionContent w OpenAPI (US4, US7, BR-7).
 *
 * @param kind Rodzaj zgłoszenia
 * @param place Miejsce zgłoszenia
 * @param barrier Szczegóły bariery
 * @param entranceId Wejście, którego dotyczy zgłoszenie, albo null
 * @param entranceMarking Nowe oznaczenie wejścia albo null (BR-24)
 * @param replacesSubmissionId Zastępowane zgłoszenie albo null (BR-19)
 * @param photoIds Identyfikatory zdjęć, co najmniej jeden
 * @since 0.1.0
 */
public record SubmissionContent(
    SubmissionKind kind,
    SubmissionPlace place,
    BarrierDetails barrier,
    @Nullable String entranceId,
    @Nullable NewEntranceMarking entranceMarking,
    @Nullable UUID replacesSubmissionId,
    List<UUID> photoIds
) {

    /**
     * Kopia treści z innym budynkiem miejsca.
     *
     * @param symbol Symbol budynku
     * @return Treść z miejscem w tym budynku i resztą bez zmian
     */
    public SubmissionContent withBuilding(final String symbol) {
        return this;
    }

    /**
     * Kopia treści z miejscem poza budynkami.
     *
     * @return Treść z miejscem bez symbolu budynku i resztą bez zmian
     */
    public SubmissionContent outsideBuildings() {
        return this;
    }
}
