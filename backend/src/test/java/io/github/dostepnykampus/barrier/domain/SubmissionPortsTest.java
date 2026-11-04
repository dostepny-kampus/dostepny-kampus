/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Porty zapisu i odczytu zgłoszeń (BR-9, BR-10): sygnatury z kontraktu.
 *
 * @since 0.1.0
 */
final class SubmissionPortsTest {

    @Test
    void mthreeFiveRepositoryHasCreateAndUpdate() throws NoSuchMethodException {
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                CreateResult.class,
                SubmissionRepository.class.getMethod(
                    "create", NewSubmission.class
                ).getReturnType(),
                "create zwraca inny typ"
            ),
            () -> Assertions.assertEquals(
                EditResult.class,
                SubmissionRepository.class.getMethod(
                    "update", SubmissionEdit.class
                ).getReturnType(),
                "update zwraca inny typ"
            ),
            () -> Assertions.assertEquals(
                2,
                SubmissionRepository.class.getDeclaredMethods().length,
                "Repozytorium ma inną liczbę metod"
            )
        );
    }

    @Test
    void mthreeFiveFinderHasFindAndList() throws NoSuchMethodException {
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Optional.class,
                SubmissionFinder.class.getMethod("find", UUID.class).getReturnType(),
                "find zwraca inny typ"
            ),
            () -> Assertions.assertEquals(
                List.class,
                SubmissionFinder.class.getMethod(
                    "list", SubmissionQuery.class
                ).getReturnType(),
                "list zwraca inny typ"
            ),
            () -> Assertions.assertEquals(
                2,
                SubmissionFinder.class.getDeclaredMethods().length,
                "Odczyt ma inną liczbę metod"
            )
        );
    }
}
