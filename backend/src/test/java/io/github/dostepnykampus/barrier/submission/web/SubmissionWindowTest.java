/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Parametry strony listy zgłoszeń: rozmiar strony i kursor.
 *
 * @since 0.1.0
 */
final class SubmissionWindowTest {

    @Test
    void usesLargestPageWithoutLimit() {
        Assertions.assertEquals(
            500, new SubmissionWindow(null, null).size(),
            "Brak limitu nie dał największej strony"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "500"})
    void acceptsLimitWithinBounds(final String limit) {
        Assertions.assertEquals(
            Integer.parseInt(limit), new SubmissionWindow(null, limit).size(),
            "Limit w granicach nie dał strony tej wielkości"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "501", "-1", "dużo", ""})
    void rejectsLimitOutsideBounds(final String limit) {
        Assertions.assertThrows(
            InvalidInput.class,
            () -> new SubmissionWindow(null, limit).size(),
            "Limit spoza 1–500 przeszedł"
        );
    }

    @Test
    void readsCursorAsSubmissionId() {
        final UUID id = UUID.randomUUID();
        Assertions.assertEquals(
            id, new SubmissionWindow(id.toString(), null).cursor(),
            "Kursor nie wskazał zgłoszenia z parametru after"
        );
    }

    @Test
    void startsFromBeginningWithoutCursor() {
        Assertions.assertEquals(
            SubmissionFinder.Page.START, new SubmissionWindow(null, null).cursor(),
            "Brak parametru after nie zaczął od początku listy"
        );
    }

    @Test
    void rejectsBrokenCursor() {
        Assertions.assertThrows(
            InvalidInput.class,
            () -> new SubmissionWindow("nie-kursor", null).cursor(),
            "Zły kursor przeszedł"
        );
    }
}
