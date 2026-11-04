/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.ConflictingState;
import io.github.dostepnykampus.MissingResource;
import io.github.dostepnykampus.barrier.SubmissionService;
import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Zgłoszenia barier: wysłanie nowego zgłoszenia (US4, US7, BR-9), edycja
 * z kontrolą wersji (US8.3, BR-10) i odczyt zgłoszeń przyjętych (BR-6).
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
     * Opis braku zgłoszenia w odpowiedzi 404.
     */
    private static final String MISSING = "Nie ma takiego zgłoszenia";

    /**
     * Zapis zgłoszeń.
     */
    private final SubmissionService service;

    /**
     * Odczyt zgłoszeń.
     */
    private final SubmissionFinder finder;

    SubmissionsController(final SubmissionService service, final SubmissionFinder finder) {
        this.service = service;
        this.finder = finder;
    }

    /**
     * Zwraca zgłoszenia przyjęte według filtrów (US1.3, US2.2, BR-6).
     *
     * @param filter Filtry z parametrów zapytania
     * @return Zgłoszenia przyjęte od najstarszego
     */
    @GetMapping("/submissions")
    List<SubmissionResponse> listSubmissions(final SubmissionFilter filter) {
        if (filter.mine()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Własne zgłoszenia wymagają logowania"
            );
        }
        return this.finder.list(filter.toQuery()).stream().map(SubmissionResponse::new).toList();
    }

    /**
     * Zwraca zgłoszenie przyjęte; pozostałe zobaczy po logowaniu autor
     * i Weryfikator (BR-5, BR-6).
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie przyjęte
     */
    @GetMapping("/submissions/{submissionId}")
    SubmissionResponse getSubmission(@PathVariable("submissionId") final UUID id) {
        return this.finder.find(id)
            .filter(submission -> submission.status().publiclyVisible())
            .map(SubmissionResponse::new)
            .orElseThrow(() -> new MissingResource(SubmissionsController.MISSING));
    }

    /**
     * Zapisuje nowe zgłoszenie. Ponowne wysłanie tego samego id zwraca
     * zapisane zgłoszenie i nie tworzy drugiego (BR-9).
     *
     * @param body Zgłoszenie według schematu NewSubmission
     * @return Nowe zgłoszenie z kodem 201 albo zapisane wcześniej z kodem 200
     */
    @PostMapping("/submissions")
    ResponseEntity<SubmissionResponse> createSubmission(
        @RequestBody final NewSubmissionRequest body
    ) {
        final CreateResult result = this.service.create(body.toCommand());
        final HttpStatus status;
        if (result instanceof CreateResult.Created) {
            status = HttpStatus.CREATED;
        } else {
            status = HttpStatus.OK;
        }
        return ResponseEntity.status(status).body(new SubmissionResponse(result.submission()));
    }

    /**
     * Zapisuje edycję zgłoszenia, jeśli na serwerze jest ta sama wersja,
     * od której zaczęto edycję (US8.3, US9.3, BR-10).
     *
     * @param id Identyfikator zgłoszenia
     * @param body Edycja według schematu SubmissionUpdate
     * @return Zgłoszenie po edycji z kolejną wersją
     */
    @PutMapping("/submissions/{submissionId}")
    SubmissionResponse updateSubmission(
        @PathVariable("submissionId") final UUID id,
        @RequestBody final SubmissionUpdateRequest body
    ) {
        return switch (this.service.update(body.toCommand(id))) {
            case EditResult.Saved saved -> new SubmissionResponse(saved.submission());
            case EditResult.Conflict conflict -> throw new ConflictingState(
                "Na serwerze jest inna wersja zgłoszenia albo jego status nie pozwala na zmianę",
                "serverSubmission",
                new SubmissionResponse(conflict.current())
            );
            case EditResult.Missing _ -> throw new MissingResource(SubmissionsController.MISSING);
            case null -> throw new IllegalStateException("Brak wyniku edycji zgłoszenia");
        };
    }
}
