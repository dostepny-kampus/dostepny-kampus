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
import org.springframework.context.annotation.Import;
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
@Import(RustfsTestConfiguration.class)
@Transactional
final class SubmissionPhotosProblemHttpTest {

    /** Zdjęcie z listy photoIds. */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /** Brak szczegółów wewnętrznych w opisie 404. */
    private static final String CLEAN =
        "404 application/problem\\+json 404 (?!.*(Exception|SELECT|submissions)).+";

    /** Oczekiwany wzorzec "kod typ status detail" dla każdego przypadku. */
    private static final Map<String, String> EXPECTED = Map.of(
        "locked",
        "403 application/problem\\+json 403 "
            + "Zdjęcie zgłoszenia przyjętego zmienia tylko Weryfikator",
        "unknown", SubmissionPhotosProblemHttpTest.CLEAN,
        "unlisted", SubmissionPhotosProblemHttpTest.CLEAN,
        "broken", "400 application/problem\\+json 400 .+",
        "large", "413 application/problem\\+json 413 .+"
    );

    @ParameterizedTest
    @ValueSource(strings = {"locked", "unknown", "unlisted", "broken", "large"})
    void iTwoBTwoGivesProblemWithDetail(final String scenario, final WebApplicationContext context)
        throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final String photo = SubmissionPhotosProblemHttpTest.PHOTO.toString();
        UUID id = requests.add("SENT", photo);
        byte[] body = JpegCleanerTest.photo();
        switch (scenario) {
            case "locked" -> id = requests.add("ACCEPTED", photo);
            case "unknown" -> id = UUID.randomUUID();
            case "unlisted" -> id = requests.add("SENT", UUID.randomUUID().toString());
            case "broken" -> body = "GIF89a".getBytes(StandardCharsets.ISO_8859_1);
            default -> body = new byte[5_000_001];
        }
        final MockHttpServletResponse response = requests.put(
            id, SubmissionPhotosProblemHttpTest.PHOTO, body
        );
        final JsonNode json = context.getBean(ObjectMapper.class).readTree(
            response.getContentAsString(StandardCharsets.UTF_8)
        );
        MatcherAssert.assertThat(
            "Błąd ".concat(scenario).concat(" nie jest problem+json ze statusem i opisem"),
            "%d %s %s %s".formatted(
                response.getStatus(), response.getContentType(),
                json.path("status").asString(), json.path("detail").asString()
            ),
            Matchers.matchesPattern(SubmissionPhotosProblemHttpTest.EXPECTED.get(scenario))
        );
    }
}
