/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.BarrierType;
import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewEntranceMarking;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.building.Outlines;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Usługa zapisu zgłoszeń (BR-14, BR-G2, BR-9, BR-10): budynek zgłoszenia
 * szybkiego ustala serwer z konturu, wyniki portu zapisu wracają bez zmian.
 *
 * @since 0.1.0
 */
final class SubmissionServiceTest {

    /** Identyfikator zgłoszenia w próbkach. */
    private static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /** Zgłoszenie zwracane przez atrapę portu zapisu. */
    private static final Submission STORED = new Submission(
        SubmissionServiceTest.ID, 1, SubmissionStatus.SENT, Instant.EPOCH,
        SubmissionServiceTest.quick("A1")
    );

    /** Kontur z budynkiem tylko dla szerokości 51,1 i długości 17,06 w tej kolejności. */
    private static final Outlines CONTOUR = (lat, lon) -> Optional.of("A1").filter(
        symbol -> Double.compare(lat, 51.1) == 0 && Double.compare(lon, 17.06) == 0
    );

    /** Kontur bez budynku w żadnym punkcie. */
    private static final Outlines EMPTY = (lat, lon) -> Optional.empty();

    /** Kontur, którego użycie jest błędem. */
    private static final Outlines FORBIDDEN = (lat, lon) -> {
        throw new AssertionError("Kontur nie powinien być pytany");
    };

    @Test
    void ioneOneQuickTakesBuildingFromOutlineAskedWithLatitudeFirst() {
        Assertions.assertEquals(
            SubmissionServiceTest.quick("A1"),
            SubmissionServiceTest.created(
                    SubmissionServiceTest.CONTOUR, SubmissionKind.QUICK, null
                ),
            "Do zapisu trafiła treść bez symbolu z konturu"
        );
    }

    @Test
    void ioneTwoQuickIgnoresClientBuildingInsideAndDropsItOutsideOutline() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionServiceTest.quick("A1"),
                SubmissionServiceTest.created(
                    SubmissionServiceTest.CONTOUR, SubmissionKind.QUICK, "Z9"
                ),
                "Budynek od klienta nie został zastąpiony symbolem z konturu"
            ),
            () -> Assertions.assertEquals(
                SubmissionServiceTest.quick(null),
                SubmissionServiceTest.created(
                    SubmissionServiceTest.EMPTY, SubmissionKind.QUICK, "Z9"
                ),
                "Poza konturem miejsce powinno stracić tylko symbol budynku"
            )
        );
    }

    @Test
    void ioneThreeOtherKindsSkipOutlineAndKeepContent() {
        Assertions.assertAll(
            Stream.of(SubmissionKind.PRECISE, SubmissionKind.ENTRANCE_MARKING).map(
                kind -> () -> Assertions.assertEquals(
                    SubmissionServiceTest.content(kind, "Z9"),
                    SubmissionServiceTest.created(SubmissionServiceTest.FORBIDDEN, kind, "Z9"),
                    "Treść rodzaju spoza szybkiego powinna trafić do zapisu bez zmian"
                )
            )
        );
    }

    @Test
    void ioneFourCreateReturnsRepositoryResult() {
        final NewSubmission command = new NewSubmission(
            SubmissionServiceTest.ID,
            SubmissionServiceTest.content(SubmissionKind.PRECISE, "Z9")
        );
        Assertions.assertAll(
            Stream.of(
                new CreateResult.Created(SubmissionServiceTest.STORED),
                new CreateResult.Existing(SubmissionServiceTest.STORED)
            ).map(
                result -> () -> Assertions.assertSame(
                    result,
                    new SubmissionService(
                        new RecordingSubmissions(result, null), SubmissionServiceTest.FORBIDDEN
                    ).create(command),
                    "Wynik utworzenia nie wrócił tym samym obiektem"
                )
            )
        );
    }

    @Test
    void ioneFourUpdateReturnsRepositoryResultAndPassesCommand() {
        final SubmissionEdit command = new SubmissionEdit(
            SubmissionServiceTest.ID, 3,
            SubmissionServiceTest.content(SubmissionKind.PRECISE, "Z9")
        );
        Assertions.assertAll(
            Stream.of(
                new EditResult.Saved(SubmissionServiceTest.STORED),
                new EditResult.Conflict(SubmissionServiceTest.STORED),
                new EditResult.Missing(SubmissionServiceTest.ID)
            ).map(
                result -> () -> {
                    final RecordingSubmissions fake = new RecordingSubmissions(null, result);
                    Assertions.assertSame(
                        result,
                        new SubmissionService(fake, SubmissionServiceTest.FORBIDDEN)
                            .update(command),
                        "Wynik edycji nie wrócił tym samym obiektem"
                    );
                    Assertions.assertEquals(
                        List.of(command), fake.seen(), "Komenda zmieniona po drodze"
                    );
                }
            )
        );
    }

    @Test
    void ioneFiveUpdateAppliesSameBuildingRulesAsCreate() {
        final RecordingSubmissions fake = new RecordingSubmissions(null, null);
        new SubmissionService(fake, SubmissionServiceTest.CONTOUR).update(
            new SubmissionEdit(SubmissionServiceTest.ID, 2, SubmissionServiceTest.quick("Z9"))
        );
        Assertions.assertEquals(
            List.of(
                new SubmissionEdit(SubmissionServiceTest.ID, 2, SubmissionServiceTest.quick("A1"))
            ),
            fake.seen(),
            "Edycja szybkiego zgłoszenia powinna dostać symbol z konturu"
        );
    }

    private static SubmissionContent created(
        final Outlines outlines, final SubmissionKind kind, final String building
    ) {
        final RecordingSubmissions fake = new RecordingSubmissions(null, null);
        new SubmissionService(fake, outlines).create(
            new NewSubmission(
                SubmissionServiceTest.ID, SubmissionServiceTest.content(kind, building)
            )
        );
        return ((NewSubmission) fake.seen().get(0)).content();
    }

    private static SubmissionContent quick(final String building) {
        return SubmissionServiceTest.content(SubmissionKind.QUICK, building);
    }

    private static SubmissionContent content(final SubmissionKind kind, final String building) {
        return new SubmissionContent(
            kind,
            new SubmissionPlace(
                new SubmissionPoint(17.06, 51.1), 4.5, true, building,
                new SubmissionFloor("p2", 2, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, new BarrierType(5), 2, "opis", false),
            "e1", NewEntranceMarking.ACCESSIBLE, null,
            List.of(UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
        );
    }

    /** Atrapa portu zapisu: zapamiętuje przyjęte komendy i zwraca podane wyniki. */
    private static final class RecordingSubmissions implements SubmissionRepository {

        /** Wynik zwracany z utworzenia. */
        private final CreateResult created;

        /** Wynik zwracany z edycji. */
        private final EditResult edited;

        /** Przyjęte komendy w kolejności. */
        private final List<Object> seen = new ArrayList<>(1);

        RecordingSubmissions(final CreateResult created, final EditResult edited) {
            this.created = created;
            this.edited = edited;
        }

        @Override
        public CreateResult create(final NewSubmission submission) {
            this.seen.add(submission);
            return this.created;
        }

        @Override
        public EditResult update(final SubmissionEdit edit) {
            this.seen.add(edit);
            return this.edited;
        }

        List<Object> seen() {
            return List.copyOf(this.seen);
        }
    }
}
