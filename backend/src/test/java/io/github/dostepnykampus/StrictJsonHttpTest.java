/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Testy W2-7: ciało, którego nie da się związać z rekordem, daje
 * 400 {@code application/problem+json}, a nie 500, także gdy odrzuca je
 * dopiero ścisłe wiązanie {@code JsonMapper} kontekstu serwera.
 *
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgisTestConfiguration.class, StrictJsonHttpTest.Binding.class})
final class StrictJsonHttpTest {

    /**
     * Status i typ treści odpowiedzi 400 RFC 9457.
     */
    private static final String BAD_REQUEST = "400 application/problem+json";

    @Test
    void strictJsonSevenGivesBadRequestForMismatchedValue(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Wartość niepasująca do typu pola nie daje 400 application/problem+json",
            StrictJsonHttpTest.reply(port, "{\"version\":\"jeden\"}"),
            Matchers.equalTo(StrictJsonHttpTest.BAD_REQUEST)
        );
    }

    @Test
    void strictJsonSevenGivesBadRequestForCoercedValue(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Napis w polu liczbowym nie daje 400 application/problem+json",
            StrictJsonHttpTest.reply(port, "{\"accuracyMeters\":\"5\"}"),
            Matchers.equalTo(StrictJsonHttpTest.BAD_REQUEST)
        );
    }

    private static String reply(final int port, final String body) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<String> reply = client.send(
                HttpRequest.newBuilder(
                    URI.create("http://localhost:%d/proba-wiazania".formatted(port))
                ).header("Content-Type", "application/json").POST(
                    HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                ).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            final MediaType type = MediaType.parseMediaType(
                reply.headers().firstValue("Content-Type").orElse("text/plain")
            );
            return "%d %s/%s".formatted(reply.statusCode(), type.getType(), type.getSubtype());
        }
    }

    /**
     * Punkt HTTP tylko dla testów, wiążący ciało JSON z rekordem tak jak
     * kontrolery serwera; {@link TestComponent} wyłącza go ze skanowania.
     *
     * @since 0.1.0
     */
    @TestComponent
    @RestController
    static final class Binding {

        @PostMapping("/proba-wiazania")
        String bind(@RequestBody final StrictJsonTest.Sample sample) {
            return sample.toString();
        }
    }
}
