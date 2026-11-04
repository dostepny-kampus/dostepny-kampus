/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ciało PUT /submissions/{id}, schemat SubmissionUpdate w OpenAPI.
 *
 * @param version Wersja, na której oparto edycję, albo null, gdy nie podano
 * @param content Treść zgłoszenia, w JSON płasko obok wersji
 * @since 0.1.0
 */
record SubmissionUpdateRequest(@Nullable Integer version, SubmissionContentJson content) {

    /**
     * Przenosi ciało żądania do komendy domeny.
     *
     * @param path Identyfikator zgłoszenia z adresu
     * @return Komenda edycji zgłoszenia
     */
    SubmissionEdit toCommand(final UUID path) {
        throw new UnsupportedOperationException();
    }
}
