/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Komenda utworzenia zgłoszenia, schemat NewSubmission w OpenAPI.
 *
 * <p>Wymagania: BR-9, BR-5</p>
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta
 * @param content Treść zgłoszenia
 * @param author Identyfikator konta z tokenu osoby, która składa zgłoszenie
 * @param roles Role autora z tokenu; niemodyfikowalna kopia
 * @since 0.1.0
 */
public record NewSubmission(
    UUID id, SubmissionContent content, String author, Set<AuthorRole> roles
) {

    /**
     * Komenda z identyfikatorem, treścią, autorem i jego rolami; brak któregokolwiek kończy się
     * niekontrolowanym wyjątkiem NullPointerException.
     */
    public NewSubmission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(author, "author");
        roles = Set.copyOf(roles);
    }
}
