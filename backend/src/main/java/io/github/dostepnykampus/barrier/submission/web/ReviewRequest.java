/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dostepnykampus.barrier.submission.ReviewDecision;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ciało decyzji Weryfikatora, schemat ReviewDecision w OpenAPI.
 *
 * <p>Wymagania: US10, BR-3, BR-16</p>
 *
 * @param decision Rodzaj decyzji: ACCEPT albo REJECT
 * @param version Wersja zgłoszenia, którą ocenił Weryfikator
 * @param rejectionReason Powód odrzucenia, wymagany przy REJECT
 * @param duplicateOf Wcześniejsze zgłoszenie przy duplikacie; jeszcze nie zapisywane
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
     * Przenosi ciało żądania do decyzji domeny; brak wersji, nieznany rodzaj decyzji
     * i odrzucenie bez powodu kończą się wyjątkiem {@link InvalidSubmission}.
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
            result = new ReviewDecision(
                path, this.version, reviewer, SubmissionStatus.ACCEPTED, ""
            );
        } else if ("REJECT".equals(this.decision)) {
            if (this.rejectionReason == null || this.rejectionReason.isBlank()) {
                throw new InvalidSubmission("Odrzucenie wymaga powodu w polu rejectionReason");
            }
            result = new ReviewDecision(
                path, this.version, reviewer, SubmissionStatus.REJECTED,
                this.rejectionReason
            );
        } else {
            throw new InvalidSubmission("Pole decision przyjmuje ACCEPT albo REJECT");
        }
        return result;
    }
}
