/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dostepnykampus.barrier.submission.ReviewDecision;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ciało decyzji Weryfikatora, schemat ReviewDecision w OpenAPI.
 *
 * <p>Wymagania: US10, US10.3, BR-3, BR-14, BR-16</p>
 *
 * @param decision Rodzaj decyzji: ACCEPT albo REJECT
 * @param version Wersja zgłoszenia, którą ocenił Weryfikator
 * @param rejectionReason Powód odrzucenia, wymagany przy REJECT
 * @param duplicateOf Wcześniejsze zgłoszenie przy duplikacie, zapisywane przy REJECT
 * @param barrierType Typ bariery ustalony przy przyjęciu
 * @param severity Uciążliwość ustalona przy przyjęciu
 * @since 0.1.0
 */
record ReviewRequest(
    @Nullable String decision, @Nullable Integer version, @Nullable String rejectionReason,
    @JsonProperty("duplicateOfSubmissionId") @Nullable UUID duplicateOf,
    @Nullable String barrierType,
    @Nullable Integer severity
) {

    /**
     * Największa liczba znaków powodu odrzucenia, równa długości kolumny w bazie.
     */
    private static final int REASON_LIMIT = 500;

    /**
     * Przenosi ciało żądania do decyzji domeny; brak wersji, nieznany rodzaj decyzji,
     * odrzucenie bez powodu i powód dłuższy niż 500 znaków kończą się wyjątkiem
     * {@link InvalidSubmission}; tak samo przyjęcie z samym typem bariery albo samą
     * uciążliwością.
     *
     * @param path Identyfikator zgłoszenia z adresu
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Decyzja Weryfikatora
     */
    ReviewDecision toDecision(final UUID path, final String reviewer) {
        if (this.version == null) {
            throw new InvalidSubmission("Brak wersji zgłoszenia w polu version");
        }
        final ReviewDecision result;
        if ("ACCEPT".equals(this.decision)) {
            result = this.accepted(path, this.version, reviewer);
        } else if ("REJECT".equals(this.decision)) {
            if (this.rejectionReason == null || this.rejectionReason.isBlank()) {
                throw new InvalidSubmission("Odrzucenie wymaga powodu w polu rejectionReason");
            }
            if (this.rejectionReason.length() > ReviewRequest.REASON_LIMIT) {
                throw new InvalidSubmission("Powód odrzucenia ma najwyżej 500 znaków");
            }
            result = new ReviewDecision.Rejected(
                path, this.version, reviewer, this.rejectionReason, this.duplicateOf
            );
        } else {
            throw new InvalidSubmission("Pole decision przyjmuje ACCEPT albo REJECT");
        }
        return result;
    }

    private ReviewDecision accepted(final UUID path, final int expected, final String reviewer) {
        if (this.barrierType == null && this.severity != null) {
            throw new InvalidSubmission("Uciążliwość wymaga też typu w polu barrierType");
        }
        if (this.barrierType != null && this.severity == null) {
            throw new InvalidSubmission("Typ bariery wymaga też uciążliwości w polu severity");
        }
        final ReviewDecision result;
        if (this.barrierType == null || this.severity == null) {
            result = new ReviewDecision.Accepted(path, expected, reviewer);
        } else {
            result = new ReviewDecision.Classified(
                path, expected, reviewer, new BarrierType(this.barrierType), this.severity
            );
        }
        return result;
    }
}
