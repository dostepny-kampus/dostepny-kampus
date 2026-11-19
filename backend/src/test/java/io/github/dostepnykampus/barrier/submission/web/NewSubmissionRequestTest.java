/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Ciała POST i PUT zgłoszenia (W4-1, W4-2, W4-3): identyfikator albo wersja
 * i treść płasko w jednym obiekcie, ścisłe wiązanie także przez podwójne
 * rozpakowanie treści. Identyfikator, który nie jest UUID, i ciało, które nie jest obiektem,
 * kończą wiązanie błędem.
 *
 * <p>Wymagania: US4, BR-9</p>
 *
 * @since 0.1.0
 */
final class NewSubmissionRequestTest {

    @Test
    void givesCommandWithIdAndContentForNewRequest() {
        Assertions.assertEquals(
            new NewSubmission(WebBodies.ID, WebBodies.content(), "konto"),
            WebBodies.MAPPER.readValue(
                WebBodies.newBody("'extra':'x'"), NewSubmissionRequest.class
            ).toCommand("konto", symbol -> true),
            "Komenda bez identyfikatora z ciała albo bez treści"
        );
    }

    @Test
    void rejectsNewRequestWithoutId() {
        Assertions.assertEquals(
            "Brak identyfikatora zgłoszenia w polu id",
            Assertions.assertThrows(
                InvalidSubmission.class,
                () -> WebBodies.MAPPER.readValue(
                    WebBodies.body("'extra':'x'"), NewSubmissionRequest.class
                ).toCommand("konto", symbol -> true),
                "Zgłoszenie bez id nie dało błędu 400"
            ).getMessage(),
            "Zły komunikat błędu"
        );
    }

    @Test
    void takesPathIdAndIgnoresBodyIdInUpdateRequest() {
        Assertions.assertEquals(
            new SubmissionEdit(WebBodies.PATH, 3, WebBodies.content()),
            WebBodies.MAPPER.readValue(
                WebBodies.body(String.format("'version':3,'id':'%s'", WebBodies.ID)),
                SubmissionUpdateRequest.class
            ).toCommand(WebBodies.PATH, symbol -> true),
            "Komenda edycji bez identyfikatora z adresu, wersji albo treści"
        );
    }

    @Test
    void rejectsUpdateWithoutVersion() {
        Assertions.assertThrows(
            InvalidInput.class,
            () -> WebBodies.MAPPER.readValue(
                WebBodies.body("'extra':'x'"), SubmissionUpdateRequest.class
            ).toCommand(WebBodies.PATH, symbol -> true),
            "Edycja bez wersji nie dała błędu 400"
        );
    }

    @Test
    void ignoresUnknownFields() {
        final String extra = "'extra':'x'";
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                WebBodies.ID,
                WebBodies.MAPPER.readValue(
                    WebBodies.newBody(extra),
                    NewSubmissionRequest.class
                ).toCommand("konto", symbol -> true).id(),
                "Nieznane pole zepsuło wiązanie POST"
            ),
            () -> Assertions.assertEquals(
                2,
                WebBodies.MAPPER.readValue(
                    WebBodies.body(String.format("'version':2,%s", extra)),
                    SubmissionUpdateRequest.class
                ).toCommand(WebBodies.PATH, symbol -> true).expectedVersion(),
                "Nieznane pole zepsuło wiązanie PUT"
            )
        );
    }

    @Test
    void rejectsNullAndCoercionInNewRequest() {
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
    void rejectsNullAndCoercionInUpdateRequest() {
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

    @Test
    void rejectsSubmissionIdThatIsNoUuid() {
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
    void rejectsBodyThatIsNoObject() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> WebBodies.MAPPER.readValue("[]", NewSubmissionRequest.class),
            "Tablica JSON przeszła jako zgłoszenie"
        );
    }
}
