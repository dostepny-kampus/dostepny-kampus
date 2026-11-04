/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.Objects;
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
 * @param replaces Zastępowane zgłoszenie albo null (BR-19)
 * @param photoIds Identyfikatory zdjęć, co najmniej jeden
 * @since 0.1.0
 */
public record SubmissionContent(
    SubmissionKind kind,
    SubmissionPlace place,
    BarrierDetails barrier,
    @Nullable String entranceId,
    @Nullable NewEntranceMarking entranceMarking,
    @Nullable UUID replaces,
    List<UUID> photoIds
) {

    /**
     * Treść z rodzajem, miejscem, szczegółami i co najmniej jednym zdjęciem;
     * lista zdjęć jest niemodyfikowalną kopią. Pusta lista zdjęć kończy się
     * niekontrolowanym wyjątkiem {@link InvalidSubmission}; brak rodzaju,
     * miejsca, szczegółów albo listy zdjęć kończy się wyjątkiem
     * NullPointerException.
     */
    public SubmissionContent {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(place, "place");
        Objects.requireNonNull(barrier, "barrier");
        Objects.requireNonNull(photoIds, "photoIds");
        if (photoIds.isEmpty()) {
            throw new InvalidSubmission(
                "Zgłoszenie wymaga co najmniej jednego UUID zdjęcia w polu photoIds"
            );
        }
        photoIds = List.copyOf(photoIds);
    }

    /**
     * Kopia treści z innym budynkiem miejsca.
     *
     * @param symbol Symbol budynku
     * @return Treść z miejscem w tym budynku i resztą bez zmian
     */
    public SubmissionContent withBuilding(final String symbol) {
        return this.withPlace(this.place.withBuilding(symbol));
    }

    /**
     * Kopia treści z miejscem poza budynkami.
     *
     * @return Treść z miejscem bez symbolu budynku i resztą bez zmian
     */
    public SubmissionContent outsideBuildings() {
        return this.withPlace(this.place.outsideBuildings());
    }

    private SubmissionContent withPlace(final SubmissionPlace other) {
        return new SubmissionContent(
            this.kind, other, this.barrier, this.entranceId, this.entranceMarking,
            this.replaces, this.photoIds
        );
    }
}
