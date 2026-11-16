/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import io.github.dostepnykampus.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Punkty Actuatora bez tokenu: {@code health} jest publiczny i nie ujawnia szczegółów,
 * metryki i każdy inny punkt są zamknięte.
 *
 * <p>Wymagania: NFR-33, NFR-57</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ActuatorHttpIT {

    @Test
    void servesPublicHealth(@LocalServerPort final int port) throws Exception {
        final HttpResponse<String> reply = ActuatorHttpIT.get(port, "/actuator/health");
        MatcherAssert.assertThat(
            "Publiczny health nie zwrócił 200 ze stanem bez szczegółów",
            "%d %s".formatted(reply.statusCode(), reply.body()),
            Matchers.equalTo("200 {\"status\":\"UP\"}")
        );
    }

    @Test
    void metricsRequireCoordinator(@LocalServerPort final int port) throws Exception {
        MatcherAssert.assertThat(
            "Metryki bez tokenu nie zwróciły 401",
            ActuatorHttpIT.get(port, "/actuator/metrics").statusCode(),
            Matchers.equalTo(401)
        );
    }

    @Test
    void closesOtherEndpoints(@LocalServerPort final int port) throws Exception {
        MatcherAssert.assertThat(
            "Nieopisany punkt Actuatora bez tokenu nie zwrócił 401",
            ActuatorHttpIT.get(port, "/actuator/env").statusCode(),
            Matchers.equalTo(401)
        );
    }

    private static HttpResponse<String> get(final int port, final String path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:%d%s".formatted(port, path)))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
        }
    }
}
