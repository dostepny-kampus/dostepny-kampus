/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Typy pól treści zgłoszenia w JSON (US4, US7): nieznany rodzaj, zdjęcie bez
 * UUID, liczba zamiast symbolu budynku i ciało, które nie jest obiektem,
 * kończą wiązanie błędem.
 *
 * @since 0.1.0
 */
final class ContentTypesJsonTest {

    @Test
    void usFourRejectsUnknownKind() {
        JsonBodies.failsBinding(
            "Nieznany rodzaj zgłoszenia przeszedł", node -> node.put("kind", "FAST")
        );
    }

    @Test
    void usFourRejectsPhotoIdThatIsNoUuid() {
        JsonBodies.failsBinding(
            "Zdjęcie bez UUID przeszło",
            node -> node.set("photoIds", JsonBodies.json("['foto']"))
        );
    }

    @Test
    void usSevenRejectsNumericBuildingSymbol() {
        JsonBodies.failsBinding(
            "Liczba zamiast symbolu budynku przeszła", node -> node.put("buildingSymbol", 13)
        );
    }

    @Test
    void usFourRejectsBodyThatIsNoObject() {
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> JsonBodies.bind(JsonBodies.json("[]")),
            "Tablica JSON przeszła jako zgłoszenie"
        );
    }
}
