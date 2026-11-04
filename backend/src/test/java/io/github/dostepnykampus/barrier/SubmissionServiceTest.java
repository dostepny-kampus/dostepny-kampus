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
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.building.Outlines;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Usługa zapisu zgłoszeń (BR-14, BR-G2, BR-9, BR-10): budynek zgłoszenia
 * szybkiego ustala serwer z konturu, wyniki portu zapisu wracają bez zmian.
 *
 * @since 0.1.0
 */
final class SubmissionServiceTest {

    /**
     * Identyfikator zgłoszenia w próbkach.
     */
    private static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Szerokość punktu asymetrycznego: różna od długości, żeby pomyłka kolejności była widoczna.
     */
    private static final double LAT = 51.1;

    /**
     * Długość punktu asymetrycznego.
     */
    private static final double LON = 17.06;

    /**
     * Kontur znajdujący budynek tylko dla szerokości LAT i długości LON w tej kolejności.
     */
    private static final Outlines CONTOUR = (lat, lon) -> {
        final Optional<String> found;
        if (Double.compare(lat, SubmissionServiceTest.LAT) == 0
            && Double.compare(lon, SubmissionServiceTest.LON) == 0) {
            found = Optional.of("A1");
        } else {
            found = Optional.empty();
        }
        return found;
    };

    /**
     * Kontur bez budynku w żadnym punkcie.
     */
    private static final Outlines EMPTY = (lat, lon) -> Optional.empty();

    /**
     * Kontur, którego użycie jest błędem.
     */
    private static final Outlines FORBIDDEN = (lat, lon) -> {
        throw new AssertionError("Kontur nie powinien być pytany");
    };

    @Test
    void ioneOneQuickTakesBuildingFromOutlineAskedWithLatitudeFirst() {
        final RecordingSubmissions repository = SubmissionServiceTest.repository();
        new SubmissionService(repository, SubmissionServiceTest.CONTOUR).create(
            new NewSubmission(SubmissionServiceTest.ID, SubmissionServiceTest.quick(null))
        );
        Assertions.assertEquals(
            List.of(
                new NewSubmission(
                    SubmissionServiceTest.ID, SubmissionServiceTest.quick("A1")
                )
            ),
            repository.creations(),
            "Do zapisu trafiła treść bez symbolu z konturu"
        );
    }

    @Test
    void ioneTwoQuickIgnoresBuildingFromClientInsideOutline() {
        final RecordingSubmissions repository = SubmissionServiceTest.repository();
        new SubmissionService(repository, SubmissionServiceTest.CONTOUR).create(
            new NewSubmission(SubmissionServiceTest.ID, SubmissionServiceTest.quick("Z9"))
        );
        Assertions.assertEquals(
            List.of(
                new NewSubmission(
                    SubmissionServiceTest.ID, SubmissionServiceTest.quick("A1")
                )
            ),
            repository.creations(),
            "Budynek od klienta nie został zastąpiony symbolem z konturu"
        );
    }

    @Test
    void ioneTwoQuickOutsideOutlineDropsBuildingAndKeepsFloor() {
        final RecordingSubmissions repository = SubmissionServiceTest.repository();
        new SubmissionService(repository, SubmissionServiceTest.EMPTY).create(
            new NewSubmission(SubmissionServiceTest.ID, SubmissionServiceTest.quick("Z9"))
        );
        Assertions.assertEquals(
            List.of(
                new NewSubmission(
                    SubmissionServiceTest.ID, SubmissionServiceTest.quick(null)
                )
            ),
            repository.creations(),
            "Poza konturem miejsce powinno stracić tylko symbol budynku"
        );
    }

    @Test
    void ioneThreePreciseSkipsOutlineAndKeepsClientBuilding() {
        final RecordingSubmissions repository = SubmissionServiceTest.repository();
        final NewSubmission command = new NewSubmission(
            SubmissionServiceTest.ID, SubmissionServiceTest.precise("Z9")
        );
        new SubmissionService(repository, SubmissionServiceTest.FORBIDDEN).create(command);
        Assertions.assertEquals(
            List.of(command),
            repository.creations(),
            "Pomiar precyzyjny powinien trafić do zapisu bez zmian"
        );
    }

