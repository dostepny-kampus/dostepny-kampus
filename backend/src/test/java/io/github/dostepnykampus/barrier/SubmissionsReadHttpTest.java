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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy BR-6, US1.3 i US2.2: bez logowania GET /submissions pokazuje tylko
 * zgłoszenia przyjęte, a własne zgłoszenia wymagają logowania (US8.1).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
@Transactional
final class SubmissionsReadHttpTest {

    @Test
    void brSixShowsAcceptedSubmission(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie przyjęte nie jest widoczne bez logowania",
            SubmissionsReadHttpTest.read(context, "ACCEPTED"),
            Matchers.equalTo(200)
        );
    }

    @Test
    void brSixHidesSentSubmission(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie wysłane jest widoczne bez logowania",
            SubmissionsReadHttpTest.read(context, "SENT"),
            Matchers.equalTo(404)
        );
    }

    @Test
    void usTwoListsAcceptedOfBuilding(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Lista zgłoszeń A-1 nie ma zgłoszenia przyjętego w A-1",
            SubmissionsReadHttpTest.listed(context, "buildingSymbol=A-1"),
            Matchers.greaterThan(0)
        );
    }

    @Test
    void usTwoSkipsOtherBuilding(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Lista zgłoszeń C-13 ma zgłoszenie z A-1",
            SubmissionsReadHttpTest.listed(context, "buildingSymbol=C-13"),
            Matchers.equalTo(0)
        );
    }

    @Test
    void usEightAllowsOnlyAcceptedStatus(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Status SENT bez logowania nie daje 400",
            SubmissionsReadHttpTest.get(context, "/submissions?status=SENT").getStatus(),
            Matchers.equalTo(400)
        );
    }

    @Test
    void usEightNeedsLoginForOwn(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Własne zgłoszenia bez logowania nie dają 401",
            SubmissionsReadHttpTest.get(context, "/submissions?mine=true").getStatus(),
            Matchers.equalTo(401)
        );
    }

    private static int read(final WebApplicationContext context, final String status)
        throws Exception {
        return SubmissionsReadHttpTest.get(
            context, "/submissions/%s".formatted(SubmissionsReadHttpTest.add(context, status))
        ).getStatus();
    }

    private static int listed(final WebApplicationContext context, final String query)
        throws Exception {
        SubmissionsReadHttpTest.add(context, "ACCEPTED");
        return context.getBean(ObjectMapper.class).readTree(
            SubmissionsReadHttpTest.get(context, "/submissions?".concat(query))
                .getContentAsString(StandardCharsets.UTF_8)
        ).size();
    }

    private static UUID add(final WebApplicationContext context, final String status) {
        final UUID id = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, status, kind, building_symbol, location, content)
            VALUES (:id, :status, 'QUICK', 'A-1',
                ST_SetSRID(ST_MakePoint(17.062, 51.108), 4326), '{"kind":"QUICK"}')
            """
        ).param("id", id).param("status", status).update();
        return id;
    }

    private static MockHttpServletResponse get(
        final WebApplicationContext context, final String uri
    ) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build()
            .perform(MockMvcRequestBuilders.get(uri))
            .andReturn()
            .getResponse();
    }
}
