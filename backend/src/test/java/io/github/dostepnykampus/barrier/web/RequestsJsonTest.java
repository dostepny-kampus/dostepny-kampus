/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Ciała POST i PUT zgłoszenia (W4-1, W4-2, W4-3): identyfikator albo wersja
 * i treść płasko w jednym obiekcie, ścisłe wiązanie także przez podwójne
 * rozpakowanie treści.
 *
 * @since 0.1.0
 */
final class RequestsJsonTest {

    @Test
    void wfourOneNewRequestGivesCommandWithIdAndContent() {
        Assertions.assertEquals(
            new NewSubmission(WebBodies.ID, WebBodies.content(), "konto"),
            WebBodies.MAPPER.readValue(
                WebBodies.newBody("'extra':'x'"), NewSubmissionRequest.class
            ).toCommand("konto"),
            "Komenda bez identyfikatora z ciała albo bez treści"
        );
    }

    @Test
    void wfourOneNewRequestWithoutIdIsClientError() {
        Assertions.assertEquals(
            "Brak identyfikatora zgłoszenia w polu id",
            Assertions.assertThrows(
                InvalidSubmission.class,
                () -> WebBodies.MAPPER.readValue(
                    WebBodies.body("'extra':'x'"), NewSubmissionRequest.class
                ).toCommand("konto"),
                "Zgłoszenie bez id nie dało błędu 400"
            ).getMessage(),
            "Zły komunikat błędu"
        );
    }

    @Test
    void wfourTwoUpdateRequestTakesPathIdAndIgnoresBodyId() {
        Assertions.assertEquals(
            new SubmissionEdit(WebBodies.PATH, 3, WebBodies.content()),
            WebBodies.MAPPER.readValue(
                WebBodies.body(String.format("'version':3,'id':'%s'", WebBodies.ID)),
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
                    WebBodies.newBody(extra),
                    NewSubmissionRequest.class
                ).toCommand("konto").id(),
                "Nieznane pole zepsuło wiązanie POST"
            ),
            () -> Assertions.assertEquals(
                2,
                WebBodies.MAPPER.readValue(
                    WebBodies.body(String.format("'version':2,%s", extra)),
                    SubmissionUpdateRequest.class
                ).toCommand(WebBodies.PATH).expectedVersion(),
                "Nieznane pole zepsuło wiązanie PUT"
            )
        );
    }

    @Test
    void wfourThreeNewRequestRejectsNullAndCoercion() {
        Assertions.assertAll(
            Stream.concat(
                Stream.of(WebBodies.body("'id':null")),
                Stream.of(
                    "'accuracyMeters':null", "'accuracyMeters':'5'", "'manualCorrection':'true'",
                    "'severity':null", "'severity':'2'", "'barrierRemoved':'true'"
                ).map(WebBodies::newBody)
            ).map(
                body -> () -> Assertions.assertThrows(
                    MismatchedInputException.class,
                    () -> WebBodies.MAPPER.readValue(body, NewSubmissionRequest.class),
                    String.format("Wiązanie POST przyjęło %s", body)
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
                    String.format("Wiązanie PUT przyjęło %s", head)
                )
            )
        );
    }
}
