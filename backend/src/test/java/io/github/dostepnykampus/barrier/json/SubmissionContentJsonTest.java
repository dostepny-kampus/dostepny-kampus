/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.BarrierType;
import io.github.dostepnykampus.barrier.domain.NewEntranceMarking;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Pełna treść zgłoszenia z OpenAPI w JSON (W3-1) wiąże się z domeną.
 *
 * @since 0.1.0
 */
final class SubmissionContentJsonTest {

    @Test
    @Disabled("zapis treści w JSON jeszcze nie przenosi pól, kod w następnym PR")
    void wThreeOneFullContentBindsToDomain() {
        Assertions.assertEquals(
            new SubmissionContent(
                SubmissionKind.PRECISE,
                new SubmissionPlace(
                    new SubmissionPoint(17.06, 51.1), 4.5, true, "C-13",
                    new SubmissionFloor("p1", 1, true)
                ),
                new BarrierDetails(BarrierGroup.VERTICAL, new BarrierType(29), 3, "opis", true),
                "node/7", NewEntranceMarking.ACCESSIBLE, JsonBodies.REPLACED,
                List.of(JsonBodies.PHOTO)
            ),
            JsonBodies.bind(JsonBodies.json(JsonBodies.FULL)).toContent(),
            "Pełna treść nie przeszła do domeny"
        );
    }
}
