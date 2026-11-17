/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Odpowiedź ze stroną listy zgłoszeń: zgłoszenia w treści, a gdy jest dalsza strona,
 * nagłówek {@code Link} z {@code rel="next"} (RFC 8288) pod bieżący adres z kursorem
 * {@code after} ostatniego zgłoszenia.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class SubmissionPageResponse {

    /**
     * Strona listy.
     */
    private final SubmissionFinder.Page page;

    /**
     * Ctor.
     *
     * @param page Strona listy
     */
    SubmissionPageResponse(final SubmissionFinder.Page page) {
        this.page = page;
    }

    /**
     * Odpowiedź HTTP 200 ze zgłoszeniami strony i, gdy trzeba, nagłówkiem następnej strony.
     *
     * @return Odpowiedź dla bieżącego żądania
     */
    ResponseEntity<List<SubmissionResponse>> entity() {
        final ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (this.page.more()) {
            response.header(
                HttpHeaders.LINK,
                String.format(
                    "<%s>; rel=\"next\"",
                    ServletUriComponentsBuilder.fromCurrentRequest()
                        .replaceQueryParam("after", this.page.last()).build().toUriString()
                )
            );
        }
        return response.body(this.page.items().stream().map(SubmissionResponse::new).toList());
    }
}
