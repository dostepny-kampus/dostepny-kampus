/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.application.SubmissionService;
import io.github.dostepnykampus.shared.error.ConflictingState;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weryfikacja zgłoszeń: wzięcie zgłoszenia do weryfikacji i decyzja Weryfikatora.
 * Własnego zgłoszenia Weryfikator nie bierze ani nie rozstrzyga.
 *
 * <p>Wymagania: US10, US10.4, BR-4, BR-11, BR-3, BR-16</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ReviewController {

    /** Opis braku zgłoszenia w odpowiedzi 404. */
    private static final String MISSING = "Nie ma takiego zgłoszenia";

    /** Zapis zgłoszeń. */
    private final SubmissionService service;

    /** Odczyt zgłoszeń. */
    private final SubmissionReading finder;

    ReviewController(final SubmissionService service, final SubmissionReading finder) {
        this.service = service;
        this.finder = finder;
    }

    /**
     * Bierze zgłoszenie „wysłane” do weryfikacji; wzięte już przez tego Weryfikatora
     * zostaje bez zmiany. Własne zgłoszenie daje 403, wzięte przez innego
     * Weryfikatora albo rozstrzygnięte 409.
     *
     * <p>Wymagania: US10.4, BR-4, BR-11</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Zalogowany Weryfikator
     * @return Zgłoszenie w weryfikacji
     */
    @PostMapping("/submissions/{submissionId}/claim")
    SubmissionResponse claimSubmission(
        @PathVariable("submissionId") final UUID id, final Principal account
    ) {
        this.checkNotOwn(id, account);
        return ReviewController.saved(this.service.claim(id, account.getName()));
    }

    /**
     * Przyjmuje albo odrzuca zgłoszenie wzięte przez Weryfikatora. Własne zgłoszenie
     * i wzięte przez innego Weryfikatora albo przez nikogo dają 403, inna wersja
     * niż aktualna albo status po rozstrzygnięciu 409 z wersją serwera.
     *
     * <p>Wymagania: US10, BR-4, BR-11, BR-3, BR-16</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param body Decyzja według schematu ReviewDecision
     * @param account Zalogowany Weryfikator
     * @return Zgłoszenie po decyzji
     */
    @PostMapping("/submissions/{submissionId}/review")
    SubmissionResponse reviewSubmission(
        @PathVariable("submissionId") final UUID id,
        @RequestBody final ReviewRequest body, final Principal account
    ) {
        this.checkNotOwn(id, account);
        if (!this.finder.claimedBy(id, account.getName())) {
            throw new ForbiddenChange("Zgłoszenie nie jest wzięte przez tego Weryfikatora");
        }
        return ReviewController.saved(
            this.service.decide(body.toDecision(id, account.getName()))
        );
    }

    private void checkNotOwn(final UUID id, final Principal account) {
        if (this.finder.find(id).isEmpty()) {
            throw new MissingResource(ReviewController.MISSING);
        }
        if (!this.finder.foreign(id, account.getName())) {
            throw new ForbiddenChange("Własnego zgłoszenia nie weryfikuje jego autor");
        }
    }

    private static SubmissionResponse saved(final EditResult result) {
        return switch (result) {
            case EditResult.Saved saved -> new SubmissionResponse(saved.submission());
            case EditResult.Conflict conflict -> throw new ConflictingState(
                "Na serwerze jest inna wersja zgłoszenia albo jego status nie pozwala na decyzję",
                "serverSubmission",
                new SubmissionResponse(conflict.current())
            );
            case EditResult.Missing _ -> throw new MissingResource(ReviewController.MISSING);
            case null -> throw new IllegalStateException("Brak wyniku decyzji o zgłoszeniu");
        };
    }
}
