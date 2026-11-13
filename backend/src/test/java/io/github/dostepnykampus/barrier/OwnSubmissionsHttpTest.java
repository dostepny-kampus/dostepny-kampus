/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /submissions z mine=true zwraca Zbierającemu jego własne
 * zgłoszenia w każdym statusie, a osobie bez tej roli odmawia.
 *
 * <p>Wymagania: US8.1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class OwnSubmissionsHttpTest {

    /**
     * Adres listy własnych zgłoszeń.
     */
    private static final String OWN = "/submissions?mine=true";

    @Test
    void usEightListsOnlyOwnInAnyStatus(final WebApplicationContext context) throws Exception {
        final UUID own = OwnSubmissionsHttpTest.add(context, "REJECTED");
        final UUID foreign = OwnSubmissionsHttpTest.add(context, "SENT");
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = 'inne-konto' WHERE id = :id")
            .param("id", foreign).update();
        MatcherAssert.assertThat(
            "Lista własnych nie ma odrzuconego zgłoszenia autora albo ma cudze",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get(OwnSubmissionsHttpTest.OWN).with(new AsCollector())
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString(own.toString()),
                Matchers.not(Matchers.containsString(foreign.toString()))
            )
        );
    }

    @Test
    void usEightRefusesOwnListWithoutCollectorRole(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Własne zgłoszenia bez roli Zbierającego nie dają 403",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get(OwnSubmissionsHttpTest.OWN).with(new AsVerifier())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(403)
        );
    }

    private static UUID add(final WebApplicationContext context, final String status) {
        return new SampleSubmissions(context).add(
            UUID.randomUUID(), status,
            SampleSubmissions.quick(
                new SubmissionPlace(
                    new SubmissionPoint(17.062, 51.108), null, false, "A-1",
                    new SubmissionFloor("A-1-0", null, false)
                ),
                "Próg przy wejściu", UUID.randomUUID()
            )
        );
    }
}
