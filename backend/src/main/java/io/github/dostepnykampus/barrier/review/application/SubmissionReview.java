/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.application;

import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.StoredPhotos;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.building.api.EntranceMarkings;
import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wzięcie zgłoszenia do weryfikacji i zapis decyzji Weryfikatora z regułami (własne zgłoszenie,
 * wzięcie przez tego Weryfikatora, komplet zdjęć, decyzja zgodna z rodzajem) i ze skutkami
 * przyjęcia: archiwizacją zastąpionego zgłoszenia, zgłoszenia „bariera usunięta”
 * i zastosowanej zmiany oznaczenia wejścia. Każda operacja to jedna transakcja.
 *
 * <p>Wymagania: US10.4, US10.6, US17.2, US17.3, BR-4, BR-7, BR-11, BR-18, BR-19, BR-24</p>
 *
 * @since 0.1.0
 */
@Service
public class SubmissionReview {

    /** Wzięcie zgłoszenia i zapis decyzji. */
    private final ReviewRepository reviews;

    /** Blokada i archiwizacja zgłoszeń. */
    private final SubmissionRepository submissions;

    /** Odczyt zgłoszenia i jego autora. */
    private final SubmissionFinder finder;

    /** Kontrola kompletu zdjęć w magazynie. */
    private final StoredPhotos photos;

    /** Zastosowanie oznaczenia w danych budynku. */
    private final EntranceMarkings entrances;

    /** Lokalizacja wejścia, gdy zgłoszenie nie wskazuje budynku. */
    private final KnownBuildings buildings;

    SubmissionReview(
        final ReviewRepository reviews, final SubmissionRepository submissions,
        final SubmissionFinder finder, final StoredPhotos photos,
        final EntranceMarkings entrances, final KnownBuildings buildings
    ) {
        this.reviews = reviews;
        this.submissions = submissions;
        this.finder = finder;
        this.photos = photos;
        this.entrances = entrances;
        this.buildings = buildings;
    }

    /**
     * Daje cudze zgłoszenie z kompletem zdjęć Weryfikatorowi. Brak zgłoszenia kończy się
     * wyjątkiem MissingResource, własne ForbiddenChange, brak zdjęcia ReviewConflict.
     *
     * <p>Wymagania: BR-4, BR-7, BR-11, US10.4</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Wynik wzięcia zgłoszenia
     */
    @Transactional
    public EditResult claim(final UUID id, final String reviewer) {
        this.complete(this.foreign(id, reviewer));
        return this.reviews.claim(id, reviewer);
    }

    /**
     * Zapisuje decyzję razem ze skutkami przyjęcia. Brak zgłoszenia kończy się wyjątkiem
     * MissingResource, własne albo niewzięte przez tego Weryfikatora ForbiddenChange, brak
     * zdjęcia, przyjęcie zmiany zgłoszenia, które nie jest już przyjęte, i nieaktualna zmiana
     * oznaczenia wejścia ReviewConflict, decyzja niezgodna z rodzajem InvalidSubmission.
     * Przyjęte „bariera usunięta” i zmiana oznaczenia stają się archiwalne.
     *
     * <p>Wymagania: BR-4, BR-7, BR-11, BR-14, BR-18, BR-19, BR-23, BR-24</p>
     *
     * @param decision Decyzja Weryfikatora
     * @return Wynik zapisu decyzji; przy przyjęciu zgłoszenie po jego skutkach
     */
    @Transactional
    public EditResult decide(final ReviewDecision decision) {
        final Submission current = this.foreign(decision.id(), decision.reviewer());
        if (!this.reviews.claimedBy(decision.id(), decision.reviewer())) {
            throw new ForbiddenChange("Zgłoszenie nie jest wzięte przez tego Weryfikatora");
        }
        this.complete(current);
        decision.admit(current.content().kind());
        if (decision.outcome() == SubmissionStatus.ACCEPTED) {
            this.replaceable(current);
        }
        try {
            return this.applied(this.reviews.decide(decision));
        } catch (final StaleEntranceMarking stale) {
            final ReviewConflict conflict = new ReviewConflict(
                Objects.requireNonNull(stale.getMessage()), current
            );
            conflict.initCause(stale);
            throw conflict;
        }
    }

    private Submission foreign(final UUID id, final String reviewer) {
        this.submissions.lock(id);
        final Submission current = this.finder.find(id)
            .orElseThrow(() -> new MissingResource("Nie ma takiego zgłoszenia"));
        if (!this.finder.foreign(id, reviewer)) {
            throw new ForbiddenChange("Własnego zgłoszenia nie weryfikuje jego autor");
        }
        return current;
    }

    private void replaceable(final Submission current) {
        final UUID replaced = current.content().replaces();
        if (replaced != null) {
            this.submissions.lock(replaced);
            if (!this.finder.find(replaced).map(Submission::accepted).orElse(false)) {
                throw new ReviewConflict(
                    "Zgłoszenie, które zmienia ta zmiana, nie jest już przyjęte (BR-23)", current
                );
            }
        }
    }

    private void complete(final Submission current) {
        if (!this.photos.complete(current)) {
            throw new ReviewConflict("Serwer nie ma jeszcze wszystkich zdjęć zgłoszenia", current);
        }
    }

    private EditResult applied(final EditResult result) {
        EditResult applied = result;
        if (result instanceof EditResult.Saved saved && saved.submission().accepted()) {
            applied = new EditResult.Saved(this.accepted(saved.submission()));
        }
        return applied;
    }

    private Submission accepted(final Submission submission) {
        final SubmissionContent content = submission.content();
        final UUID replaced = content.replaces();
        if (replaced != null) {
            this.submissions.archive(replaced);
        }
        final boolean marking = content.kind() == SubmissionKind.ENTRANCE_MARKING;
        if (marking) {
            this.apply(submission);
        }
        Submission result = submission;
        if (marking || content.barrier().removed()) {
            result = this.submissions.archive(submission.id()).orElseThrow();
        }
        return result;
    }

    private void apply(final Submission submission) {
        final SubmissionContent content = submission.content();
        final String entrance = Objects.requireNonNull(content.entranceId());
        if (!this.entrances.apply(
            new EntranceMarkings.Change(
                this.building(content, entrance), entrance,
                EntranceMarkings.Marking.valueOf(
                    Objects.requireNonNull(content.entranceMarking()).name()
                ),
                submission.createdAt(), submission.verification()
            )
        )) {
            throw new StaleEntranceMarking();
        }
    }

    private String building(final SubmissionContent content, final String entrance) {
        return Optional.ofNullable(content.place().buildingSymbol()).orElseGet(
            () -> this.buildings.entrance(entrance, null)
                .map(EntrancePlace::buildingSymbol).orElseThrow(StaleEntranceMarking::new)
        );
    }
}
