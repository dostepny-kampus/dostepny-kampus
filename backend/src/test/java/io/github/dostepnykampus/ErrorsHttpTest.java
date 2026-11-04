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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testy E1–E3 i W1-1, W1-2: wyjątek rzucony w obsłudze żądania daje
 * odpowiedź {@code application/problem+json} (RFC 9457) z kodem zależnym od
 * jego typu, a nieoczekiwany błąd nie ujawnia szczegółów wewnętrznych.
 * Konflikt z bieżącym stanem daje 409 z tym stanem jako obiektem JSON.
 *
 * <p>Prawdziwy serwer HTTP daje odpowiedź, jaką dostałby klient, także
 * dla wyjątku bez obsługi.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ErrorsHttpTest {

    @Test
    void errorOneGivesBadRequestProblemWithDetail(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Niepoprawne dane nie dają 400 application/problem+json z opisem w detail",
            ErrorsHttpTest.problem(port, "/proba-bledow/niepoprawne"),
            Matchers.equalTo(
                "400 application/problem+json 400 Szerokość 91 jest poza zakresem WGS84"
            )
        );
    }

    @Test
    void errorTwoGivesNotFoundProblemWithDetail(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Brak zasobu nie daje 404 application/problem+json z opisem w detail",
            ErrorsHttpTest.problem(port, "/proba-bledow/brak"),
            Matchers.equalTo("404 application/problem+json 404 Nie ma budynku Z-99")
        );
    }

    @Test
    void errorThreeGivesServerErrorProblem(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Nieoczekiwany wyjątek nie daje 500 application/problem+json",
            ErrorsHttpTest.problem(port, "/proba-bledow/awaria"),
            Matchers.startsWith("500 application/problem+json 500 ")
        );
    }

    @Test
    void errorThreeHidesExceptionClassAndMessage(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 500 ujawnia klasę albo komunikat wyjątku",
            ErrorsHttpTest.send(port, "/proba-bledow/awaria").body(),
            Matchers.not(
                Matchers.anyOf(
                    Matchers.containsString("IllegalStateException"),
                    Matchers.containsString("sekret-wewnetrzny-7f3a"),
                    Matchers.containsString("FailingController")
                )
            )
        );
    }

    @Test
    void conflictOneGivesConflictProblemWithDetail(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Konflikt z bieżącym stanem nie daje 409 application/problem+json z opisem w detail",
            ErrorsHttpTest.problem(port, "/proba-bledow/konflikt"),
            Matchers.equalTo("409 application/problem+json 409 Na serwerze jest nowsza wersja")
        );
    }

    @Test
    void conflictTwoGivesCurrentStateAsTopLevelJsonObject(@LocalServerPort final int port)
        throws Exception {
        MatcherAssert.assertThat(
            "Odpowiedź 409 nie ma na najwyższym poziomie obiektu currentState z polami rekordu",
            JsonMapper.shared().readTree(
                ErrorsHttpTest.send(port, "/proba-bledow/konflikt").body()
            ).path("currentState"),
            Matchers.equalTo(
                JsonMapper.shared().readTree("{\"version\":3,\"status\":\"IN_REVIEW\"}")
            )
        );
    }

    private static String problem(final int port, final String path) throws Exception {
        final HttpResponse<String> reply = ErrorsHttpTest.send(port, path);
        MatcherAssert.assertThat(
            "Odpowiedź błędu nie jest schematem Problem z kontraktu",
            new OpenApiContract().violations("Problem", reply.body()),
            Matchers.empty()
        );
        final MediaType type = MediaType.parseMediaType(
            reply.headers().firstValue("Content-Type").orElse("text/plain")
        );
        JsonNode body = JsonMapper.shared().missingNode();
        if (type.getSubtype().endsWith("json")) {
            body = JsonMapper.shared().readTree(reply.body());
        }
        return "%d %s/%s %s %s".formatted(
            reply.statusCode(), type.getType(), type.getSubtype(),
            body.path("status").asString(), body.path("detail").asString()
        );
    }

    private static HttpResponse<String> send(final int port, final String path)
        throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                HttpRequest.newBuilder(
                    URI.create("http://localhost:%d%s".formatted(port, path))
                ).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        }
    }
}
