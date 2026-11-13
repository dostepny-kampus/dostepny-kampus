/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * POST /submissions zapisuje zgłoszenie ze statusem
 * „wysłane”, a budynek zgłoszenia szybkiego ustala z konturu.
 * Bez transakcji testowej, jak na produkcji; wiersze usuwane po teście.
 *
 * <p>Wymagania: BR-14, US4, US7, BR-9</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SubmissionsHttpTest {

    /**
     * Współrzędne GeoJSON punktu w konturze A-1: długość, szerokość.
     */
    private static final String IN_A1 = "17.06209,51.10785";

    /**
     * Wiersz zarezerwowany dla sekwencyjnych scenariuszy tej klasy, usuwany po każdym teście.
     */
    private static final UUID ID = UUID.fromString("8f1d5a52-6c0e-4b43-9a57-3f2d7c1e9b60");

    @AfterEach
    void clean(final WebApplicationContext context) {
        context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id = :id")
            .param("id", SubmissionsHttpTest.ID).update();
    }

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
    void brThreeSavesAccountAsAuthor(final WebApplicationContext context)
        throws Exception {
        SubmissionsHttpTest.quick(context);
        MatcherAssert.assertThat(
            "Zgłoszenie nie ma autora z tokenu",
            context.getBean(JdbcClient.class)
                .sql("SELECT author FROM submissions WHERE id = :id")
                .param("id", SubmissionsHttpTest.ID).query(String.class).single(),
            Matchers.equalTo("konto-testowe")
        );
    }

    /**
     * Ponowiony POST zwraca zapisane zgłoszenie bez drugiego wiersza.
     * Scenariusz (częściowo): synchronizacja.feature: Przerwana synchronizacja
     * nie gubi zgłoszenia i nie tworzy drugiego
     *
     * <p>Wymagania: BR-9</p>
     *
     * @param context Kontekst aplikacji z bazą danych
     * @throws Exception Gdy nie uda się wykonać żądania HTTP
     */
    @Test
    void brNineReturnsSavedSubmissionOnRetry(final WebApplicationContext context)
        throws Exception {
        final String body = SubmissionsHttpTest.body("QUICK", SubmissionsHttpTest.IN_A1);
        final SubmissionRequests requests = new SubmissionRequests(context);
        final MockHttpServletResponse first = requests.post(body);
        final MockHttpServletResponse retry = requests.post(body);
        MatcherAssert.assertThat(
            "Ponowienie zmieniło zapisane zgłoszenie albo utworzyło duplikat",
            List.of(
                first.getStatus(), retry.getStatus(),
                retry.getContentAsString(StandardCharsets.UTF_8),
                context.getBean(JdbcClient.class)
                    .sql("SELECT count(*) FROM submissions WHERE id = :id")
                    .param("id", SubmissionsHttpTest.ID).query(Integer.class).single()
            ),
            Matchers.equalTo(
                List.of(201, 200, first.getContentAsString(StandardCharsets.UTF_8), 1)
            )
        );
    }

    @Test
    void usSevenRejectsUnknownBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Pomiar precyzyjny w nieznanym budynku nie daje 400 albo zostaje zapisany",
            List.of(
                new SubmissionRequests(context).post(
                    SubmissionsHttpTest.body("PRECISE", "17.05,51.108")
                ).getStatus(),
                context.getBean(JdbcClient.class)
                    .sql("SELECT count(*) FROM submissions WHERE id = :id")
                    .param("id", SubmissionsHttpTest.ID).query(Integer.class).single()
            ),
            Matchers.equalTo(List.of(400, 0))
        );
    }

    private static JsonNode quick(final WebApplicationContext context) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            new SubmissionRequests(context).post(
                SubmissionsHttpTest.body("QUICK", SubmissionsHttpTest.IN_A1)
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
                "\"barrierType\":\"TB-1\",\"severity\":2,\"floor\":{\"number\":0},",
                "\"photoIds\":[\"%s\"]}"
            ),
            SubmissionsHttpTest.ID, kind, point, UUID.randomUUID()
        );
    }
}
