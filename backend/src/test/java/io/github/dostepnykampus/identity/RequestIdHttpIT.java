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
 * Odpowiedź serwera ma nagłówek {@code X-Request-Id}, także odmowa 401 z łańcucha zabezpieczeń:
 * poprawny identyfikator z żądania wraca bez zmian, a bez niego serwer nadaje własny.
 *
 * <p>Wymagania: NFR-57</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class RequestIdHttpIT {

    @Test
    void echoesRequestIdOnDeniedRequest(@LocalServerPort final int port) throws Exception {
        final HttpResponse<String> reply = RequestIdHttpIT.get(port, "klient-trace-1");
        MatcherAssert.assertThat(
            "Odmowa 401 nie zwróciła identyfikatora żądania z nagłówka X-Request-Id",
            "%d %s".formatted(
                reply.statusCode(), reply.headers().firstValue("X-Request-Id").orElse("brak")
            ),
            Matchers.equalTo("401 klient-trace-1")
        );
    }

    @Test
    void createsRequestIdWhenClientSendsNone(@LocalServerPort final int port) throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź bez identyfikatora w żądaniu nie dostała własnego X-Request-Id",
            RequestIdHttpIT.get(port, null).headers().firstValue("X-Request-Id").orElse("brak"),
            Matchers.matchesPattern("[0-9a-f-]{36}")
        );
    }

    private static HttpResponse<String> get(final int port, final String identifier)
        throws Exception {
        final HttpRequest.Builder request = HttpRequest.newBuilder(
            URI.create("http://localhost:%d/me".formatted(port))
        );
        if (identifier != null) {
            request.header("X-Request-Id", identifier);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
