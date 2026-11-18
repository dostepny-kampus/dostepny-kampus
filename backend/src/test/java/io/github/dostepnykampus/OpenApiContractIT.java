/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprawdzenie zgodności z {@code api/openapi.yaml} na sztucznych wymianach
 * HTTP: zgodne przechodzą, a niezgodne dają błąd testu
 * z operacją, statusem i ścieżką JSON naruszenia.
 *
 * @since 0.1.0
 */
final class OpenApiContractIT {

    /**
     * Zgodna karta budynku bez wejść.
     */
    private static final String CARD = String.join(
        "",
        "{\"symbol\":\"A-1\",\"name\":\"Budynek A-1\",\"detailLevel\":\"BASIC\",",
        "\"outline\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[17.05,51.10],",
        "[17.06,51.10],[17.06,51.11],[17.05,51.10]]]]},\"entrances\":[]}"
    );

    /**
     * Zgodna karta budynku z wejściem i wyjściem ewakuacyjnym z planów PWr.
     */
    private static final String PWR_CARD = String.join(
        "",
        OpenApiContractIT.CARD.replace("\"entrances\":[]}", ""),
        "\"entrances\":[{\"id\":\"w-1\",\"buildingSymbol\":\"A-1\",\"main\":true,",
        "\"location\":{\"type\":\"Point\",\"coordinates\":[17.05,51.10]},",
        "\"marking\":\"ACCESSIBLE\",\"markingDate\":\"2024-01-10\",\"markingSource\":\"IMPORT\",",
        "\"name\":\"Wejście Cesarskie\",\"approach\":\"Od ul. Wybrzeże.\",\"doorWidthCm\":120,",
        "\"source\":\"Politechnika Wrocławska\"}],",
        "\"evacuationExits\":[{\"id\":\"e-1\",",
        "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06,51.10]},",
        "\"description\":\"Wyjście ewakuacyjne z pomieszczenia 41\",",
        "\"source\":\"Politechnika Wrocławska\"}]}"
    );

    /**
     * Zgodna lista budynków z wejściem budynku.
     */
    private static final String LIST = String.join(
        "",
        "{\"source\":\"© OpenStreetMap contributors\",\"buildings\":[",
        OpenApiContractIT.PWR_CARD.substring(
            0, OpenApiContractIT.PWR_CARD.indexOf(",\"evacuationExits\"")
        ),
        "}]}"
    );

    /**
     * Adres zdjęcia zgłoszenia.
     */
    private static final String PHOTO = String.join(
        "/", "/submissions", "8d0c7a5e-1b7a-4c64-9f50-3c1f0d0e6a11", "photos",
        "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
    );

