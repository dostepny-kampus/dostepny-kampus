/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review;

import io.github.dostepnykampus.barrier.submission.AuthorRole;
import java.util.List;
import java.util.Objects;

/**
 * Autor zgłoszenia znany serwerowi: konto i role z chwili wysłania.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @param account Identyfikator konta autora
 * @param roles Role autora w kolejności kontraktu; pusta lista, gdy serwer ich nie zapisał
 * @since 0.1.0
 */
public record SubmissionAuthor(String account, List<AuthorRole> roles) {

    /**
     * Autor z kontem i niemodyfikowalną kopią ról; brak któregokolwiek kończy się
     * wyjątkiem NullPointerException.
     */
    public SubmissionAuthor {
        Objects.requireNonNull(account, "account");
        roles = List.copyOf(roles);
    }
}
