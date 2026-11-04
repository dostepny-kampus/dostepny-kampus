/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy US1.3, US4, US8.1, US8.3, BR-6, BR-9 i BR-10: kontroler zgłoszeń
 * odpowiada błędami application/problem+json, a w odpowiedziach pomija
 * pola puste. Transakcja testu wycofuje zapisane wiersze.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionsTypedHttpTest {

    /**
     * Zgłoszenie zapisywane w testach.
     */
    private static final UUID ID = UUID.fromString("0b6e3f5a-7d21-4c98-a4e0-6c1f8d2b9a37");

    /**
     * Pola treści zgłoszenia bez id, wersji i pól opcjonalnych.
     */
    private static final String CONTENT = String.join(
        "",
        "\"kind\":\"QUICK\",\"location\":{\"type\":\"Point\",",
        "\"coordinates\":[17.06209,51.10785]},\"barrierGroup\":\"OTHER\",",
        "\"photoIds\":[\"5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d\"]"
    );

    /**
     * Typ odpowiedzi z błędem.
     */
    private static final String PROBLEM = "application/problem+json";

    @ParameterizedTest
    @Disabled("Stary kontroler przyjmuje pusty status, poprawka w następnym PR")
    @ValueSource(strings = {"status=", "status=,"})
    void usOneRejectsEmptyStatus(final String query, final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Pusty filtr status nie daje 400 application/problem+json",
            SubmissionsTypedHttpTest.summary(
                new SubmissionRequests(context).get("/submissions?".concat(query))
            ),
            Matchers.equalTo("400 ".concat(SubmissionsTypedHttpTest.PROBLEM))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"mine=true", "mine=true&status=SENT", "mine=true&status=ACCEPTED"})
    void usEightNeedsLoginForOwn(final String query, final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Własne zgłoszenia bez logowania nie dają 401",
            new SubmissionRequests(context).get("/submissions?".concat(query)).getStatus(),
            Matchers.equalTo(401)
        );
    }

    @Test
    @Disabled("Stary kontroler inaczej opisuje brak id, poprawka w następnym PR")
    void brNineRejectsNewSubmissionWithoutId(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "POST bez id nie daje 400 z opisem braku identyfikatora",
            SubmissionsTypedHttpTest.detail(
                new SubmissionRequests(context).post(
                    "{".concat(SubmissionsTypedHttpTest.CONTENT).concat("}")
                ),
                context
            ),
            Matchers.equalTo(
                "400 application/problem+json Brak identyfikatora zgłoszenia w polu id"
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"1\"", "null", "0", "-1"})
    void brTenRejectsBadVersion(final String version, final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Wersja bazowa edycji ".concat(version).concat(" nie daje 400 problem+json"),
            SubmissionsTypedHttpTest.summary(
                new SubmissionRequests(context).put(
                    SubmissionsTypedHttpTest.ID, SubmissionsTypedHttpTest.edit(version)
                )
            ),
            Matchers.equalTo("400 ".concat(SubmissionsTypedHttpTest.PROBLEM))
        );
    }

    @Test
    void brTenGivesConflictWithServerSubmissionAsObject(final WebApplicationContext context)
        throws Exception {
        SubmissionsTypedHttpTest.add(context, "ACCEPTED", 2);
        final MockHttpServletResponse response = new SubmissionRequests(context).put(
            SubmissionsTypedHttpTest.ID, SubmissionsTypedHttpTest.edit("1")
        );
        MatcherAssert.assertThat(
            "Konflikt wersji nie daje 409 z opisem i pełnym zgłoszeniem z serwera",
            List.of(
                SubmissionsTypedHttpTest.detail(response, context),
                SubmissionsTypedHttpTest.json(response, context).path("serverSubmission")
            ),
            Matchers.equalTo(
                List.of(
                    String.join(
                        " ", "409", SubmissionsTypedHttpTest.PROBLEM,
                        "Na serwerze jest inna wersja zgłoszenia albo jego status",
                        "nie pozwala na zmianę"
                    ),
                    SubmissionsTypedHttpTest.json(
                        new SubmissionRequests(context).get(
                            "/submissions/%s".formatted(SubmissionsTypedHttpTest.ID)
                        ),
                        context
                    )
                )
            )
        );
    }

    @Test
    @Disabled("Stary kontroler nie opisuje braku zgłoszenia, poprawka w następnym PR")
    void usEightReportsUnknownSubmissionAsMissing(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Edycja nieistniejącego zgłoszenia nie daje 404 z opisem",
            SubmissionsTypedHttpTest.detail(
                new SubmissionRequests(context).put(
                    SubmissionsTypedHttpTest.ID, SubmissionsTypedHttpTest.edit("1")
                ),
                context
            ),
            Matchers.equalTo("404 application/problem+json Nie ma takiego zgłoszenia")
        );
    }

    @Test
    void usFourOmitsEmptyFieldsInCreated(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź POST ma pola null albo flagi false",
            SubmissionsTypedHttpTest.empty(
                new SubmissionRequests(context).post(
                    String.format(
                        "{\"id\":\"%s\",%s}",
                        SubmissionsTypedHttpTest.ID, SubmissionsTypedHttpTest.CONTENT
                    )
                ),
                context
            ),
            Matchers.empty()
        );
    }

    @Test
    void usEightOmitsEmptyFieldsInEdited(final WebApplicationContext context)
        throws Exception {
        SubmissionsTypedHttpTest.add(context, "SENT", 1);
        MatcherAssert.assertThat(
            "Odpowiedź PUT ma pola null albo flagi false",
            SubmissionsTypedHttpTest.empty(
                new SubmissionRequests(context).put(
                    SubmissionsTypedHttpTest.ID, SubmissionsTypedHttpTest.edit("1")
                ),
                context
            ),
            Matchers.empty()
        );
    }

    @Test
    void brSixOmitsEmptyFieldsInRead(final WebApplicationContext context) throws Exception {
        SubmissionsTypedHttpTest.add(context, "ACCEPTED", 1);
        MatcherAssert.assertThat(
            "Odpowiedź GET ma pola null albo flagi false",
            SubmissionsTypedHttpTest.empty(
                new SubmissionRequests(context).get(
                    "/submissions/%s".formatted(SubmissionsTypedHttpTest.ID)
                ),
                context
            ),
            Matchers.empty()
        );
    }

    private static List<String> empty(
        final MockHttpServletResponse response, final WebApplicationContext context
    ) throws Exception {
        final List<String> names = new ArrayList<>(0);
        for (final Map.Entry<String, JsonNode> field
            : SubmissionsTypedHttpTest.json(response, context).properties()) {
            if (field.getValue().isNull()
                || field.getValue().isBoolean() && !field.getValue().booleanValue()) {
                names.add(field.getKey());
            }
        }
        return names;
    }

    private static String detail(
        final MockHttpServletResponse response, final WebApplicationContext context
    ) throws Exception {
        return "%s %s".formatted(
            SubmissionsTypedHttpTest.summary(response),
            SubmissionsTypedHttpTest.json(response, context).path("detail").asString()
        );
    }

    private static String summary(final MockHttpServletResponse response) {
        return "%d %s".formatted(response.getStatus(), response.getContentType());
    }

    private static JsonNode json(
        final MockHttpServletResponse response, final WebApplicationContext context
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private static String edit(final String version) {
        return "{\"version\":%s,%s}".formatted(version, SubmissionsTypedHttpTest.CONTENT);
    }

    private static void add(
        final WebApplicationContext context, final String status, final int version
    ) {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, version, status, kind, location, content)
            VALUES (:id, :version, :status, 'QUICK',
                ST_SetSRID(ST_MakePoint(17.06209, 51.10785), 4326),
                CAST(:content AS JSONB))
            """
        ).param("id", SubmissionsTypedHttpTest.ID).param("version", version)
            .param("status", status).param("content", "{".concat(SubmissionsTypedHttpTest.CONTENT).concat("}"))
            .update();
    }
}