    @ParameterizedTest(name = "{2}")
    @MethodSource("conforming")
    void acceptsConformingExchange(
        final RequestBuilder request, final ResponseEntity<String> reply, final String name
    ) {
        Assertions.assertDoesNotThrow(
            () -> OpenApiContractIT.answer(request, reply).andExpect(new OpenApiContract()),
            String.format("Zgodna wymiana nie przechodzi: %s", name)
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("violating")
    void rejectsViolatingExchange(
        final RequestBuilder request, final ResponseEntity<String> reply, final String message
    ) {
        MatcherAssert.assertThat(
            "Niezgodna wymiana przechodzi albo komunikat nie wskazuje naruszenia",
            String.valueOf(
                Assertions.assertThrows(
                    AssertionError.class,
                    () -> OpenApiContractIT.answer(request, reply)
                        .andExpect(new OpenApiContract()),
                    "Niezgodna wymiana kończy się błędem asercji"
                ).getMessage()
            ),
            Matchers.containsString(message)
        );
    }

    @Test
    void findsProblemWithoutTitle() {
        MatcherAssert.assertThat(
            "Błąd bez title przechodzi sprawdzenie schematem Problem",
            new OpenApiContract().violations("Problem", "{\"status\":400}"),
            Matchers.hasItem(Matchers.containsString("'title'"))
        );
    }

    @Test
    void readsSpecificationOnce() throws Exception {
        final AtomicInteger reads = new AtomicInteger();
        final OpenApiContract contract = new OpenApiContract(
            () -> {
                reads.incrementAndGet();
                return OpenApiContract.text();
            }
        );
        for (int done = 0; done < 3; done += 1) {
            OpenApiContractIT.answer(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractIT.json(200, OpenApiContractIT.CARD)
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
                OpenApiContractIT.json(200, OpenApiContractIT.CARD), "karta budynku"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractIT.json(200, OpenApiContractIT.PWR_CARD),
                "karta budynku z wejściem i wyjściem ewakuacyjnym z planów PWr"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings"),
                OpenApiContractIT.json(200, OpenApiContractIT.LIST), "lista budynków z wejściem"
            ),
            Arguments.of(
                (RequestBuilder) servlet -> new MockHttpServletRequest(
                    servlet, "GET", "/buildings/A-1"
                ),
                OpenApiContractIT.json(200, OpenApiContractIT.CARD),
                "żądanie złożone ręcznie, bez pathInfo"
            ),
            Arguments.of(
                OpenApiContractIT.locate("51.1"),
                OpenApiContractIT.json(200, "{\"insideOutline\":false}"),
                "ścieżka bez szablonu przed szablonem /buildings/{symbol}"
            ),
            Arguments.of(
                OpenApiContractIT.review(
                    "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Duplikat\"}"
                ),
                OpenApiContractIT.problem(401, "{\"title\":\"Brak tokenu\",\"status\":401}"),
                "zgodne ciało żądania"
            ),
            Arguments.of(
                MockMvcRequestBuilders.put(OpenApiContractIT.PHOTO).contentType("image/jpeg"),
                ResponseEntity.noContent().<String>build(),
                "treść żądania strumieniem, niewidoczna dla biblioteki"
            ),
            Arguments.of(
                OpenApiContractIT.photo(MediaType.IMAGE_JPEG),
                ResponseEntity.noContent().<String>build(), "zdjęcie JPEG jako treść nie-JSON"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get(OpenApiContractIT.PHOTO),
                ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body("zdjęcie"),
                "odpowiedź JPEG"
            ),
            Arguments.of(
                OpenApiContractIT.review("{\"decision\":\"REJECT\",\"version\":1}"),
                OpenApiContractIT.problem(400, "{\"title\":\"Błąd\",\"status\":400}"),
                "niezgodne żądanie odrzucone statusem 400 z Problem"
            ),
            Arguments.of(
                OpenApiContractIT.locate("abc"),
                OpenApiContractIT.problem(400, "{\"title\":\"Błąd\",\"status\":400}"),
                "parametr złego typu odrzucony statusem 400"
            )
        );
    }

    private static Stream<Arguments> violating() {
        return Stream.of(
            Arguments.of(
                OpenApiContractIT.locate("abc"),
                OpenApiContractIT.json(200, "{\"insideOutline\":false}"),
                "GET /buildings/locate 200: validation.request.parameter.schema.type"
            ),
            OpenApiContractIT.card("\"detailLevel\":\"BASIC\",", "", "property 'detailLevel'"),
            OpenApiContractIT.card("\"A-1\"", "1", "body.schema.type /symbol "),
            OpenApiContractIT.card("BASIC", "MEDIUM", "body.schema.enum /detailLevel "),
            OpenApiContractIT.pwrCard(
                "\"doorWidthCm\":120", "\"doorWidthCm\":0",
                "body.schema.minimum /entrances/0/doorWidthCm "
            ),
            OpenApiContractIT.pwrCard(
                "\"approach\":\"Od ul. Wybrzeże.\"", "\"approach\":1",
                "body.schema.type /entrances/0/approach "
            ),
            OpenApiContractIT.pwrCard(
                "{\"id\":\"e-1\",", "{", "body.schema.required /evacuationExits/0 "
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings"),
                OpenApiContractIT.json(
                    200, OpenApiContractIT.LIST.replace("ACCESSIBLE", "OPEN")
                ),
                "body.schema.enum /buildings/0/entrances/0/marking "
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/nieznane"), OpenApiContractIT.json(200, "{}"),
                "GET /nieznane 200: validation.request.path.missing"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                OpenApiContractIT.problem(500, "{\"title\":\"Błąd\",\"status\":500}"),
                "GET /buildings/A-1 500: validation.response.status.unknown"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                    .body(OpenApiContractIT.CARD),
                "A-1 200: validation.response.contentType.notAllowed"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings"),
                OpenApiContractIT.problem(405, "{\"status\":405}"),
                "GET /buildings 405: validation.response.body.schema.required"
            ),
            Arguments.of(
                OpenApiContractIT.review("{\"decision\":\"REJECT\",\"version\":1}"),
                OpenApiContractIT.json(200, "{}"),
                "review 200: validation.request.body.schema"
            ),
            Arguments.of(
                OpenApiContractIT.review("{\"decision\":\"REJECT\",\"version\":1}"),
                OpenApiContractIT.problem(400, "{\"status\":400}"),
                "review 400: validation.response.body.schema.required"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get(OpenApiContractIT.PHOTO),
                ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body("zdjęcie"),
                "200: validation.response.contentType.notAllowed"
            ),
            Arguments.of(
                OpenApiContractIT.photo(MediaType.TEXT_PLAIN),
                ResponseEntity.noContent().<String>build(),
                "204: validation.request.contentType.notAllowed"
            ),
            Arguments.of(
                MockMvcRequestBuilders.get("/buildings/A-1"),
                ResponseEntity.ok().<String>build(),
                "GET /buildings/A-1 200: validation.response.body.missing"
            )
        );
    }

    private static Arguments card(
        final String part, final String replacement, final String message
    ) {
        return Arguments.of(
            MockMvcRequestBuilders.get("/buildings/A-1"),
            OpenApiContractIT.json(200, OpenApiContractIT.CARD.replace(part, replacement)),
            message
        );
    }

    private static Arguments pwrCard(
        final String part, final String replacement, final String message
    ) {
        return Arguments.of(
            MockMvcRequestBuilders.get("/buildings/A-1"),
            OpenApiContractIT.json(200, OpenApiContractIT.PWR_CARD.replace(part, replacement)),
            message
        );
    }

    private static RequestBuilder photo(final MediaType type) {
        return MockMvcRequestBuilders.put(OpenApiContractIT.PHOTO).contentType(type)
            .content(new byte[] {(byte) 0xFF, (byte) 0xD8});
    }

    private static RequestBuilder locate(final String latitude) {
        return MockMvcRequestBuilders.get("/buildings/locate").queryParam("lat", latitude)
            .queryParam("lon", "17.06");
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
