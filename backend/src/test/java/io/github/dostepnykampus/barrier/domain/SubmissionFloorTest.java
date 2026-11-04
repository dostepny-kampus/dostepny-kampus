/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Piętro zgłoszenia (BR-G1): dowolna kombinacja pól jest dozwolona,
 * a piętro rozpoznane to takie z identyfikatorem albo numerem.
 *
 * @since 0.1.0
 */
final class SubmissionFloorTest {

    @Test
    void brGOneAcceptsEveryCombinationOfFields() {
        Assertions.assertAll(
            Stream.of("p1", null).flatMap(
                id -> Stream.of(2, null).flatMap(
                    number -> Stream.of(true, false).map(
                        mezzanine -> () -> Assertions.assertDoesNotThrow(
                            () -> new SubmissionFloor(id, number, mezzanine),
                            "Kombinacja pól piętra została odrzucona"
                        )
                    )
                )
            )
        );
    }

    @Test
    void brGOneIdentifiesFloorByIdOrNumber() {
        Assertions.assertAll(
            () -> Assertions.assertTrue(
                new SubmissionFloor("p1", null, false).identified(),
                "Piętro z identyfikatorem nie jest rozpoznane"
            ),
            () -> Assertions.assertTrue(
                new SubmissionFloor(null, 0, false).identified(),
                "Piętro z numerem 0 nie jest rozpoznane"
            ),
            () -> Assertions.assertTrue(
                new SubmissionFloor("p1", 1, true).identified(),
                "Piętro z identyfikatorem i numerem nie jest rozpoznane"
            )
        );
    }

    @Test
    void brGOneDoesNotIdentifyEmptyFloor() {
        Assertions.assertAll(
            () -> Assertions.assertFalse(
                new SubmissionFloor(null, null, false).identified(),
                "Puste piętro jest rozpoznane"
            ),
            () -> Assertions.assertFalse(
                new SubmissionFloor(null, null, true).identified(),
                "Samo półpiętro jest rozpoznane"
            )
        );
    }
}
