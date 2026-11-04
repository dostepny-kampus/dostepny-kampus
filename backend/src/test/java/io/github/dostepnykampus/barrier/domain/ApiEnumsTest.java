/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Stałe enumów zgłoszenia są dokładnie wartościami z api/openapi.yaml:
 * schematy SubmissionStatus i BarrierGroup oraz pola kind
 * i entranceMarking schematu SubmissionContent, w kolejności ze spec.
 *
 * @since 0.1.0
 */
final class ApiEnumsTest {

    @Test
    void usEightStatusesMatchSpec() {
        Assertions.assertEquals(
            List.of("SENT", "IN_REVIEW", "ACCEPTED", "REJECTED", "ARCHIVED"),
            ApiEnumsTest.names(SubmissionStatus.values()),
            "Statusy różnią się od schematu SubmissionStatus"
        );
    }

    @Test
    void brSevenKindsMatchSpec() {
        Assertions.assertEquals(
            List.of("PRECISE", "QUICK", "ENTRANCE_MARKING"),
            ApiEnumsTest.names(SubmissionKind.values()),
            "Rodzaje różnią się od pola SubmissionContent.kind"
        );
    }

    @Test
    void usFourGroupsMatchSpec() {
        Assertions.assertEquals(
            List.of(
                "ENTRANCE_AND_DOORS", "VERTICAL_CIRCULATION", "HORIZONTAL_CIRCULATION",
                "TOILETS", "ORIENTATION_AND_INFORMATION", "OTHER"
            ),
            ApiEnumsTest.names(BarrierGroup.values()),
            "Grupy barier różnią się od schematu BarrierGroup"
        );
    }

    @Test
    void brTwentyFourMarkingsMatchSpecWithoutNoData() {
        Assertions.assertEquals(
            List.of("ACCESSIBLE", "INACCESSIBLE"),
            ApiEnumsTest.names(NewEntranceMarking.values()),
            "Oznaczenia różnią się od pola SubmissionContent.entranceMarking"
        );
    }

    private static List<String> names(final Enum<?>... values) {
        return Stream.of(values).map(Enum::name).toList();
    }
}
