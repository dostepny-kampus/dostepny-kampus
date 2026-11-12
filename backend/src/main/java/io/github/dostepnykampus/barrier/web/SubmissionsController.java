/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.ConflictingState;
import io.github.dostepnykampus.ForbiddenChange;
import io.github.dostepnykampus.MissingLogin;
import io.github.dostepnykampus.MissingResource;
import io.github.dostepnykampus.barrier.SubmissionService;
import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.Reader;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zgłoszenia barier: wysłanie nowego zgłoszenia (US4, US7, BR-9), edycja
 * z kontrolą wersji (US8.3, BR-10), odczyt zgłoszeń przyjętych (BR-6),
 * odczyt zgłoszenia nieprzyjętego przez autora i Weryfikatora (BR-5)
 * i podobne zgłoszenia dla Weryfikatora (US10.1, BR-13).
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
     * Zwraca zgłoszenia przyjęte według filtrów (US1.3, US2.2, BR-6), a z
     * mine=true własne zgłoszenia zalogowanej osoby w każdym statusie (US8.1).
     *
     * @param filter Filtry z parametrów zapytania
     * @param account Zalogowane konto albo null bez logowania
     * @return Zgłoszenia od najstarszego
     */
    @GetMapping("/submissions")
    List<SubmissionResponse> listSubmissions(
        final SubmissionFilter filter, @Nullable final Principal account
    ) {
        final SubmissionQuery query = filter.toQuery();
        final List<Submission> found;
        if (filter.mine()) {
            found = this.finder.own(SubmissionsController.name(account), query);
        } else {
            found = this.finder.list(query);
        }
        return found.stream().map(SubmissionResponse::new).toList();
    }

    /**
     * Zwraca zgłoszenie przyjęte każdemu, a pozostałe po logowaniu autorowi
     * i Weryfikatorowi (BR-5, BR-6). Kto zgłoszenia nie widzi, dostaje 404
     * jak przy jego braku.
     *
     * @param id Identyfikator zgłoszenia
     * @param request Wejściowe żądanie z kontem i rolą czytelnika
     * @return Zgłoszenie widoczne dla czytelnika
     */
    @GetMapping("/submissions/{submissionId}")
    SubmissionResponse getSubmission(
        @PathVariable("submissionId") final UUID id, final HttpServletRequest request
    ) {
        return this.finder.visible(
            id,
            new Reader(
                Optional.ofNullable(request.getUserPrincipal()).map(Principal::getName),
                request.isUserInRole("VERIFIER")
            )
        ).map(SubmissionResponse::new)
            .orElseThrow(() -> new MissingResource(SubmissionsController.MISSING));
    }

    /**
     * Zwraca zgłoszenia podobne do danego w promieniu 10 m, z każdego piętra
     * (US10.1, BR-13). Sprawdzane zgłoszenie może mieć dowolny status.
     * Wymóg roli Weryfikatora dochodzi z logowaniem w sprincie 5.
     *
     * @param id Identyfikator sprawdzanego zgłoszenia
     * @return Podobne zgłoszenia od najbliższego
     */
    @GetMapping("/submissions/{submissionId}/similar")
    List<SimilarSubmissionResponse> listSimilarSubmissions(
        @PathVariable("submissionId") final UUID id
    ) {
        if (this.finder.find(id).isEmpty()) {
            throw new MissingResource(SubmissionsController.MISSING);
        }
        return this.finder.similar(id).stream().map(SimilarSubmissionResponse::new).toList();
    }

    /**
     * Zapisuje nowe zgłoszenie. Ponowne wysłanie tego samego id zwraca
     * zapisane zgłoszenie i nie tworzy drugiego (BR-9). Autorem zostaje konto
     * z tokenu (BR-3).
     *
     * @param body Zgłoszenie według schematu NewSubmission
     * @param account Zalogowane konto; jego nazwa to identyfikator z tokenu
     * @return Nowe zgłoszenie z kodem 201 albo zapisane wcześniej z kodem 200
     */
    @PostMapping("/submissions")
    ResponseEntity<SubmissionResponse> createSubmission(
        @RequestBody final NewSubmissionRequest body, final Principal account
    ) {
        final CreateResult result = this.service.create(body.toCommand(account.getName()));
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
     * od której zaczęto edycję (US8.3, US9.3, BR-10). Cudzego zgłoszenia
     * nie zmienia nikt poza autorem (BR-3).
     *
     * @param id Identyfikator zgłoszenia
     * @param body Edycja według schematu SubmissionUpdate
     * @param account Zalogowane konto; jego nazwa to identyfikator z tokenu
     * @return Zgłoszenie po edycji z kolejną wersją
     */
    @PutMapping("/submissions/{submissionId}")
    SubmissionResponse updateSubmission(
        @PathVariable("submissionId") final UUID id,
        @RequestBody final SubmissionUpdateRequest body, final Principal account
    ) {
        if (this.finder.foreign(id, account.getName())) {
            throw new ForbiddenChange("Zgłoszenie zmienia tylko jego autor");
        }
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

    private static String name(@Nullable final Principal account) {
        if (account == null) {
            throw new MissingLogin("Własne zgłoszenia wymagają logowania");
        }
        return account.getName();
    }
}
