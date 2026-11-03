/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Punkt niezgodny z kontraktem zgłoszenia daje HTTP 400 niezależnie od
 * wewnętrznego typu błędu (US4, US7).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class SubmissionPointHttpTest {

    @Test
    void usFourRejectsInvalidPointAtHttpBoundary(final WebApplicationContext context) {
        Assertions.assertAll(
            Stream.of(
                "{\"type\":\"Point\",\"coordinates\":[181,0]}",
                "{\"type\":\"Point\",\"coordinates\":[0,91]}",
                "{\"type\":\"Point\",\"coordinates\":[\"bad\",0]}",
                "{\"type\":\"Point\",\"coordinates\":[0]}",
                "{\"type\":\"LineString\",\"coordinates\":[0,0]}"
            ).map(
                location -> () -> Assertions.assertEquals(
                    400, SubmissionPointHttpTest.status(context, location),
                    "Niepoprawny punkt nie został odrzucony jako HTTP 400"
                )
            )
        );
    }

    private static int status(final WebApplicationContext context, final String location)
        throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).build().perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"%s","kind":"QUICK","barrierGroup":"OTHER",
                    "location":%s,"photoIds":["5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"]}
                    """.formatted(UUID.randomUUID(), location)
                )
        ).andReturn().getResponse().getStatus();
    }
}
