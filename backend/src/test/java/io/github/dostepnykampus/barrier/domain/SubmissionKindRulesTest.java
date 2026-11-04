/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Reguły rodzaju zgłoszenia w treści (BR-7, BR-8, BR-19, BR-24, US7.3):
 * które pola są wymagane zależnie od rodzaju i jaki opis niesie odmowa.
 *
 * @since 0.1.0
 */
final class SubmissionKindRulesTest {

    /**
     * Zgłoszenie zastępowane przez usunięcie bariery.
     */
    private static final UUID REPLACED = UUID.fromString("7a1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Miejsce w budynku z rozpoznanym piętrem.
     */
    private static final SubmissionPlace INSIDE = SubmissionKindRulesTest.place(
        "C-13", new SubmissionFloor(null, 0, false)
    );

    /**
     * Szczegóły bariery spełniające każdy rodzaj.
     */
    private static final BarrierDetails GOOD = new BarrierDetails(
        BarrierGroup.OTHER, new BarrierType(3), 2, null, false
    );

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brNineteenNeedsReplacedSubmissionForEveryKind() {
        Assertions.assertAll(
            Stream.of(SubmissionKind.values()).map(
                kind -> () -> SubmissionKindRulesTest.rejected(
                    "„Bariera usunięta” wymaga zgłoszenia w polu replacesSubmissionId (BR-19)",
                    String.format("Usunięcie bez zgłoszenia przeszło dla %s", kind),
                    () -> SubmissionKindRulesTest.removal(kind, null)
                )
            )
        );
    }

    @Test
    void brNineteenAcceptsRemovalWithReplacedSubmission() {
        Assertions.assertAll(
            Stream.of(SubmissionKind.values()).map(
                kind -> () -> Assertions.assertDoesNotThrow(
                    () -> SubmissionKindRulesTest.removal(kind, SubmissionKindRulesTest.REPLACED),
                    String.format("Usunięcie ze zgłoszeniem odrzucono dla %s", kind)
                )
            )
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brEightNeedsBarrierGroup() {
        Assertions.assertEquals(
            "Zgłoszenie szybkie wymaga grupy barier (BR-8)",
            SubmissionKindRulesTest.message(
                "Zgłoszenie szybkie bez grupy barier przeszło",
                () -> SubmissionKindRulesTest.valid(
                    SubmissionKind.QUICK, new BarrierDetails(null, null, null, null, false)
                )
            ),
            "Zły opis błędu braku grupy"
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brSevenNeedsBarrierType() {
        Assertions.assertEquals(
            "Pomiar precyzyjny wymaga typu bariery (BR-7)",
            SubmissionKindRulesTest.message(
                "Pomiar bez typu przeszedł",
                () -> SubmissionKindRulesTest.valid(
                    SubmissionKind.PRECISE, new BarrierDetails(null, null, 2, null, false)
                )
            ),
            "Zły opis błędu braku typu"
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brSevenNeedsSeverityUnlessBarrierRemoved() {
        Assertions.assertEquals(
            "Pomiar precyzyjny wymaga uciążliwości (BR-7)",
            SubmissionKindRulesTest.message(
                "Pomiar bez uciążliwości przeszedł",
                () -> SubmissionKindRulesTest.valid(
                    SubmissionKind.PRECISE,
                    new BarrierDetails(null, new BarrierType(3), null, null, false)
                )
            ),
            "Zły opis błędu braku uciążliwości"
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brSevenNeedsIdentifiedFloorInBuilding() {
        Assertions.assertAll(
            Stream.of(null, new SubmissionFloor(null, null, false)).map(
                floor -> () -> SubmissionKindRulesTest.rejected(
                    "Pomiar precyzyjny w budynku wymaga piętra (BR-7)",
                    "Pomiar w budynku bez rozpoznanego piętra przeszedł",
                    () -> SubmissionKindRulesTest.content(
                        SubmissionKind.PRECISE, SubmissionKindRulesTest.place("C-13", floor),
                        SubmissionKindRulesTest.GOOD, null, null, null
                    )
                )
            )
        );
    }

    @Test
    void brSevenAcceptsFloorByIdOrNumberAndNoFloorOutsideBuildings() {
        Assertions.assertAll(
            Stream.of(
                SubmissionKindRulesTest.place("C-13", new SubmissionFloor("p1", null, false)),
                SubmissionKindRulesTest.place("C-13", new SubmissionFloor(null, -1, true)),
                SubmissionKindRulesTest.place(null, null)
            ).map(
                place -> () -> Assertions.assertDoesNotThrow(
                    () -> SubmissionKindRulesTest.content(
                        SubmissionKind.PRECISE, place, SubmissionKindRulesTest.GOOD,
                        null, null, null
                    ),
                    "Poprawne miejsce pomiaru zostało odrzucone"
                )
            )
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void usSevenThreeNeedsDescriptionOfOtherBarrier() {
        Assertions.assertAll(
            Stream.of(null, "", "   ").map(
                text -> () -> SubmissionKindRulesTest.rejected(
                    "Typ TB-29 „inna bariera” wymaga opisu (US7.3)",
                    "TB-29 bez opisu przeszedł",
                    () -> SubmissionKindRulesTest.valid(
                        SubmissionKind.PRECISE,
                        new BarrierDetails(null, new BarrierType(29), 2, text, false)
                    )
                )
            )
        );
    }

    @Test
    void usSevenThreeAcceptsOtherBarrierWithDescriptionAndOthersWithout() {
        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionKindRulesTest.valid(
                    SubmissionKind.PRECISE,
                    new BarrierDetails(null, new BarrierType(29), 2, "Słup", false)
                ),
                "TB-29 z opisem została odrzucona"
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionKindRulesTest.valid(
                    SubmissionKind.PRECISE,
                    new BarrierDetails(null, new BarrierType(28), 2, null, false)
                ),
                "Typ inny niż TB-29 bez opisu został odrzucony"
            )
        );
    }

    @Test
    @Disabled("treść zgłoszenia nie sprawdza reguł rodzaju, kod w następnym PR")
    void brTwentyFourNeedsEntranceIdAndMarking() {
        Assertions.assertAll(
            () -> SubmissionKindRulesTest.entranceRejected(null, NewEntranceMarking.ACCESSIBLE),
            () -> SubmissionKindRulesTest.entranceRejected("node/1", null),
            () -> SubmissionKindRulesTest.entranceRejected(null, null)
        );
    }

    @Test
    void brSevenAcceptsValidContentAndIgnoresFieldsOutsideOfKind() {
        Assertions.assertAll(
            () -> Assertions.assertAll(
                Stream.of(SubmissionKind.values()).map(
                    kind -> () -> Assertions.assertDoesNotThrow(
                        () -> SubmissionKindRulesTest.valid(kind, SubmissionKindRulesTest.GOOD),
                        String.format("Poprawna treść rodzaju %s została odrzucona", kind)
                    )
                )
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionKindRulesTest.content(
                    SubmissionKind.QUICK, SubmissionKindRulesTest.place("C-13", null),
                    SubmissionKindRulesTest.GOOD, null, null, null
                ),
                "Typ bariery w zgłoszeniu szybkim został odrzucony"
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionKindRulesTest.content(
                    SubmissionKind.ENTRANCE_MARKING, SubmissionKindRulesTest.place("C-13", null),
                    new BarrierDetails(null, null, null, null, false),
                    "node/1", NewEntranceMarking.INACCESSIBLE, null
                ),
                "Zmiana oznaczenia wejścia bez bariery została odrzucona"
            ),
            () -> Assertions.assertDoesNotThrow(
                () -> SubmissionKindRulesTest.content(
                    SubmissionKind.PRECISE, SubmissionKindRulesTest.INSIDE,
                    SubmissionKindRulesTest.GOOD, "node/1", NewEntranceMarking.ACCESSIBLE, null
                ),
                "Wejście w pomiarze precyzyjnym zostało odrzucone"
            )
        );
    }

    @Test
    void brSevenRejectsTypeOutsideOfClassificationAndSeverityAboveThree() {
        Assertions.assertAll(
            () -> SubmissionKindRulesTest.rejected(
                "Typ bariery spoza TB-1…TB-29", "Typ TB-30 przeszedł",
                () -> new BarrierType("TB-30")
            ),
            () -> SubmissionKindRulesTest.rejected(
                "Uciążliwość spoza zakresu 1–3", "Uciążliwość 4 przeszła",
                () -> new BarrierDetails(null, null, 4, null, false)
            )
        );
    }

    private static void entranceRejected(final String entrance, final NewEntranceMarking marking) {
        SubmissionKindRulesTest.rejected(
            "Zmiana oznaczenia wejścia wymaga entranceId i entranceMarking (BR-24)",
            "Zmiana oznaczenia wejścia z brakami przeszła",
            () -> SubmissionKindRulesTest.content(
                SubmissionKind.ENTRANCE_MARKING, SubmissionKindRulesTest.INSIDE,
                SubmissionKindRulesTest.GOOD, entrance, marking, null
            )
        );
    }

    private static void rejected(final String detail, final String why, final Executable build) {
        Assertions.assertEquals(detail, SubmissionKindRulesTest.message(why, build), "Zły opis");
    }

    private static String message(final String why, final Executable build) {
        return Assertions.assertThrows(InvalidSubmission.class, build, why).getMessage();
    }

    private static SubmissionContent removal(final SubmissionKind kind, final UUID replaces) {
        return SubmissionKindRulesTest.content(
            kind, SubmissionKindRulesTest.INSIDE,
            new BarrierDetails(BarrierGroup.OTHER, new BarrierType(3), null, null, true),
            "node/1", NewEntranceMarking.ACCESSIBLE, replaces
        );
    }

    private static SubmissionContent valid(final SubmissionKind kind, final BarrierDetails bar) {
        return SubmissionKindRulesTest.content(
            kind, SubmissionKindRulesTest.INSIDE, bar, "node/1", NewEntranceMarking.ACCESSIBLE, null
        );
    }

    private static SubmissionPlace place(final String building, final SubmissionFloor floor) {
        return new SubmissionPlace(new SubmissionPoint(17.06, 51.1), 4.5, false, building, floor);
    }

    private static SubmissionContent content(
        final SubmissionKind kind, final SubmissionPlace place, final BarrierDetails barrier,
        final String entrance, final NewEntranceMarking marking, final UUID replaces
    ) {
        return new SubmissionContent(
            kind, place, barrier, entrance, marking, replaces,
            List.of(UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
        );
    }
}
