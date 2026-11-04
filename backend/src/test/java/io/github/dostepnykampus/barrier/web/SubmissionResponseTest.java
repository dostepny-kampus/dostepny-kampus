/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Odpowiedź ze zgłoszeniem (W4-4): treść płasko obok identyfikatora, wersji,
 * statusu i chwili utworzenia jako napisu ISO-8601, bez pól pustych i flag
 * fałszywych.
 *
 * @since 0.1.0
 */
@Disabled("Odpowiedź ze zgłoszeniem jeszcze nie zapisuje JSON")
final class SubmissionResponseTest {

    @Test
    void wfourFourResponseIsFlatJsonWithoutEmptyFields() {
        Assertions.assertEquals(
            WebBodies.MAPPER.readTree(
                String.join(
                    "",
                    "{'id':'%s','version':2,'status':'IN_REVIEW','createdAt':'2026-03-01T10:00:00Z',",
                    "'kind':'QUICK','location':{'type':'Point','coordinates':[17.06,51.1]},",
                    "'accuracyMeters':4.5,'buildingSymbol':'B1','floor':{'floorId':'p2','number':2},",
                    "'barrierGroup':'OTHER','severity':2,'description':'opis','photoIds':['%s']}"
                ).formatted(WebBodies.ID, WebBodies.PHOTO).replace('\'', '"')
            ),
            WebBodies.MAPPER.valueToTree(
                new SubmissionResponse(
                    new Submission(
                        WebBodies.ID, 2, SubmissionStatus.IN_REVIEW,
                        Instant.parse("2026-03-01T10:00:00Z"),
                        new SubmissionContent(
                            SubmissionKind.QUICK,
                            new SubmissionPlace(
                                new SubmissionPoint(17.06, 51.1), 4.5, false, "B1",
                                new SubmissionFloor("p2", 2, false)
                            ),
                            new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false),
                            null, null, null, List.of(WebBodies.PHOTO)
                        )
                    )
                )
            ),
            "Odpowiedź różni się od oczekiwanego JSON"
        );
    }
}
