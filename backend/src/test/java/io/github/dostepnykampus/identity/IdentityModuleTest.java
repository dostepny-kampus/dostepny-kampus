/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import io.github.dostepnykampus.ModuleBeans;
import io.github.dostepnykampus.ModuleTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Moduł tożsamości uruchamia się bez modułów zgłoszeń i budynków i odpowiada na
 * {@code GET /me}.
 *
 * <p>Wymagania: US14.1</p>
 *
 * @since 0.1.0
 */
@ModuleTest
final class IdentityModuleTest {

    @Test
    void startsWithoutOtherModules(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontekst modułu tożsamości ma beany innego modułu",
            new ModuleBeans(context).in("barrier", "building"),
            Matchers.empty()
        );
    }

    @Test
    void usFourteenOneAnswersProfileInModuleContext(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Moduł tożsamości bez innych modułów nie zwraca profilu",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get("/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR")
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }
}
