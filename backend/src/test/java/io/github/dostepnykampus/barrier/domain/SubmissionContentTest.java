/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Treść zgłoszenia (US4, US7): zdjęcia, pola obowiązkowe i kopie
 * z innym budynkiem.
 *
 * @since 0.1.0
 */
final class SubmissionContentTest {

    /**
     * Zdjęcie używane w próbkach.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void usFourRejectsSubmissionWithoutPhoto() {
        Assertions.assertEquals(
            "Zgłoszenie wymaga co najmniej jednego UUID zdjęcia w polu photoIds",
            Assertions.assertThrows(
                InvalidSubmission.class,
                () -> SubmissionContentTest.content(List.of()),
                "Zgłoszenie bez zdjęcia przeszło"
            ).getMessage(),
            "Zły opis błędu braku zdjęcia"
        );
    }

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void usFourRejectsMissingRequiredComponents() {
        final SubmissionContent valid = SubmissionContentTest.content(
            List.of(SubmissionContentTest.PHOTO)
        );
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionContent(
                    null, valid.place(), valid.barrier(), null, null, null, valid.photoIds()
                ),
                "Zgłoszenie bez rodzaju przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionContent(
                    valid.kind(), null, valid.barrier(), null, null, null, valid.photoIds()
                ),
                "Zgłoszenie bez miejsca przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionContent(
                    valid.kind(), valid.place(), null, null, null, null, valid.photoIds()
                ),
                "Zgłoszenie bez bariery przeszło"
            ),
            () -> Assertions.assertThrows(
                NullPointerException.class,
                () -> new SubmissionContent(
                    valid.kind(), valid.place(), valid.barrier(), null, null, null, null
                ),
                "Zgłoszenie bez listy zdjęć przeszło"
            )
        );
    }

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void usFourCopiesPhotoIdsAsUnmodifiableList() {
        final List<UUID> source = new ArrayList<>(List.of(SubmissionContentTest.PHOTO));
        final SubmissionContent content = SubmissionContentTest.content(source);
        source.add(UUID.randomUUID());
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                List.of(SubmissionContentTest.PHOTO),
                content.photoIds(),
                "Zmiana listy wejściowej zmieniła zgłoszenie"
            ),
            () -> Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> content.photoIds().add(UUID.randomUUID()),
                "Lista zdjęć zgłoszenia jest modyfikowalna"
            )
        );
    }

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void brGTwoWithBuildingChangesOnlyPlaceOfContent() {
        final SubmissionContent origin = SubmissionContentTest.content(
            List.of(SubmissionContentTest.PHOTO)
        );
        final SubmissionContent copy = origin.withBuilding("C3");
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionContentTest.rebuilt(origin, origin.place().withBuilding("C3")),
                copy,
                "Kopia z budynkiem zmieniła inne pola treści"
            ),
            () -> Assertions.assertEquals(
                "B1", origin.place().buildingSymbol(), "Oryginał treści zmienił budynek"
            )
        );
    }

    @Test
    @Disabled("Reguły treści zgłoszenia dochodzą w następnym PR")
    void brGTwoOutsideBuildingsChangesOnlyPlaceOfContent() {
        final SubmissionContent origin = SubmissionContentTest.content(
            List.of(SubmissionContentTest.PHOTO)
        );
        final SubmissionContent copy = origin.outsideBuildings();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SubmissionContentTest.rebuilt(origin, origin.place().outsideBuildings()),
                copy,
                "Kopia poza budynkami zmieniła inne pola treści"
            ),
            () -> Assertions.assertEquals(
                "B1", origin.place().buildingSymbol(), "Oryginał treści utracił budynek"
            )
        );
    }

    private static SubmissionContent rebuilt(
        final SubmissionContent origin, final SubmissionPlace place
    ) {
        return new SubmissionContent(
            origin.kind(), place, origin.barrier(), origin.entranceId(),
            origin.entranceMarking(), origin.replaces(), origin.photoIds()
        );
    }

    private static SubmissionContent content(final List<UUID> photos) {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(
                new SubmissionPoint(17.06, 51.1), 4.5, true, "B1",
                new SubmissionFloor("p2", 2, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false),
            "node/1",
            NewEntranceMarking.ACCESSIBLE,
            UUID.fromString("7a1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
            photos
        );
    }
}
