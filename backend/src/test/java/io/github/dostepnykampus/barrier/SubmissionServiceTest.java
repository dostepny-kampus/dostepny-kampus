/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
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
import io.github.dostepnykampus.barrier.domain.OutsideCampus;
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
 * Usługa zapisu zgłoszeń: budynek zgłoszenia
 * szybkiego ustala serwer z konturu, wyniki portu zapisu wracają bez zmian.
 *
 * <p>Wymagania: BR-14, BR-G2, BR-9, BR-10</p>
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
            SubmissionServiceTest.content(SubmissionKind.PRECISE, "Z9"), "konto"
        );
        Assertions.assertAll(
            Stream.of(
                new CreateResult.Created(SubmissionServiceTest.STORED),
                new CreateResult.Existing(SubmissionServiceTest.STORED)
            ).map(
                result -> () -> {
                    final SubmissionServiceTest.RecordingSubmissions fake =
                        new SubmissionServiceTest.RecordingSubmissions(result, null);
                    Assertions.assertSame(
                        result,
                        SubmissionServiceTest.service(fake, SubmissionServiceTest.FORBIDDEN)
                            .create(command),
                        "Wynik utworzenia nie wrócił tym samym obiektem"
                    );
                    Assertions.assertEquals(
                        List.of(command), fake.seen(), "Komenda zmieniona po drodze"
                    );
                }
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
                    final SubmissionServiceTest.RecordingSubmissions fake =
                        new SubmissionServiceTest.RecordingSubmissions(null, result);
                    Assertions.assertSame(
                        result,
                        SubmissionServiceTest.service(fake, SubmissionServiceTest.FORBIDDEN)
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
        final SubmissionServiceTest.RecordingSubmissions fake =
            new SubmissionServiceTest.RecordingSubmissions(null, null);
        SubmissionServiceTest.service(fake, SubmissionServiceTest.CONTOUR).update(
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

    @Test
    void brEightPublishesOnlySavedEditForPhotoCleanup() {
        final List<Object> published = new ArrayList<>(1);
        final EditResult saved = new EditResult.Saved(SubmissionServiceTest.STORED);
        Stream.of(
            saved,
            new EditResult.Conflict(SubmissionServiceTest.STORED),
            new EditResult.Missing(SubmissionServiceTest.ID)
        ).forEach(
            result -> new SubmissionService(
                new SubmissionServiceTest.RecordingSubmissions(null, result),
                SubmissionServiceTest.FORBIDDEN, (lat, lon) -> true,
                published::add
            ).update(
                new SubmissionEdit(
                    SubmissionServiceTest.ID, 1,
                    SubmissionServiceTest.content(SubmissionKind.PRECISE, "Z9")
                )
            )
        );
        Assertions.assertEquals(
            List.of(saved), published, "Sprzątanie zdjęć dostało inną edycję niż zapisaną"
        );
    }

    @Test
    void lockedTakesLockBeforeWrite() {
        final SubmissionServiceTest.RecordingSubmissions repository =
            new SubmissionServiceTest.RecordingSubmissions(null, null);
        final List<Object> order = new ArrayList<>(2);
        SubmissionServiceTest.service(repository, SubmissionServiceTest.EMPTY)
            .locked(SubmissionServiceTest.ID, () -> order.addAll(repository.seen()));
        Assertions.assertEquals(
            List.of(SubmissionServiceTest.ID), order,
            "Zapis zdjęcia ruszył bez blokady zgłoszenia"
        );
    }

    @Test
    void brGFiveRejectsSubmissionOutsideCampusWithoutSaving() {
        final SubmissionServiceTest.RecordingSubmissions fake =
            new SubmissionServiceTest.RecordingSubmissions(null, null);
        final List<Double> asked = new ArrayList<>(2);
        final SubmissionService service = new SubmissionService(
            fake, SubmissionServiceTest.EMPTY,
            (lat, lon) -> {
                asked.add(lat);
                asked.add(lon);
                return false;
            },
            event -> { }
        );
        final NewSubmission command = new NewSubmission(
            SubmissionServiceTest.ID,
            SubmissionServiceTest.content(SubmissionKind.QUICK, null), "konto"
        );
        Assertions.assertThrows(OutsideCampus.class, () -> service.create(command));
        final SubmissionPoint point = command.content().place().point();
        Assertions.assertEquals(
            List.of(point.latitude(), point.longitude(), 0),
            List.of(asked.get(0), asked.get(1), fake.seen().size()),
            "Teren PWr pytany nie szerokością i długością albo zgłoszenie zapisane mimo odmowy"
        );
    }

    private static SubmissionService service(
        final SubmissionRepository repository, final Outlines outlines
    ) {
        return new SubmissionService(repository, outlines, (lat, lon) -> true, event -> { });
    }

    private static SubmissionContent created(
        final Outlines outlines, final SubmissionKind kind, final String building
    ) {
        final SubmissionServiceTest.RecordingSubmissions fake =
            new SubmissionServiceTest.RecordingSubmissions(null, null);
        SubmissionServiceTest.service(fake, outlines).create(
            new NewSubmission(
                SubmissionServiceTest.ID, SubmissionServiceTest.content(kind, building), "konto"
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
        private final List<Object> seen;

        RecordingSubmissions(final CreateResult created, final EditResult edited) {
            this.created = created;
            this.edited = edited;
            this.seen = new ArrayList<>(1);
        }

        @Override
        public void lock(final UUID id) {
            this.seen.add(id);
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
