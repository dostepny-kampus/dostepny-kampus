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
     * lista zdjęć jest niemodyfikowalną kopią. Pusta lista zdjęć oraz
     * naruszenie reguł rodzaju (BR-7, BR-8, BR-19, BR-24, US7.3) kończą się
     * niekontrolowanym wyjątkiem {@link InvalidSubmission}; pola spoza rodzaju
     * nie są sprawdzane. Brak rodzaju, miejsca, szczegółów albo listy zdjęć
     * kończy się wyjątkiem NullPointerException.
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
        SubmissionContent.checkRemoval(barrier, replaces);
        if (kind == SubmissionKind.QUICK) {
            SubmissionContent.checkQuick(barrier);
        } else if (kind == SubmissionKind.PRECISE) {
            SubmissionContent.checkPrecise(place, barrier);
        } else {
            SubmissionContent.checkMarking(entranceId, entranceMarking);
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

    private static void checkRemoval(final BarrierDetails barrier, final @Nullable UUID replaces) {
        if (barrier.removed() && replaces == null) {
            throw new InvalidSubmission(
                "„Bariera usunięta” wymaga zgłoszenia w polu replacesSubmissionId (BR-19)"
            );
        }
    }

    private static void checkQuick(final BarrierDetails barrier) {
        if (barrier.group() == null) {
            throw new InvalidSubmission("Zgłoszenie szybkie wymaga grupy barier (BR-8)");
        }
    }

    private static void checkPrecise(final SubmissionPlace place, final BarrierDetails barrier) {
        if (barrier.type() == null) {
            throw new InvalidSubmission("Pomiar precyzyjny wymaga typu bariery (BR-7)");
        }
        if (barrier.severity() == null && !barrier.removed()) {
            throw new InvalidSubmission("Pomiar precyzyjny wymaga uciążliwości (BR-7)");
        }
        if (place.buildingSymbol() != null
            && (place.floor() == null || !place.floor().identified())) {
            throw new InvalidSubmission("Pomiar precyzyjny w budynku wymaga piętra (BR-7)");
        }
        if (barrier.type().other()
            && (barrier.description() == null || barrier.description().isBlank())) {
            throw new InvalidSubmission("Typ TB-29 „inna bariera” wymaga opisu (US7.3)");
        }
    }

    private static void checkMarking(
        final @Nullable String entranceId, final @Nullable NewEntranceMarking marking
    ) {
        if (entranceId == null || marking == null) {
            throw new InvalidSubmission(
                "Zmiana oznaczenia wejścia wymaga entranceId i entranceMarking (BR-24)"
            );
        }
    }
}
