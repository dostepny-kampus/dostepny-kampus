/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Identyfikator w ciele POST zgłoszenia: tekst, który nie jest
 * UUID, i ciało, które nie jest obiektem, kończą wiązanie błędem.
 *
 * <p>Wymagania: US4, BR-9</p>
 *
 * @since 0.1.0
 */
final class RequestIdJsonTest {

    @Test
    void usFourRejectsSubmissionIdThatIsNoUuid() {
        Assertions.assertAll(
            Stream.of("'abc'", "''", "42").map(
                id -> () -> Assertions.assertThrows(
                    MismatchedInputException.class,
                    () -> WebBodies.MAPPER.readValue(
                        WebBodies.body(String.format("'id':%s", id)), NewSubmissionRequest.class
                    ),
                    String.format("Zgłoszenie z id %s przeszło", id)
                )
            )
        );
    }

    @Test
    void usFourRejectsBodyThatIsNoObject() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> WebBodies.MAPPER.readValue("[]", NewSubmissionRequest.class),
            "Tablica JSON przeszła jako zgłoszenie"
        );
    }
}
