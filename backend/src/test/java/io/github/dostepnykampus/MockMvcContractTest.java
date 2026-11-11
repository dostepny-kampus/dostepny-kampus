/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Wspólne {@code MockMvc} samo sprawdza zgodność odpowiedzi z kontraktem
 * {@code api/openapi.yaml} (NFR-33): test HTTP nie dopisuje tej kontroli.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class MockMvcContractTest {

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
            Matchers.containsString("brak operacji w kontrakcie")
        );
    }
}
