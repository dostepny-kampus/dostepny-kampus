/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Odpowiedź ze zgłoszeniem (W4-4): treść płasko obok identyfikatora, wersji,
 * statusu i chwili utworzenia jako napisu ISO-8601, bez pól pustych i flag
 * fałszywych; data weryfikacji tylko przy zgłoszeniu przyjętym.
 *
 * @since 0.1.0
 */
final class SubmissionResponseTest {

    @Test
    void writesFlatJsonWithoutEmptyFields() {
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
                        SubmissionResponseTest.content()
                    )
                )
            ),
            "Odpowiedź różni się od oczekiwanego JSON"
        );
    }

    @Test
    void writesVerificationTimeOfAcceptedSubmission() {
        final String verified = "2026-03-02T09:30:00Z";
        Assertions.assertEquals(
            verified,
            WebBodies.MAPPER.valueToTree(
                new SubmissionResponse(
                    new Submission(
                        WebBodies.ID, 1, SubmissionStatus.ACCEPTED,
                        Instant.parse("2026-03-01T10:00:00Z"),
                        SubmissionResponseTest.content(),
                        Instant.parse(verified)
                    )
                )
            ).path("verifiedAt").asString(),
            "Odpowiedź przyjętego zgłoszenia nie ma daty weryfikacji"
        );
    }

    private static SubmissionContent content() {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(
                new SubmissionPoint(17.06, 51.1), 4.5, false, "B1",
                new SubmissionFloor("p2", 2, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false),
            null, null, null, List.of(WebBodies.PHOTO)
        );
    }
}
