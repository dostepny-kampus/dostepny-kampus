/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Parametr mine listy zgłoszeń przyjmuje tylko true i false, a każda wartość inna niż false
 * wymaga roli Zbierającego, zanim serwer zwróci zgłoszenia w każdym statusie.
 *
 * <p>Wymagania: US8.3, NFR-33</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class OwnSubmissionsParameterHttpIT {

    @Test
    void refusesOtherSpellingsOfTrueWithoutCollectorRole(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Konto bez roli Zbierającego dostało listę własnych przez inny zapis true",
            OwnSubmissionsParameterHttpIT.statuses(
                context, "VISITOR", "yes", "on", "1", "TRUE", "True"
            ),
            Matchers.everyItem(Matchers.equalTo(403))
        );
    }

    @Test
    void refusesOtherSpellingsOfTrueFromCollector(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Wartość mine inna niż true i false nie dała 400",
            OwnSubmissionsParameterHttpIT.statuses(context, "COLLECTOR", "yes", "on", "1", "TRUE"),
            Matchers.everyItem(Matchers.equalTo(400))
        );
    }

    @Test
    void keepsTrueForCollectorAndFalseForEveryone(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "mine=true od Zbierającego albo mine=false od Odwiedzającego nie dało 200",
            List.of(
                OwnSubmissionsParameterHttpIT.status(context, "COLLECTOR", "true"),
                OwnSubmissionsParameterHttpIT.status(context, "VISITOR", "false")
            ),
            Matchers.everyItem(Matchers.equalTo(200))
        );
    }

    private static List<Integer> statuses(
        final WebApplicationContext context, final String role, final String... values
    ) throws Exception {
        final List<Integer> result = new ArrayList<>(values.length);
        for (final String mine : values) {
            result.add(OwnSubmissionsParameterHttpIT.status(context, role, mine));
        }
        return result;
    }

    private static int status(
        final WebApplicationContext context, final String role, final String mine
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/submissions").param("mine", mine)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", role))
        ).andReturn().getResponse().getStatus();
    }
}
