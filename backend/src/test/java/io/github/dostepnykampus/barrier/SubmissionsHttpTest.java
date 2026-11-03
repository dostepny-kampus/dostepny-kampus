/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US4, US7 i BR-9: POST /submissions zapisuje zgłoszenie ze statusem
 * „wysłane”, a budynek zgłoszenia szybkiego ustala z konturu (BR-14).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
@Transactional
final class SubmissionsHttpTest {

    /**
     * Współrzędne GeoJSON punktu w konturze A-1: długość, szerokość.
     */
    private static final String IN_A1 = "17.06209,51.10785";

    @Test
    void usFourSavesSubmissionAsSent(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Nowe zgłoszenie nie ma statusu „wysłane”",
            SubmissionsHttpTest.quick(context).path("status").asString(),
            Matchers.equalTo("SENT")
        );
    }

    @Test
    void brFourteenTakesBuildingFromOutline(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie szybkie w konturze A-1 ma budynek z telefonu",
            SubmissionsHttpTest.quick(context).path("buildingSymbol").asString(),
            Matchers.equalTo("A-1")
        );
    }

    @Test
    void brNineReturnsSavedSubmissionOnRetry(final WebApplicationContext context)
        throws Exception {
        final String body = SubmissionsHttpTest.body("QUICK", SubmissionsHttpTest.IN_A1);
        SubmissionsHttpTest.post(context, body);
        MatcherAssert.assertThat(
            "Ponowne wysłanie tego samego id nie zwraca 200",
            SubmissionsHttpTest.post(context, body).getStatus(),
            Matchers.equalTo(200)
        );
    }

    @Test
    void usSevenRejectsUnknownBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Pomiar precyzyjny w nieznanym budynku nie daje 400",
            SubmissionsHttpTest.post(
                context, SubmissionsHttpTest.body("PRECISE", "17.0,51.0")
            ).getStatus(),
            Matchers.equalTo(400)
        );
    }

    private static MockHttpServletResponse post(
        final WebApplicationContext context, final String body
    ) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build().perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andReturn().getResponse();
    }

    private static JsonNode quick(final WebApplicationContext context) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            SubmissionsHttpTest.post(
                context, SubmissionsHttpTest.body("QUICK", SubmissionsHttpTest.IN_A1)
            ).getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private static String body(final String kind, final String point) {
        return String.format(
            String.join(
                "",
                "{\"id\":\"%s\",\"kind\":\"%s\",",
                "\"location\":{\"type\":\"Point\",\"coordinates\":[%s]},",
                "\"buildingSymbol\":\"NIE-MA\",\"barrierGroup\":\"OTHER\",",
                "\"photoIds\":[\"%s\"]}"
            ),
            UUID.randomUUID(), kind, point, UUID.randomUUID()
        );
    }
}
