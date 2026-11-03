/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * Zgłoszenia barier: wysłanie nowego zgłoszenia (US4, US7, BR-9).
 *
 * <p>Logowanie przez Keycloak dochodzi w sprincie 5, do tego czasu serwer
 * przyjmuje zgłoszenia bez tokenu i bez autora.</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SubmissionsController {

    /**
     * Zapis i odczyt zgłoszeń.
     */
    private final SubmissionStore store;

    SubmissionsController(final SubmissionStore store) {
        this.store = store;
    }

    /**
     * Zapisuje nowe zgłoszenie. Ponowne wysłanie tego samego id zwraca
     * zapisane zgłoszenie i nie tworzy drugiego (BR-9).
     *
     * @param body Zgłoszenie według schematu NewSubmission
     * @return Nowe zgłoszenie z kodem 201 albo zapisane wcześniej z kodem 200
     */
    @PostMapping("/submissions")
    ResponseEntity<JsonNode> createSubmission(@RequestBody final JsonNode body) {
        final SubmissionStore.Saved saved = this.store.create(SubmissionDraft.of(body));
        final HttpStatus status;
        if (saved.created()) {
            status = HttpStatus.CREATED;
        } else {
            status = HttpStatus.OK;
        }
        return ResponseEntity.status(status).body(saved.submission());
    }
}
