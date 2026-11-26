/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import io.github.dostepnykampus.identity.api.AccountId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Pseudonim konta: stały dla jednego konta, bez identyfikatora konta.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
final class AccountIdTest {

    @Test
    void givesNumberedPseudonym() {
        Assertions.assertEquals(
            "Autor 819", new AccountId("konto-testowe").pseudonym(),
            "Pseudonim nie jest numerem wyliczonym z identyfikatora konta"
        );
    }

    @Test
    void keepsPseudonymOfOneAccount() {
        Assertions.assertEquals(
            new AccountId("autor").pseudonym(), new AccountId("autor").pseudonym(),
            "To samo konto dostało dwa różne pseudonimy"
        );
    }

    @Test
    void startsNumbersFromOne() {
        Assertions.assertEquals(
            "Autor 1", new AccountId("").pseudonym(),
            "Numer pseudonimu nie zaczyna się od jedynki"
        );
    }

    @Test
    void rejectsMissingAccount() {
        Assertions.assertThrows(
            NullPointerException.class, () -> new AccountId(null),
            "Identyfikator konta bez wartości przeszedł"
        );
    }
}
