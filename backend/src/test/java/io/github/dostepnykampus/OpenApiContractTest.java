/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprawdzenie zgodności z {@code api/openapi.yaml} na sztucznych wymianach
 * HTTP (decyzja 122): zgodne przechodzą, a niezgodne dają błąd testu
 * z operacją, statusem i ścieżką JSON naruszenia.
 *
 * @since 0.1.0
 */
final class OpenApiContractTest {

    /**
     * Zgodna karta budynku bez wejść.
     */
    private static final String CARD = String.join(
        "",
        "{\"symbol\":\"A-1\",\"name\":\"Budynek A-1\",\"detailLevel\":\"BASIC\",",
        "\"outline\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[17.05,51.10],",
        "[17.06,51.10],[17.06,51.11],[17.05,51.10]]]]},\"entrances\":[]}"
    );

    @ParameterizedTest(name = "{2}")
    @MethodSource("conforming")
    void contractAcceptsConformingExchange(
        final RequestBuilder request, final ResponseEntity<String> reply, final String name
    ) {
        Assertions.assertDoesNotThrow(
            () -> OpenApiContractTest.answer(request, reply).andExpect(new OpenApiContract()),
            String.format("Zgodna wymiana nie przechodzi: %s", name)
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("violating")
    void contractRejectsViolatingExchange(
        final RequestBuilder request, final ResponseEntity<String> reply, final String message
    ) {
        MatcherAssert.assertThat(
            "Niezgodna wymiana przechodzi albo komunikat nie wskazuje naruszenia",
            String.valueOf(
                Assertions.assertThrows(
                    AssertionError.class,
                    () -> OpenApiContractTest.answer(request, reply)
                        .andExpect(new OpenApiContract())
                ).getMessage()
            ),
            Matchers.containsString(message)
        );
    }

    @Test
    void contractReadsSpecificationOnce() throws Exception {
        final AtomicInteger reads = new AtomicInteger();
        final OpenApiContract contract = new OpenApiContract(
            () -> {
                reads.incrementAndGet();
                return Files.readString(Path.of("..", "api", "openapi.yaml"));
            }
        );
        for (int done = 0; done < 3; done += 1) {
            OpenApiContractTest.answer(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractTest.json(200, OpenApiContractTest.CARD)
            ).andExpect(contract);
        }
        MatcherAssert.assertThat(
            "Kontrakt jest wczytywany przy każdym sprawdzeniu", reads.get(), Matchers.equalTo(1)
        );
    }

    private static Stream<Arguments> conforming() {
        return Stream.of(
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractTest.json(200, OpenApiContractTest.CARD), "karta budynku"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/locate"),
                OpenApiContractTest.json(200, "{\"insideOutline\":false}"),
                "ścieżka bez szablonu przed szablonem /buildings/{symbol}"
            ),
            Arguments.of(
                OpenApiContractTest.review(
                    "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Duplikat\"}"
                ),
                OpenApiContractTest.problem(401, "{\"title\":\"Brak tokenu\",\"status\":401}"),
                "zgodne ciało żądania"
            )
        );
    }

    private static Stream<Arguments> violating() {
        return Stream.of(
            OpenApiContractTest.card("\"name\":\"Budynek A-1\",", "", "property 'name'"),
            OpenApiContractTest.card("\"A-1\"", "1", "{symbol} 200: /symbol "),
            OpenApiContractTest.card(
                "BASIC", "MEDIUM", "GET /buildings/{symbol} 200: /detailLevel "
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/nieznane"), OpenApiContractTest.json(200, "{}"),
                "GET /nieznane: brak operacji"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractTest.problem(500, "{\"title\":\"Błąd\",\"status\":500}"),
                "GET /buildings/{symbol} 500: brak statusu"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                    .body(OpenApiContractTest.CARD),
                "GET /buildings/{symbol} 200: typ treści 'text/plain'"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings"),
                OpenApiContractTest.problem(405, "{\"status\":405}"),
                "GET /buildings 405:  required property 'title'"
            ),
            Arguments.of(
                OpenApiContractTest.review("{\"decision\":\"REJECT\",\"version\":1}"),
                OpenApiContractTest.problem(401, "{\"title\":\"Brak tokenu\",\"status\":401}"),
                "POST /submissions/{submissionId}/review, żądanie: "
            )
        );
    }

    private static Arguments card(
        final String part, final String replacement, final String message
    ) {
        return Arguments.of(
            MockMvcRequestBuilders.get("/buildings/A-1"),
            OpenApiContractTest.json(200, OpenApiContractTest.CARD.replace(part, replacement)),
            message
        );
    }

    private static RequestBuilder review(final String decision) {
        return MockMvcRequestBuilders.post(
            "/submissions/{submissionId}/review", "8d0c7a5e-1b7a-4c64-9f50-3c1f0d0e6a11"
        ).contentType(MediaType.APPLICATION_JSON)
            .content(decision);
    }

    private static ResponseEntity<String> json(final int status, final String body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private static ResponseEntity<String> problem(final int status, final String body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    private static ResultActions answer(
        final RequestBuilder request, final ResponseEntity<String> reply
    ) throws Exception {
        return MockMvcBuilders.standaloneSetup(new Canned(reply)).build().perform(request);
    }

    /**
     * Punkt HTTP z tą samą odpowiedzią na każde żądanie.
     *
     * @param reply Odpowiedź na każde żądanie
     * @since 0.1.0
     */
    @RestController
    record Canned(ResponseEntity<String> reply) {

        /**
         * Odpowiedź na żądanie o dowolnej metodzie i ścieżce.
         *
         * @return Ta sama odpowiedź
         */
        @RequestMapping("/**")
        ResponseEntity<String> answer() {
            return this.reply;
        }
    }
}
