/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.content.EntrancePlaces;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.json.SubmissionContentJson;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Ciało PUT /submissions/{id}, schemat SubmissionUpdate w OpenAPI.
 *
 * @param version Wersja, na której oparto edycję, albo null, gdy nie podano
 * @param content Treść zgłoszenia, w JSON płasko obok wersji
 * @since 0.1.0
 */
record SubmissionUpdateRequest(
    @Nullable Integer version, @JsonUnwrapped SubmissionContentJson content
) {

    /**
     * Przenosi ciało żądania do komendy domeny.
     *
     * @param path Identyfikator zgłoszenia z adresu
     * @param entrances Miejsca wejść znanych serwerowi
     * @return Komenda edycji zgłoszenia
     */
    SubmissionEdit toCommand(final UUID path, final EntrancePlaces entrances) {
        if (this.version == null) {
            throw new InvalidSubmission("Brak wersji bazowej edycji w polu version");
        }
        return new SubmissionEdit(path, this.version, this.content.toContent(entrances));
    }
}
