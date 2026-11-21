/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.EntranceChangeArchive;
import io.github.dostepnykampus.barrier.submission.ReviewDecision;
import io.github.dostepnykampus.barrier.submission.StaleEntranceMarking;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.building.api.EntranceMarkings;
import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis decyzji Weryfikatora i skutku przyjęcia zmiany oznaczenia wejścia.
 *
 * <p>Wymagania: US17.2, US17.3, BR-11, BR-23, BR-24</p>
 *
 * @since 0.1.0
 */
@Service
public class SubmissionReview {

    /** Zapis decyzji o zgłoszeniu. */
    private final SubmissionRepository decisions;

    /** Zastosowanie oznaczenia w danych budynku. */
    private final EntranceMarkings entrances;

    /** Archiwizacja zastosowanej zmiany. */
    private final EntranceChangeArchive archive;

    /** Lokalizacja wejścia, gdy zgłoszenie nie wskazuje budynku. */
    private final KnownBuildings buildings;

    SubmissionReview(
        final SubmissionRepository decisions, final EntranceMarkings entrances,
        final EntranceChangeArchive archive, final KnownBuildings buildings
    ) {
        this.decisions = decisions;
        this.entrances = entrances;
        this.archive = archive;
        this.buildings = buildings;
    }

    /**
     * Zapisuje decyzję, a przyjętą zmianę oznaczenia stosuje i archiwizuje atomowo.
     *
     * @param decision Decyzja Weryfikatora
     * @return Wynik zapisu decyzji
     * @throws StaleEntranceMarking Gdy przyjęcia nie da się zastosować do bieżącego wejścia
     */
    @Transactional
    public EditResult decide(final ReviewDecision decision) throws StaleEntranceMarking {
        EditResult result = this.decisions.decide(decision);
        if (result instanceof EditResult.Saved saved && saved.submission().accepted()
            && saved.submission().content().kind() == SubmissionKind.ENTRANCE_MARKING) {
            result = this.apply(saved.submission());
        }
        return result;
    }

    private EditResult apply(final Submission submission) {
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
        return new EditResult.Saved(this.archive.archiveEntranceChange(submission.id()));
    }

    private String building(final SubmissionContent content, final String entrance) {
        return Optional.ofNullable(content.place().buildingSymbol()).orElseGet(
            () -> this.buildings.entrance(entrance, null)
                .map(EntrancePlace::buildingSymbol).orElseThrow(StaleEntranceMarking::new)
        );
    }
}
