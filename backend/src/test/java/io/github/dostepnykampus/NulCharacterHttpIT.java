/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Znak NUL w parametrze zapytania albo w polu tekstowym ciała JSON daje 400, zanim tekst
 * trafi do bazy, także w żądaniu bez logowania.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class NulCharacterHttpIT {

    @Test
    void rejectsNulInQueryParameters(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Znak NUL w parametrze zapytania nie dał 400",
            List.of(
                NulCharacterHttpIT.status(
                    context,
                    MockMvcRequestBuilders.get("/submissions").param("buildingSymbol", "\u0000")
                ),
                NulCharacterHttpIT.status(
                    context, MockMvcRequestBuilders.get("/submissions").param("floorId", "\u0000")
                ),
                NulCharacterHttpIT.status(
                    context, MockMvcRequestBuilders.get("/rooms").param("q", "A\u0000")
                )
            ),
            Matchers.everyItem(Matchers.equalTo(400))
        );
    }

    @Test
    void rejectsNulInJsonText(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Znak NUL w opisie zgłoszenia nie dał 400",
            NulCharacterHttpIT.status(
                context,
                MockMvcRequestBuilders.post("/submissions")
                    .with(new AsCollector())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.join(
                            "",
                            "{\"id\":\"3f2504e0-4f89-11d3-9a0c-0305e82c3301\",\"kind\":\"QUICK\",",
                            "\"barrierGroup\":\"OTHER\",\"description\":\"a\\u0000b\",",
                            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
                            "\"photoIds\":[\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"]}"
                        )
                    )
            ),
            Matchers.equalTo(400)
        );
    }

    private static int status(
        final WebApplicationContext context, final MockHttpServletRequestBuilder request
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(request).andReturn().getResponse()
            .getStatus();
    }
}
