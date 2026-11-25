/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.entrance.StaleEntranceMarking;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.ReviewDecision;
import io.github.dostepnykampus.barrier.submission.StoredPhotos;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReview;
import io.github.dostepnykampus.barrier.submission.application.SubmissionService;
import io.github.dostepnykampus.shared.error.ConflictingState;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weryfikacja zgłoszeń: kolejka weryfikacji, wzięcie zgłoszenia do weryfikacji
 * i decyzja Weryfikatora. Własnego zgłoszenia Weryfikator nie widzi w kolejce,
 * nie bierze ani nie rozstrzyga.
 *
 * <p>Wymagania: US10, US10.4, US10.5, US10.7, BR-4, BR-11, BR-3, BR-16, BR-7, BR-8</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ReviewController {

    /** Opis braku zgłoszenia w odpowiedzi 404. */
    private static final String MISSING = "Nie ma takiego zgłoszenia";

    /** Zapis zgłoszeń. */
    private final SubmissionService service;

    /** Zapis i zastosowanie decyzji. */
    private final SubmissionReview review;

    /** Odczyt zgłoszeń. */
    private final SubmissionReading finder;

    /** Kontrola kompletu zdjęć przed wzięciem i decyzją. */
    private final StoredPhotos photos;

    ReviewController(
        final SubmissionService service, final SubmissionReview review,
        final SubmissionReading finder, final StoredPhotos photos
    ) {
        this.service = service;
        this.review = review;
        this.finder = finder;
        this.photos = photos;
    }

    /**
     * Zwraca kolejkę zgłoszeń do weryfikacji przez zalogowanego Weryfikatora,
     * od najdawniej wysłanego: zgłoszenia „wysłane” innych osób oraz „w weryfikacji”
     * wzięte przez tego Weryfikatora, z kompletem zdjęć w magazynie.
     *
     * <p>Wymagania: US10.4, US10.5, US10.7, BR-4, BR-7, BR-11</p>
     *
     * @param account Zalogowany Weryfikator
     * @return Zgłoszenia w kolejce od najstarszego
     */
    @GetMapping("/review/queue")
    List<SubmissionResponse> listReviewQueue(final Principal account) {
        return this.finder.reviewQueue(account.getName()).stream()
            .filter(this.photos::complete)
            .map(SubmissionResponse::new)
            .toList();
    }

    /**
     * Bierze zgłoszenie „wysłane” do weryfikacji; wzięte już przez tego Weryfikatora
     * zostaje bez zmiany. Własne zgłoszenie daje 403, wzięte przez innego
     * Weryfikatora, rozstrzygnięte albo bez wszystkich zdjęć w magazynie 409.
     *
     * <p>Wymagania: US10.4, BR-4, BR-11, BR-7</p>
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
        this.checkPhotos(id);
        return ReviewController.saved(this.service.claim(id, account.getName()));
    }

    /**
     * Przyjmuje albo odrzuca zgłoszenie wzięte przez Weryfikatora. Własne zgłoszenie
     * i wzięte przez innego Weryfikatora albo przez nikogo dają 403, inna wersja
     * niż aktualna, status po rozstrzygnięciu albo brak zdjęcia w magazynie 409 z wersją
     * serwera.
     *
     * <p>Wymagania: US10, US10.3, BR-4, BR-11, BR-3, BR-14, BR-16, BR-7</p>
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
        this.checkPhotos(id);
        final ReviewDecision decision = body.toDecision(id, account.getName());
        final Submission current = this.finder.find(id).orElseThrow();
        decision.admit(current.content().kind());
        try {
            return ReviewController.saved(this.review.decide(decision));
        } catch (final StaleEntranceMarking stale) {
            final ConflictingState conflict = new ConflictingState(
                Objects.requireNonNull(stale.getMessage()), "serverSubmission",
                new SubmissionResponse(current)
            );
            conflict.initCause(stale);
            throw conflict;
        }
    }

    private void checkNotOwn(final UUID id, final Principal account) {
        if (this.finder.find(id).isEmpty()) {
            throw new MissingResource(ReviewController.MISSING);
        }
        if (!this.finder.foreign(id, account.getName())) {
            throw new ForbiddenChange("Własnego zgłoszenia nie weryfikuje jego autor");
        }
    }

    private void checkPhotos(final UUID id) {
        final Submission current = this.finder.find(id).orElseThrow();
        if (!this.photos.complete(current)) {
            throw new ConflictingState(
                "Serwer nie ma jeszcze wszystkich zdjęć zgłoszenia",
                "serverSubmission", new SubmissionResponse(current)
            );
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
