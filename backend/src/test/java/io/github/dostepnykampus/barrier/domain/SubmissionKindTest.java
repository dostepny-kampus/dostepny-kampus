/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Reguła rodzaju zgłoszenia po wszystkich wartościach enumu: budynek
 * z konturu tylko w zgłoszeniu szybkim.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
final class SubmissionKindTest {

    @Test
    void brFourteenTakesBuildingFromOutlineOnlyForQuick() {
        final Set<SubmissionKind> outlined = EnumSet.of(SubmissionKind.QUICK);
        Assertions.assertAll(
            Stream.of(SubmissionKind.values()).map(
                kind -> () -> Assertions.assertEquals(
                    outlined.contains(kind),
                    kind.buildingFromOutline(),
                    String.format("Budynek z konturu dla rodzaju %s niezgodny z BR-14", kind)
                )
            )
        );
    }
}
