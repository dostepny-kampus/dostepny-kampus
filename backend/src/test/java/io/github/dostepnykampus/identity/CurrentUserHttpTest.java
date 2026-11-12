/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import io.github.dostepnykampus.IntegrationTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testy US14.1 i BR-1: {@code GET /me} zwraca identyfikator i role z tokenu,
 * a bez tokenu odpowiada 401 jako {@code application/problem+json}.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class CurrentUserHttpTest {

    @Test
    void usFourteenOneReturnsAccountWithEveryRoleFromToken(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Profil nie ma identyfikatora konta albo obu ról z tokenu obok roli Odwiedzającego",
            JsonMapper.shared().readTree(
                context.getBean(MockMvc.class).perform(
                    MockMvcRequestBuilders.get("/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR+VERIFIER")
                ).andReturn().getResponse().getContentAsString()
            ),
            Matchers.equalTo(
                JsonMapper.shared().readTree(
                    String.join(
                        "",
                        "{\"id\":\"konto-testowe\",\"displayName\":\"Konto testowe\",",
                        "\"email\":\"konto@example.com\",",
                        "\"roles\":[\"VISITOR\",\"COLLECTOR\",\"VERIFIER\"]}"
                    )
                )
            )
        );
    }

    @Test
    void usFourteenOneRefusesProfileWithoutToken(final WebApplicationContext context)
        throws Exception {
        final MockHttpServletResponse reply = context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/me")).andReturn().getResponse();
        MatcherAssert.assertThat(
            "Profil bez tokenu nie daje 401 application/problem+json",
            "%d %s".formatted(reply.getStatus(), reply.getContentType()),
            Matchers.startsWith("401 application/problem+json")
        );
    }
}
