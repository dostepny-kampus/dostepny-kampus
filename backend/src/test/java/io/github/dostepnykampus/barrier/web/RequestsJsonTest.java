/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.InvalidInput;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Ciała POST i PUT zgłoszenia (W4-1, W4-2, W4-3): identyfikator albo wersja
 * i treść płasko w jednym obiekcie, ścisłe wiązanie także przez podwójne
 * rozpakowanie treści.
 *
 * @since 0.1.0
 */
@Disabled("Ciała żądań zgłoszeń jeszcze nie wiążą JSON")
final class RequestsJsonTest {

    private static final String NEW_ID = "'id':'" + WebBodies.ID + "'";

    @Test
    void wfourOneNewRequestGivesCommandWithIdAndContent() {
        Assertions.assertEquals(
            new NewSubmission(WebBodies.ID, WebBodies.content()),
            WebBodies.MAPPER.readValue(
                WebBodies.body(RequestsJsonTest.NEW_ID), NewSubmissionRequest.class
            ).toCommand(),
            "Komenda bez identyfikatora z ciała albo bez treści"
        );
    }

    @Test
    void wfourTwoUpdateRequestTakesPathIdAndIgnoresBodyId() {
        Assertions.assertEquals(
            new SubmissionEdit(WebBodies.PATH, 3, WebBodies.content()),
            WebBodies.MAPPER.readValue(
                WebBodies.body("'version':3," + RequestsJsonTest.NEW_ID),
                SubmissionUpdateRequest.class
            ).toCommand(WebBodies.PATH),
            "Komenda edycji bez identyfikatora z adresu, wersji albo treści"
        );
    }

    @Test
    void wfourTwoUpdateWithoutVersionIsClientError() {
        Assertions.assertThrows(
            InvalidInput.class,
            () -> WebBodies.MAPPER.readValue(
                WebBodies.body("'extra':'x'"), SubmissionUpdateRequest.class
            ).toCommand(WebBodies.PATH),
            "Edycja bez wersji nie dała błędu 400"
        );
    }

    @Test
    void wfourThreeUnknownFieldsAreIgnored() {
        final String extra = "'extra':'x'";
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                WebBodies.ID,
                WebBodies.MAPPER.readValue(
                    WebBodies.body(RequestsJsonTest.NEW_ID + "," + extra),
                    NewSubmissionRequest.class
                ).toCommand().id(),
                "Nieznane pole zepsuło wiązanie POST"
            ),
            () -> Assertions.assertEquals(
                2,
                WebBodies.MAPPER.readValue(
                    WebBodies.body("'version':2," + extra), SubmissionUpdateRequest.class
                ).toCommand(WebBodies.PATH).expectedVersion(),
                "Nieznane pole zepsuło wiązanie PUT"
            )
        );
    }

    @Test
    void wfourThreeNewRequestRejectsNullAndCoercion() {
        Assertions.assertAll(
            Stream.of(
                "'id':null",
                RequestsJsonTest.NEW_ID + ",'accuracyMeters':null",
                RequestsJsonTest.NEW_ID + ",'accuracyMeters':'5'",
                RequestsJsonTest.NEW_ID + ",'manualCorrection':'true'",
                RequestsJsonTest.NEW_ID + ",'severity':null",
                RequestsJsonTest.NEW_ID + ",'severity':'2'",
                RequestsJsonTest.NEW_ID + ",'barrierRemoved':'true'"
            ).map(
                head -> () -> Assertions.assertThrows(
                    MismatchedInputException.class,
                    () -> WebBodies.MAPPER.readValue(
                        WebBodies.body(head), NewSubmissionRequest.class
                    ),
                    "Wiązanie POST przyjęło " + head
                )
            )
        );
    }

    @Test
    void wfourThreeUpdateRequestRejectsNullAndCoercion() {
        Assertions.assertAll(
            Stream.of(
                "'version':null", "'version':'1'", "'version':2.0",
                "'version':1,'accuracyMeters':'5'", "'version':1,'severity':null"
            ).map(
                head -> () -> Assertions.assertThrows(
                    MismatchedInputException.class,
                    () -> WebBodies.MAPPER.readValue(
                        WebBodies.body(head), SubmissionUpdateRequest.class
                    ),
                    "Wiązanie PUT przyjęło " + head
                )
            )
        );
    }
}
