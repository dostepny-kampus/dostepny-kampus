/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * Zgłoszenia barier: wysłanie nowego zgłoszenia (US4, US7, BR-9) i odczyt
 * zgłoszeń przyjętych (BR-6).
 *
 * <p>Logowanie przez Keycloak dochodzi w sprincie 5. Do tego czasu serwer
 * przyjmuje zgłoszenia bez tokenu i bez autora, a pokazuje tylko zgłoszenia
 * przyjęte, jak osobie niezalogowanej.</p>
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
     * Zwraca zgłoszenia przyjęte według filtrów (US1.3, US2.2, BR-6).
     *
     * @param filter Filtry z parametrów zapytania
     * @return Zgłoszenia przyjęte od najstarszego
     */
    @GetMapping("/submissions")
    List<JsonNode> listSubmissions(final SubmissionFilter filter) {
        if (Boolean.TRUE.equals(filter.mine())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Własne zgłoszenia wymagają logowania"
            );
        }
        if (filter.status() != null && !List.of("ACCEPTED").containsAll(filter.status())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Bez mine=true dozwolony jest tylko status ACCEPTED"
            );
        }
        return this.store.accepted(filter.buildingSymbol(), filter.floorId());
    }

    /**
     * Zwraca zgłoszenie przyjęte; pozostałe zobaczy po logowaniu autor
     * i Weryfikator (BR-5, BR-6).
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie przyjęte
     */
    @GetMapping("/submissions/{submissionId}")
    JsonNode getSubmission(@PathVariable("submissionId") final UUID id) {
        return this.store.accepted(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );
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
