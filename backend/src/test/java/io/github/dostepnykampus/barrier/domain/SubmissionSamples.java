/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Próbki zgłoszeń wspólne dla testów komend, zapytań i wyników.
 *
 * @since 0.1.0
 */
final class SubmissionSamples {

    /**
     * Identyfikator zgłoszenia w próbkach.
     */
    static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Chwila utworzenia zgłoszenia w próbkach.
     */
    static final Instant CREATED = Instant.parse("2026-03-01T10:00:00Z");

    private SubmissionSamples() {
    }

    static SubmissionContent content() {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(
                new SubmissionPoint(17.06, 51.1), 4.5, true, "B1",
                new SubmissionFloor("p2", 2, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false),
            null,
            null,
            null,
            List.of(UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
        );
    }

    static Submission submission(final int version, final SubmissionStatus status) {
        return new Submission(
            SubmissionSamples.ID, version, status, SubmissionSamples.CREATED,
            SubmissionSamples.content()
        );
    }
}
