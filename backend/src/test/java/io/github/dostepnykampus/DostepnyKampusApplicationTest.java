/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov
 */
package io.github.dostepnykampus;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Test zadania INFRA: szkielet serwera podnosi kontekst Springa.
 *
 * @since 0.1.0
 */
@SpringBootTest
final class DostepnyKampusApplicationTest {

    @Test
    void infraStartsApplicationContext(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontekst aplikacji nie zarejestrował klasy startowej",
            context.getBeanNamesForType(DostepnyKampusApplication.class),
            Matchers.arrayWithSize(1)
        );
    }
}
