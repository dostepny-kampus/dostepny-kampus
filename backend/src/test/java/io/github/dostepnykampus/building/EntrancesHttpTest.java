/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US2 i US17.1: karta budynku GET /buildings/{symbol} ma wejścia
 * z bazy w kształcie schematu Entrance.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class EntrancesHttpTest {

    @Test
    void usTwoShowsEntranceMarking(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Oznaczenie wejścia B-8 nie jest polem wejścia na karcie",
            context.getBean(ObjectMapper.class)
                .readTree(EntrancesHttpTest.card(context, "B-8"))
                .at("/entrances/0/marking").asString(),
            Matchers.equalTo("INACCESSIBLE")
        );
    }

    @Test
    void usTwoOmitsMissingEntranceName(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Wejście bez opisu w danych ma pole name na karcie",
            context.getBean(ObjectMapper.class)
                .readTree(EntrancesHttpTest.card(context, "B-8"))
                .at("/entrances/0").has("name"),
            Matchers.is(false)
        );
    }

    @Test
    void usTwoShowsAllEntrancesOfBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Karta P-23 nie ma 12 wejść z pliku konturów",
            context.getBean(ObjectMapper.class)
                .readTree(EntrancesHttpTest.card(context, "P-23"))
                .required("entrances").size(),
            Matchers.equalTo(12)
        );
    }

    private static String card(final WebApplicationContext context, final String symbol)
        throws Exception {
        return context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get("/buildings/{symbol}", symbol))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }
}
