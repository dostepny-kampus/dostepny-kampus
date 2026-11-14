/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.util.Objects;
import java.util.UUID;

/**
 * Komenda edycji zgłoszenia, schemat SubmissionUpdate w OpenAPI.
 *
 * <p>Wymagania: BR-10</p>
 *
 * @param id Identyfikator edytowanego zgłoszenia
 * @param expectedVersion Wersja, na której oparto edycję, co najmniej 1
 * @param content Nowa treść zgłoszenia
 * @since 0.1.0
 */
public record SubmissionEdit(UUID id, int expectedVersion, SubmissionContent content) {

    /**
     * Komenda z identyfikatorem, wersją bazową i treścią. Wersja bazowa poniżej 1
     * kończy się niekontrolowanym wyjątkiem {@link InvalidSubmission}; brak
     * identyfikatora albo treści kończy się wyjątkiem NullPointerException.
     */
    public SubmissionEdit {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(content, "content");
        if (expectedVersion < 1) {
            throw new InvalidSubmission("Brak wersji bazowej edycji w polu version");
        }
    }
}
