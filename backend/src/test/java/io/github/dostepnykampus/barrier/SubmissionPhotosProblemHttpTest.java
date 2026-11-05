/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test I2b-2 (NFR-34, BR-7, BR-15): błędy zapisu zdjęcia (403, 404, 400, 413)
 * to {@code application/problem+json} z {@code status} i {@code detail}.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionPhotosProblemHttpTest {

    /** Zdjęcie z listy photoIds. */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /** Oczekiwany wzorzec "kod typ status detail" dla każdego przypadku. */
    private static final Map<String, String> EXPECTED = Map.of(
        "locked",
        String.join(
            " ",
            "403 application/problem\\+json 403",
            "Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator"
        ),
        "broken", "400 application/problem\\+json 400 .+",
        "large", "413 application/problem\\+json 413 .+"
    );

    @ParameterizedTest
    @ValueSource(strings = {"locked", "broken", "large"})
    void acTwoBTwoGivesProblemWithDetail(final String scenario, final WebApplicationContext context)
        throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final String photo = SubmissionPhotosProblemHttpTest.PHOTO.toString();
        UUID id = requests.add("SENT", photo);
        byte[] body = JpegCleanerTest.photo();
        switch (scenario) {
            case "locked" -> id = requests.add("ACCEPTED", photo);
            case "broken" -> body = "GIF89a".getBytes(StandardCharsets.ISO_8859_1);
            case null, default -> body = new byte[5_000_001];
        }
        MatcherAssert.assertThat(
            "Błąd ".concat(scenario).concat(" nie jest problem+json ze statusem i opisem"),
            SubmissionPhotosProblemHttpTest.problem(
                requests.put(id, SubmissionPhotosProblemHttpTest.PHOTO, body), context
            ),
            Matchers.matchesPattern(SubmissionPhotosProblemHttpTest.EXPECTED.get(scenario))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acTwoBTwoExplainsMissingPhotoWithoutInternals(
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
                "404 application/problem\\+json 404 (?!.*(Exception|SELECT|submissions)).+"
            )
        );
    }

    private static String problem(
        final MockHttpServletResponse response, final WebApplicationContext context
    ) throws Exception {
        final JsonNode json = context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
        return "%d %s %s %s".formatted(
            response.getStatus(), response.getContentType(),
            json.path("status").asString(), json.path("detail").asString()
        );
    }
}
