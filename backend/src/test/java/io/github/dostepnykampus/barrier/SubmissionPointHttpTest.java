/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Punkt niezgodny z kontraktem zgłoszenia daje HTTP 400 niezależnie od
 * wewnętrznego typu błędu, przy wysłaniu i przy edycji.
 *
 * <p>Wymagania: US4, US7, US8.3</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionPointHttpTest {

    @Test
    void usFourRejectsInvalidPointAtHttpBoundary(final WebApplicationContext context) {
        Assertions.assertAll(
            Stream.of(
                SubmissionPointHttpTest.point("[181,0]"),
                SubmissionPointHttpTest.point("[0,91]"),
                SubmissionPointHttpTest.point("[\"bad\",0]"),
                SubmissionPointHttpTest.point("[0]"),
                "{\"type\":\"LineString\",\"coordinates\":[0,0]}"
            ).map(
                location -> () -> Assertions.assertEquals(
                    400, SubmissionPointHttpTest.post(context, UUID.randomUUID(), location),
                    "Niepoprawny punkt nie został odrzucony jako HTTP 400"
                )
            )
        );
    }

    @Test
    void usSevenRejectsNonNumericLatitudeAtHttpBoundary(final WebApplicationContext context) {
        final UUID id = UUID.randomUUID();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                400,
                SubmissionPointHttpTest.post(
                    context, id, SubmissionPointHttpTest.point("[0,\"bad\"]")
                ),
                "Szerokość niebędąca liczbą nie została odrzucona jako HTTP 400"
            ),
            () -> Assertions.assertEquals(
                0,
                context.getBean(JdbcClient.class)
                    .sql("SELECT count(*) FROM submissions WHERE id = :id")
                    .param("id", id).query(Integer.class).single(),
                "Odrzucone zgłoszenie zostało zapisane"
            )
        );
    }

    @Test
    void usEightRejectsInvalidPointOnEdit(final WebApplicationContext context)
        throws Exception {
        final UUID id = UUID.randomUUID();
        SubmissionPointHttpTest.post(context, id, SubmissionPointHttpTest.point("[17.05,51.108]"));
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                400, SubmissionPointHttpTest.put(context, id, "[181,0]"), "PUT [181,0] bez 400"
            ),
            () -> Assertions.assertEquals(
                400, SubmissionPointHttpTest.put(context, id, "[0,91]"), "PUT [0,91] bez 400"
            ),
            () -> Assertions.assertEquals(
                1,
                context.getBean(JdbcClient.class)
                    .sql("SELECT version FROM submissions WHERE id = :id")
                    .param("id", id).query(Integer.class).single(),
                "Odrzucona edycja zmieniła wersję zgłoszenia"
            )
        );
    }

    private static String point(final String coordinates) {
        return String.format("{\"type\":\"Point\",\"coordinates\":%s}", coordinates);
    }

    private static int post(
        final WebApplicationContext context, final UUID id, final String location
    ) throws Exception {
        return SubmissionPointHttpTest.status(
            context, MockMvcRequestBuilders.post("/submissions"),
            String.format("\"id\":\"%s\",\"location\":%s", id, location)
        );
    }

    private static int put(final WebApplicationContext context, final UUID id, final String place)
        throws Exception {
        return SubmissionPointHttpTest.status(
            context, MockMvcRequestBuilders.put("/submissions/{id}", id),
            String.format("\"version\":1,\"location\":%s", SubmissionPointHttpTest.point(place))
        );
    }

    private static int status(
        final WebApplicationContext context, final MockHttpServletRequestBuilder request,
        final String fields
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            request.contentType(MediaType.APPLICATION_JSON).with(new AsCollector()).content(
                String.format(
                    "{%s,\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",\"photoIds\":[\"%s\"]}",
                    fields, "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
                )
            )
        ).andReturn().getResponse().getStatus();
    }
}
