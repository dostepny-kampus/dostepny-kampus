/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Testy I2b-2 (NFR-34, BR-7, BR-15): błędy zapisu zdjęcia (403, 404, 400, 413)
 * to {@code application/problem+json} z {@code status} i {@code detail}.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Import(RustfsTestConfiguration.class)
@Transactional
final class SubmissionPhotosProblemHttpTest {

    /**
     * Zdjęcie z listy photoIds zgłoszeń testowych.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void iTwoBTwoExplainsLockedPhoto(final WebApplicationContext context) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        MatcherAssert.assertThat(
            "Zmiana zdjęcia zgłoszenia przyjętego nie daje 403 problem+json z opisem",
            SubmissionPhotosProblemHttpTest.problem(
                requests.put(
                    requests.add("ACCEPTED", SubmissionPhotosProblemHttpTest.PHOTO.toString()),
                    SubmissionPhotosProblemHttpTest.PHOTO,
                    JpegCleanerTest.photo()
                ),
                context
            ),
            Matchers.equalTo(
                "403 application/problem+json 403 "
                    + "Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator"
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void iTwoBTwoHidesInternalsOfMissingPhoto(
        final boolean exists, final WebApplicationContext context
    ) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        UUID id = UUID.randomUUID();
        if (exists) {
            id = requests.add("SENT", UUID.randomUUID().toString());
        }
        MatcherAssert.assertThat(
            "Brak zgłoszenia albo zdjęcia na liście nie daje 404 problem+json z opisem",
            SubmissionPhotosProblemHttpTest.problem(
                requests.put(id, SubmissionPhotosProblemHttpTest.PHOTO, JpegCleanerTest.photo()),
                context
            ),
            Matchers.matchesPattern(
                "404 application/problem\\+json 404 "
                    + "(?!.*(Exception|SELECT|submissions|io\\.github)).+"
            )
        );
    }

    @Test
    void iTwoBTwoExplainsBrokenJpeg(final WebApplicationContext context) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        MatcherAssert.assertThat(
            "Uszkodzony JPEG nie daje 400 problem+json z opisem",
            SubmissionPhotosProblemHttpTest.problem(
                requests.put(
                    requests.add("SENT", SubmissionPhotosProblemHttpTest.PHOTO.toString()),
                    SubmissionPhotosProblemHttpTest.PHOTO,
                    "GIF89a".getBytes(StandardCharsets.ISO_8859_1)
                ),
                context
            ),
            Matchers.matchesPattern("400 application/problem\\+json 400 .+")
        );
    }

    @Test
    void iTwoBTwoExplainsTooLargePhoto(final WebApplicationContext context) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        MatcherAssert.assertThat(
            "Zdjęcie ponad limit nie daje 413 problem+json z opisem",
            SubmissionPhotosProblemHttpTest.problem(
                requests.put(
                    requests.add("SENT", SubmissionPhotosProblemHttpTest.PHOTO.toString()),
                    SubmissionPhotosProblemHttpTest.PHOTO,
                    new byte[5_000_001]
                ),
                context
            ),
            Matchers.matchesPattern("413 application/problem\\+json 413 .+")
        );
    }

    private static String problem(
        final MockHttpServletResponse response, final WebApplicationContext context
    ) throws Exception {
        final JsonNode body = context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
        return "%d %s %s %s".formatted(
            response.getStatus(), response.getContentType(),
            body.path("status").asString(), body.path("detail").asString()
        );
    }
}
