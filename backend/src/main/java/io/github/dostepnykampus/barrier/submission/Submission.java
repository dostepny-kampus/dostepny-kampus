/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Zgłoszenie zapisane na serwerze, schemat Submission w OpenAPI.
 *
 * <p>Wymagania: US8, BR-3</p>
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta
 * @param version Wersja zgłoszenia, co najmniej 1
 * @param status Status zgłoszenia
 * @param createdAt Chwila utworzenia zgłoszenia
 * @param content Treść zgłoszenia
 * @param verifiedAt Chwila przyjęcia albo null, gdy jest nieznana
 * @since 0.1.0
 */
public record Submission(
    UUID id, int version, SubmissionStatus status, Instant createdAt,
    SubmissionContent content, @Nullable Instant verifiedAt
) {

    /**
     * Zgłoszenie z wersją co najmniej 1 i kompletem referencji. Wersja poniżej 1
     * to uszkodzone dane serwera, więc kończy się niekontrolowanym wyjątkiem
     * IllegalArgumentException, a nie błędem klienta; brak którejkolwiek referencji
     * kończy się wyjątkiem NullPointerException.
     */
    public Submission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(content, "content");
        if (version < 1) {
            throw new IllegalArgumentException(
                String.format("Wersja zgłoszenia %d jest mniejsza niż 1", version)
            );
        }
    }

    /**
     * Zgłoszenie bez zapisanej daty przyjęcia.
     *
     * @param id Identyfikator
     * @param version Wersja
     * @param status Status
     * @param createdAt Chwila utworzenia
     * @param content Treść
     */
    public Submission(
        final UUID id, final int version, final SubmissionStatus status,
        final Instant createdAt, final SubmissionContent content
    ) {
        this(id, version, status, createdAt, content, null);
    }

    /**
     * Czy Weryfikator przyjął zgłoszenie.
     *
     * <p>Wymagania: BR-6, BR-18</p>
     *
     * @return Prawda dla statusu ACCEPTED
     */
    public boolean accepted() {
        return this.status == SubmissionStatus.ACCEPTED;
    }
}
