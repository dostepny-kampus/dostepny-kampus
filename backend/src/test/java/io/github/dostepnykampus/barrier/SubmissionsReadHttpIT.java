/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bez logowania GET /submissions pokazuje tylko
 * zgłoszenia przyjęte w kształcie schematu Submission, a własne zgłoszenia
 * wymagają logowania.
 *
 * <p>Wymagania: US8.1, BR-6, US1.3, US2.2</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionsReadHttpIT {

    /**
     * Chwila odbioru zgłoszeń z tej klasy.
     */
    private static final String CREATED = "2026-11-04T10:00:00Z";

    /**
     * Chwila przyjęcia zgłoszeń przyjętych z tej klasy.
     */
    private static final String VERIFIED = "2026-11-05T09:30:00Z";

    /**
     * Piętro zgłoszeń wyszukiwanych w testach listy.
     */
    private static final String FLOOR = "A-1-0";

    @Test
    void showsAcceptedSubmission(final WebApplicationContext context) throws Exception {
        final Row row = SubmissionsReadHttpIT.add(
            context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
        );
        final MockHttpServletResponse response = SubmissionsReadHttpIT.get(
            context, "/submissions/%s".formatted(row.id())
        );
        MatcherAssert.assertThat(
            "Zgłoszenie przyjęte nie jest widoczne bez logowania w kształcie Submission",
            List.of(response.getStatus(), SubmissionsReadHttpIT.json(context, response)),
            Matchers.equalTo(List.of(200, row.expected(context)))
        );
    }

    @Test
    void hidesSentSubmission(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie wysłane jest widoczne bez logowania",
            SubmissionsReadHttpIT.read(context, "SENT"),
            Matchers.equalTo(404)
        );
    }

    @Test
    void listsOnlyAcceptedOfBuilding(final WebApplicationContext context)
        throws Exception {
        final Row accepted = SubmissionsReadHttpIT.add(
            context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
        );
        MatcherAssert.assertThat(
            "Lista zgłoszeń A-1 nie ma samego zgłoszenia przyjętego z A-1",
            SubmissionsReadHttpIT.listed(
                context, "buildingSymbol=A-1",
                List.of(
                    accepted,
                    SubmissionsReadHttpIT.add(context, "SENT", SubmissionsReadHttpIT.FLOOR)
                )
            ),
            Matchers.equalTo(List.of(accepted.expected(context)))
        );
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void listsReadableSubmissionsAndLogsCorruptRow(
        final WebApplicationContext context, final CapturedOutput output
    ) throws Exception {
        final Row valid = SubmissionsReadHttpIT.add(
            context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
        );
        final Row corrupt = SubmissionsReadHttpIT.add(
            context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
        );
        context.getBean(JdbcClient.class).sql(
            "UPDATE submissions SET content = content || '{\"photoIds\": \"zepsute\"}' WHERE id = :id"
        ).param("id", corrupt.id()).update();
        final MockHttpServletResponse response = SubmissionsReadHttpIT.get(
            context, "/submissions?buildingSymbol=A-1"
        );
        MatcherAssert.assertThat(
            "Lista zgłoszeń albo diagnostyka uszkodzonego wiersza jest niepoprawna",
            List.of(
                response.getStatus(), SubmissionsReadHttpIT.json(context, response).size(),
                SubmissionsReadHttpIT.json(context, response).path(0),
                output.getOut().contains(
                    "Pominięto nieczytelne zgłoszenie %s".formatted(corrupt.id())
                )
            ),
            Matchers.equalTo(List.of(200, 1, valid.expected(context), true))
        );
    }

    @Test
    void listsOnlyRequestedFloor(final WebApplicationContext context) throws Exception {
        final Row floor = SubmissionsReadHttpIT.add(
            context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
        );
        MatcherAssert.assertThat(
            "Lista zgłoszeń piętra ma zgłoszenie z innego piętra albo nie ma swojego",
            SubmissionsReadHttpIT.listed(
                context, "buildingSymbol=A-1&floorId=".concat(SubmissionsReadHttpIT.FLOOR),
                List.of(floor, SubmissionsReadHttpIT.add(context, "ACCEPTED", "A-1-1"))
            ),
            Matchers.equalTo(List.of(floor.expected(context)))
        );
    }

    @Test
    void skipsOtherBuilding(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Lista zgłoszeń C-13 ma zgłoszenie z A-1",
            SubmissionsReadHttpIT.listed(
                context, "buildingSymbol=C-13",
                List.of(
                    SubmissionsReadHttpIT.add(
                        context, "ACCEPTED", SubmissionsReadHttpIT.FLOOR
                    )
                )
            ),
            Matchers.empty()
        );
    }

    @Test
    void allowsOnlyAcceptedStatus(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Status SENT bez logowania nie daje 400",
            SubmissionsReadHttpIT.get(context, "/submissions?status=SENT").getStatus(),
            Matchers.equalTo(400)
        );
    }

    @Test
    void needsLoginForOwn(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Własne zgłoszenia bez logowania nie dają 401",
            SubmissionsReadHttpIT.get(context, "/submissions?mine=true").getStatus(),
            Matchers.equalTo(401)
        );
    }

    private static int read(final WebApplicationContext context, final String status)
        throws Exception {
        return SubmissionsReadHttpIT.get(
            context, "/submissions/%s".formatted(
                SubmissionsReadHttpIT.add(context, status, SubmissionsReadHttpIT.FLOOR).id()
            )
        ).getStatus();
    }

    private static List<JsonNode> listed(
        final WebApplicationContext context, final String query, final List<Row> rows
    ) throws Exception {
        final List<String> ids = rows.stream().map(row -> row.id().toString()).toList();
        final List<JsonNode> own = new ArrayList<>(rows.size());
        for (final JsonNode item : SubmissionsReadHttpIT.json(
            context, SubmissionsReadHttpIT.get(context, "/submissions?".concat(query))
        )) {
            if (ids.contains(item.path("id").asString())) {
                own.add(item);
            }
        }
        return own;
    }

    private static Row add(
        final WebApplicationContext context, final String status, final String floor
    ) {
        final Row row = new Row(UUID.randomUUID(), status, floor, UUID.randomUUID());
        new SampleSubmissions(context).add(
            row.id(), status,
            SampleSubmissions.quick(
                new SubmissionPlace(
                    new SubmissionPoint(17.062, 51.108), null, false, "A-1",
                    new SubmissionFloor(floor, null, false)
                ),
                "Próg przy wejściu", row.photo()
            )
        );
        context.getBean(JdbcClient.class).sql(
            """
            UPDATE submissions SET created_at = CAST(:created AS TIMESTAMPTZ),
                verified_at = CASE WHEN status = 'ACCEPTED'
                    THEN CAST(:verified AS TIMESTAMPTZ) END
            WHERE id = :id
            """
        ).param("created", SubmissionsReadHttpIT.CREATED)
            .param("verified", SubmissionsReadHttpIT.VERIFIED).param("id", row.id()).update();
        return row;
    }

    private static JsonNode json(
        final WebApplicationContext context, final MockHttpServletResponse response
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private static MockHttpServletResponse get(
        final WebApplicationContext context, final String uri
    ) throws Exception {
        return context.getBean(MockMvc.class)
            .perform(MockMvcRequestBuilders.get(uri))
            .andReturn()
            .getResponse();
    }

    /**
     * Zgłoszenie dodane do bazy przed odczytem.
     *
     * @param id Identyfikator zgłoszenia
     * @param status Status zgłoszenia
     * @param floor Piętro budynku szczegółowego
     * @param photo Identyfikator zdjęcia
     * @since 0.1.0
     */
    private record Row(UUID id, String status, String floor, UUID photo) {

        /**
         * Treść według schematu SubmissionContent bez zamykającego nawiasu:
         * piętro i identyfikator zdjęcia.
         */
        private static final String CONTENT = String.join(
            "",
            "{\"kind\":\"QUICK\",\"location\":{\"type\":\"Point\",",
            "\"coordinates\":[17.062,51.108]},\"buildingSymbol\":\"A-1\",",
            "\"floor\":{\"floorId\":\"%s\"},\"barrierGroup\":\"OTHER\",",
            "\"description\":\"Próg przy wejściu\",\"photoIds\":[\"%s\"]"
        );

        /**
         * Treść zgłoszenia bez zamykającego nawiasu.
         *
         * @return Początek obiektu SubmissionContent
         */
        String content() {
            return Row.CONTENT.formatted(this.floor, this.photo);
        }

        /**
         * Zgłoszenie przyjęte, jakie według schematu Submission zwraca serwer.
         *
         * @param context Kontekst z ObjectMapper
         * @return Treść z polami nadanymi przez serwer
         */
        JsonNode expected(final WebApplicationContext context) {
            return context.getBean(ObjectMapper.class).readTree(
                String.join(
                    "",
                    "%s,\"id\":\"%s\",\"version\":1,\"status\":\"%s\",",
                    "\"createdAt\":\"%s\",\"verifiedAt\":\"%s\"}"
                ).formatted(
                    this.content(), this.id, this.status, SubmissionsReadHttpIT.CREATED,
                    SubmissionsReadHttpIT.VERIFIED
                )
            );
        }
    }
}