    @Test
    void ioneFourCreateReturnsRepositoryResultAndPassesCommand() {
        final Submission stored = SubmissionServiceTest.stored();
        final NewSubmission command = new NewSubmission(
            SubmissionServiceTest.ID, SubmissionServiceTest.precise("Z9")
        );
        final CreateResult created = new CreateResult.Created(stored);
        final CreateResult existing = new CreateResult.Existing(stored);
        Assertions.assertAll(
            () -> Assertions.assertSame(
                created,
                new SubmissionService(
                    new RecordingSubmissions(created, null), SubmissionServiceTest.FORBIDDEN
                ).create(command),
                "Utworzone zgłoszenie nie wróciło tym samym wynikiem"
            ),
            () -> Assertions.assertSame(
                existing,
                new SubmissionService(
                    new RecordingSubmissions(existing, null), SubmissionServiceTest.FORBIDDEN
                ).create(command),
                "Istniejące zgłoszenie nie wróciło tym samym wynikiem"
            )
        );
    }

    @Test
    void ioneFourUpdateReturnsRepositoryResultAndPassesCommand() {
        final Submission stored = SubmissionServiceTest.stored();
        final SubmissionEdit command = new SubmissionEdit(
            SubmissionServiceTest.ID, 3, SubmissionServiceTest.precise("Z9")
        );
        final EditResult saved = new EditResult.Saved(stored);
        final EditResult conflict = new EditResult.Conflict(stored);
        final EditResult missing = new EditResult.Missing(SubmissionServiceTest.ID);
        final RecordingSubmissions repository = new RecordingSubmissions(null, saved);
        Assertions.assertAll(
            () -> Assertions.assertSame(
                saved,
                new SubmissionService(repository, SubmissionServiceTest.FORBIDDEN)
                    .update(command),
                "Zapisana edycja nie wróciła tym samym wynikiem"
            ),
            () -> Assertions.assertEquals(
                List.of(command), repository.edits(),
                "Id, wersja bazowa i treść powinny trafić do zapisu bez zmian"
            ),
            () -> Assertions.assertSame(
                conflict,
                new SubmissionService(
                    new RecordingSubmissions(null, conflict), SubmissionServiceTest.FORBIDDEN
                ).update(command),
                "Konflikt nie wrócił tym samym wynikiem"
            ),
            () -> Assertions.assertSame(
                missing,
                new SubmissionService(
                    new RecordingSubmissions(null, missing), SubmissionServiceTest.FORBIDDEN
                ).update(command),
                "Brak zgłoszenia nie wrócił tym samym wynikiem"
            )
        );
    }

    @Test
    void ioneFiveUpdateAppliesSameBuildingRulesAsCreate() {
        final RecordingSubmissions repository = SubmissionServiceTest.repository();
        new SubmissionService(repository, SubmissionServiceTest.CONTOUR).update(
            new SubmissionEdit(SubmissionServiceTest.ID, 2, SubmissionServiceTest.quick("Z9"))
        );
        Assertions.assertEquals(
            List.of(
                new SubmissionEdit(
                    SubmissionServiceTest.ID, 2, SubmissionServiceTest.quick("A1")
                )
            ),
            repository.edits(),
            "Edycja szybkiego zgłoszenia powinna dostać symbol z konturu"
        );
    }

    private static RecordingSubmissions repository() {
        return new RecordingSubmissions(
            new CreateResult.Created(SubmissionServiceTest.stored()),
            new EditResult.Saved(SubmissionServiceTest.stored())
        );
    }

    private static Submission stored() {
        return new Submission(
            SubmissionServiceTest.ID, 1, SubmissionStatus.SENT,
            Instant.parse("2026-03-01T10:00:00Z"), SubmissionServiceTest.quick("A1")
        );
    }

    private static SubmissionContent quick(final String building) {
        return SubmissionServiceTest.content(
            SubmissionKind.QUICK, building,
            new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false)
        );
    }

    private static SubmissionContent precise(final String building) {
        return SubmissionServiceTest.content(
            SubmissionKind.PRECISE, building,
            new BarrierDetails(null, new BarrierType(5), 2, "opis", false)
        );
    }

    private static SubmissionContent content(
        final SubmissionKind kind, final String building, final BarrierDetails barrier
    ) {
        return new SubmissionContent(
            kind,
            new SubmissionPlace(
                new SubmissionPoint(SubmissionServiceTest.LON, SubmissionServiceTest.LAT),
                4.5, true, building, new SubmissionFloor("p2", 2, false)
            ),
            barrier, null, null, null,
            List.of(UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
        );
    }
}
