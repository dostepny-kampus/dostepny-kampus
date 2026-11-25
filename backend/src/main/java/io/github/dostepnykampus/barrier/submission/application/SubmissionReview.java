/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.building.api.EntranceMarkings;
import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wzięcie zgłoszenia do weryfikacji i zapis decyzji Weryfikatora ze skutkami przyjęcia:
 * archiwizacją zastąpionego zgłoszenia, zgłoszenia „bariera usunięta” i zastosowanej
 * zmiany oznaczenia wejścia. TODO(#3999): klasa przechodzi do tematu review.
 *
 * <p>Wymagania: US10.6, US17.2, US17.3, BR-11, BR-18, BR-19, BR-23, BR-24</p>
 *
 * @since 0.1.0
 */
@Service
public class SubmissionReview {

    /** Wzięcie zgłoszenia i zapis decyzji. */
    private final ReviewRepository reviews;

    /** Archiwizacja zgłoszeń. */
    private final SubmissionRepository submissions;

    /** Zastosowanie oznaczenia w danych budynku. */
    private final EntranceMarkings entrances;

    /** Lokalizacja wejścia, gdy zgłoszenie nie wskazuje budynku. */
    private final KnownBuildings buildings;

    SubmissionReview(
        final ReviewRepository reviews, final SubmissionRepository submissions,
        final EntranceMarkings entrances, final KnownBuildings buildings
    ) {
        this.reviews = reviews;
        this.submissions = submissions;
        this.entrances = entrances;
        this.buildings = buildings;
    }

    /**
     * Daje zgłoszenie Weryfikatorowi.
     *
     * <p>Wymagania: BR-11, US10.4</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Wynik wzięcia zgłoszenia
     */
    @Transactional
    public EditResult claim(final UUID id, final String reviewer) {
        return this.reviews.claim(id, reviewer);
    }

    /**
     * Sprawdza, czy zgłoszenie wzięło do weryfikacji to konto.
     *
     * <p>Wymagania: BR-11</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param account Identyfikator konta Weryfikatora
     * @return Prawda, gdy konto wzięło zgłoszenie
     */
    public boolean claimedBy(final UUID id, final String account) {
        return this.reviews.claimedBy(id, account);
    }

    /**
     * Zapisuje decyzję razem ze skutkami przyjęcia w jednej transakcji. Przyjęcie zmiany
     * archiwizuje zastąpione zgłoszenie, jeśli jest jeszcze przyjęte. Przyjęte zgłoszenie
     * „bariera usunięta” i zastosowana zmiana oznaczenia wejścia same stają się archiwalne.
     * TODO(#3958): przyjęcie zmiany zgłoszenia, które nie jest już przyjęte, ma dać konflikt.
     *
     * <p>Wymagania: BR-18, BR-19, BR-24</p>
     *
     * @param decision Decyzja Weryfikatora
     * @return Wynik zapisu decyzji; przy przyjęciu zgłoszenie po jego skutkach
     * @throws StaleEntranceMarking Gdy przyjęcia nie da się zastosować do bieżącego wejścia
     */
    @Transactional
    public EditResult decide(final ReviewDecision decision) throws StaleEntranceMarking {
        EditResult result = this.reviews.decide(decision);
        if (result instanceof EditResult.Saved saved && saved.submission().accepted()) {
            result = new EditResult.Saved(this.accepted(saved.submission()));
        }
        return result;
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
