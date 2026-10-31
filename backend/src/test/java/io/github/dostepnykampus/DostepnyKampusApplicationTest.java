/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Test zadania INFRA: szkielet serwera podnosi kontekst Springa.
 *
 * <p>Kontekst wymaga bazy, więc test używa {@link PostgisTestConfiguration}.</p>
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
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
