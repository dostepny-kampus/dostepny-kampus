/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Wspólne {@code MockMvc} samo sprawdza zgodność odpowiedzi z kontraktem
 * {@code api/openapi.yaml}: test HTTP nie dopisuje tej kontroli.
 *
 * <p>Wymagania: NFR-33</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class MockMvcContractTest {

    @Test
    void nfrThirtyThreeSharedMockMvcRunsSecurityFilters(
        final WebApplicationContext context
    ) throws Exception {
        final MockMvc mvc = context.getBean(MockMvc.class);
        final String body = "{}";
        MatcherAssert.assertThat(
            "Zapis zgłoszenia bez tożsamości nie daje 401 albo tożsamość nie zdejmuje odmowy",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.post("/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                ).andReturn().getResponse().getStatus(),
                mvc.perform(
                    MockMvcRequestBuilders.post("/submissions")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(new AsCollector())
                ).andReturn().getResponse().getStatus()
            ),
            Matchers.contains(Matchers.is(401), Matchers.not(Matchers.oneOf(401, 403)))
        );
    }

    @Test
    void sharedMockMvcRejectsReplyOutsideContract(final WebApplicationContext context) {
        final MockMvc mvc = context.getBean(MockMvc.class);
        MatcherAssert.assertThat(
            "Wspólne MockMvc przepuszcza odpowiedź operacji spoza kontraktu",
            String.valueOf(
                Assertions.assertThrows(
                    AssertionError.class,
                    () -> mvc.perform(MockMvcRequestBuilders.get("/proba-bledow/poza-kontraktem"))
                ).getMessage()
            ),
            Matchers.containsString("validation.request.path.missing")
        );
    }
}
