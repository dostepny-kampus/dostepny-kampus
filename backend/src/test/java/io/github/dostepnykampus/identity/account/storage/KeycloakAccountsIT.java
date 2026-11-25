/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.storage;

import io.github.dostepnykampus.KeycloakTestConfiguration;
import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.RustfsTestConfiguration;
import io.github.dostepnykampus.identity.account.Accounts;
import io.github.dostepnykampus.identity.account.Grant;
import io.github.dostepnykampus.identity.account.StaffRole;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;

/**
 * Konto techniczne serwera nadaje role w prawdziwym Keycloaku z realmem deweloperskim: zakłada
 * konto, dokłada rolę istniejącemu kontu i rozpoznaje rolę, którą konto już ma.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@Tag("kontekst-springa")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(
    {
        PostgisTestConfiguration.class,
        RustfsTestConfiguration.class,
        KeycloakTestConfiguration.class
    }
)
final class KeycloakAccountsIT {

    @Test
    void createsAccountThenAddsRoleOnce(final WebApplicationContext context) {
        final Accounts accounts = context.getBean(Accounts.class);
        final String email = "zaproszona-%s@example.test".formatted(UUID.randomUUID());
        MatcherAssert.assertThat(
            "Nowe konto, dodana rola i powtórzone zaproszenie dały złe wyniki",
            List.of(
                accounts.grant(email, StaffRole.COLLECTOR),
                accounts.grant(email, StaffRole.VERIFIER),
                accounts.grant(email, StaffRole.VERIFIER),
                accounts.grant(email, StaffRole.COLLECTOR)
            ),
            Matchers.contains(Grant.CREATED, Grant.ADDED, Grant.UNCHANGED, Grant.UNCHANGED)
        );
    }
}
