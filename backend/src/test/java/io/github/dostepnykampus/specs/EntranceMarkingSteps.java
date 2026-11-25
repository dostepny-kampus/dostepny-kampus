/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kroki scenariuszy zmiany oznaczenia wejścia: złożenie zmiany przez Zbierającego,
 * przyjęcie i odrzucenie przez Weryfikatora, wynik na karcie budynku i lista własna.
 *
 * <p>Wymagania: US17.2, US17.3, BR-16, BR-23</p>
 *
 * @since 0.1.0
 */
public final class EntranceMarkingSteps {

    /** Zdjęcie zgłoszeń scenariusza. */
    private static final UUID PHOTO = UUID.fromString("a1b2c3d4-1002-4e8a-9b0c-1d2e3f4a5b6c");

    /** Identyfikator wejścia scenariusza. */
    private static final String ENTRANCE = "spec-wejscie-oznaczenie";

    /** Zapis dnia w scenariuszach. */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d.MM.yyyy");

    /** Zgłoszenia według kolejności ze scenariusza. */
    private static final Map<String, UUID> IDS = Map.of(
        "pierwszy", UUID.fromString("a1b2c3d4-1001-4e8a-9b0c-1d2e3f4a5b6c"),
        "drugi", UUID.fromString("a1b2c3d4-1003-4e8a-9b0c-1d2e3f4a5b6c")
    );

    /** Numer konta według kolejności ze scenariusza. */
    private static final Map<String, String> NUM = Map.of("pierwszy", "1", "drugi", "2");

    /** Wartości API dla słów oznaczenia, źródła i statusu ze scenariuszy. */
    private static final Map<String, String> API = Map.of(
        "dostępne", "ACCESSIBLE", "niedostępne", "INACCESSIBLE", "brak danych", "NO_DATA",
        "import", "IMPORT", "obchód", "WALKTHROUGH",
        "archiwalne", "ARCHIVED", "odrzucone", "REJECTED"
    );

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Port serwera do eksportu CSV. */
    private final Environment env;

    /** Budynek bieżącego scenariusza. */
    private String building;

    /** Ostatni powód odrzucenia ze scenariusza. */
    private String reason;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param photos Magazyn zdjęć zgłoszeń
     * @param env Port serwera do eksportu CSV
     */
    public EntranceMarkingSteps(
        final MockMvc http, final JdbcClient jdbc, final PhotoStore photos, final Environment env
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.photos = photos;
        this.env = env;
        this.building = "";
        this.reason = "";
    }

    @Given("^(\\S+ \\S+ \\S+) ma oznaczenie „([^”]+)” ze źródłem „import”$")
    void entranceWithSource(final String target, final String marking) {
        this.place(target, marking, "1.11.2026");
    }

    @Given("^(\\S+ \\S+ \\S+) ma oznaczenie „([^”]+)” z datą (\\S+) i źródłem „import”$")
    void entranceWithDate(final String target, final String marking, final String day) {
        this.place(target, marking, day);
    }

    @Given("^Zbierający wysłał zmianę oznaczenia tego wejścia na „([^”]+)” (?:z nowym|ze) .+$")
    void collectorSentChange(final String marking) throws Exception {
        this.submit(EntranceMarkingSteps.IDS.get("pierwszy"), "COLLECTOR.c1", marking);
    }

    @Given("^Zbierający wysłał zmianę oznaczenia wejścia bocznego (\\S+) na „([^”]+)”$")
    void sentSideChange(final String symbol, final String marking) throws Exception {
        this.place("wejście boczne ".concat(symbol), "brak danych", "1.11.2026");
        this.collectorSentChange(marking);
    }

    @Given("^inny Zbierający wysłał zmianę oznaczenia tego wejścia na „([^”]+)”$")
    void otherCollectorSent(final String marking) throws Exception {
        this.submit(EntranceMarkingSteps.IDS.get("drugi"), "COLLECTOR.c2", marking);
    }

    @Given("^pierwszy Weryfikator (\\d{1,2}\\.\\d{2}\\.\\d{4}) przyjął zmianę na „.+”$")
    @When("^pierwszy Weryfikator (\\d{1,2}\\.\\d{2}\\.\\d{4}) weźmie i przyjmie tę zmianę$")
    void firstVerifierAccepts(final String day) throws Exception {
        this.decide(
            EntranceMarkingSteps.IDS.get("pierwszy"), "VERIFIER.v1",
            "{\"decision\":\"ACCEPT\",\"version\":1}"
        );
        this.jdbc.sql(
            "UPDATE entrances SET marking_date = :date WHERE id = :id AND building_symbol = :s"
        ).param("date", LocalDate.parse(day, EntranceMarkingSteps.DAY))
            .param("id", EntranceMarkingSteps.ENTRANCE).param("s", this.building).update();
    }

    @When("^(pierwszy|drugi) Weryfikator weźmie .+? i odrzuci ją z powodem „([^”]+)”$")
    void verifierRejects(final String who, final String text) throws Exception {
        this.reason = text;
        this.decide(
            EntranceMarkingSteps.IDS.get(who),
            "VERIFIER.v".concat(EntranceMarkingSteps.NUM.get(who)),
            "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"%s\"}".formatted(text)
        );
    }

    @Then("^zgłoszenie zmiany oznaczenia ma status „([^”]+)”( z tym powodem)?$")
    void submissionHasStatus(final String status, final @Nullable String why) throws Exception {
        final String body = this.get(
            "/submissions/%s".formatted(EntranceMarkingSteps.IDS.get("pierwszy")), "VERIFIER.v1"
        );
        MatcherAssert.assertThat(
            "Status zgłoszenia zmiany oznaczenia nie zgadza się z oczekiwanym", body,
            Matchers.containsString(
                "\"status\":\"%s\"".formatted(EntranceMarkingSteps.API.get(status))
            )
        );
        if (why != null) {
            MatcherAssert.assertThat(
                "Odrzucone zgłoszenie zmiany oznaczenia nie ma powodu odrzucenia", body,
                Matchers.containsString("\"rejectionReason\":\"%s\"".formatted(this.reason))
            );
        }
    }

    @Then("^na karcie budynku (\\S+) (?:wejście \\S+|inne wejście) ma napis „([^”]+)” i ikonę$")
    @Then("^(?:wejście \\S+|inne wejście) (\\S+) nadal ma oznaczenie „([^”]+)”$")
    void entranceHasMarking(final String symbol, final String marking) throws Exception {
        MatcherAssert.assertThat(
            "Oznaczenie wejścia na karcie budynku %s nie zgadza się".formatted(symbol),
            this.entranceNode(symbol).path("marking").asString(""),
            Matchers.equalTo(EntranceMarkingSteps.API.get(marking))
        );
    }

    @Then("^przy tym wejściu widnieje data oznaczenia (\\S+ i źródł(?:o|em) „[^”]+”)$")
    void entranceHasMeta(final String meta) throws Exception {
        this.checkMeta(this.building, meta);
    }

    @Then("^\\S+ \\S+ (\\S+) nadal ma oznaczenie „([^”]+)” z datą (\\S+ i źródłem „[^”]+”)$")
    @Then("^\\S+ \\S+ (\\S+) ma oznaczenie „([^”]+)” z datą (\\S+ i źródłem „obchód”)$")
    void entranceHasFullState(
        final String symbol, final String marking, final String meta
    ) throws Exception {
        this.entranceHasMarking(symbol, marking);
        this.checkMeta(symbol, meta);
    }

    @Then("zgłoszenie zmiany oznaczenia nie jest widoczne na mapie ani w eksporcie")
    void hiddenFromMapAndExport() throws Exception {
        final String id = EntranceMarkingSteps.IDS.get("pierwszy").toString();
        MatcherAssert.assertThat(
            "Zgłoszenie zmiany oznaczenia jest widoczne na mapie albo w eksporcie",
            List.of(
                this.get("/submissions", ""), this.exportCsv(),
                this.get("/submissions?buildingSymbol=".concat(this.building), "")
            ),
            Matchers.everyItem(Matchers.not(Matchers.containsString(id)))
        );
        MatcherAssert.assertThat(
            "Gość nie dostał 404 dla zgłoszenia zmiany oznaczenia",
            this.http.perform(MockMvcRequestBuilders.get("/submissions/{id}", id))
                .andReturn().getResponse().getStatus(),
            Matchers.equalTo(404)
        );
    }

    @Then("Zbierający widzi powód odrzucenia na liście swoich zgłoszeń")
    void collectorSeesRejection() throws Exception {
        this.checkOwn("pierwszy", "REJECTED", this.reason);
    }

    @Then("^inny Zbierający widzi tę zmianę ze statusem „(\\S+)” i powodem odrzucenia „([^”]+)”$")
    void otherCollectorSeesRejection(final String status, final String expected) throws Exception {
        this.checkOwn("drugi", EntranceMarkingSteps.API.get(status), expected);
    }

    @After
    void cleanUp() throws IOException {
        this.photos.delete(EntranceMarkingSteps.IDS.get("pierwszy"), EntranceMarkingSteps.PHOTO);
        this.photos.delete(EntranceMarkingSteps.IDS.get("drugi"), EntranceMarkingSteps.PHOTO);
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param("ids", List.copyOf(EntranceMarkingSteps.IDS.values())).update();
        this.jdbc.sql("DELETE FROM entrances WHERE id = :id")
            .param("id", EntranceMarkingSteps.ENTRANCE).update();
    }

    private void place(final String target, final String marking, final String day) {
        final int split = target.lastIndexOf(' ');
        final String kind = target.substring(0, split);
        this.building = target.substring(split + 1);
        this.jdbc.sql(
            String.join(
                " ", "INSERT INTO entrances (id, building_symbol, name, main, location,",
                "marking, marking_date, marking_source) SELECT :id, symbol, :name, :main,",
                "ST_PointOnSurface(outline), :marking, :date, 'IMPORT' FROM buildings",
                "WHERE symbol = :symbol ON CONFLICT (id, building_symbol) DO NOTHING"
            )
        ).param("id", EntranceMarkingSteps.ENTRANCE).param("name", kind)
            .param("main", "wejście główne".equals(kind)).param("symbol", this.building)
            .param("marking", EntranceMarkingSteps.API.get(marking))
            .param("date", LocalDate.parse(day, EntranceMarkingSteps.DAY)).update();
    }

    private void submit(final UUID id, final String token, final String marking) throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zmiany oznaczenia wejścia",
            this.post(
                "/submissions", token,
                String.join(
                    "", "{\"id\":\"%s\",\"kind\":\"ENTRANCE_MARKING\",\"buildingSymbol\":\"%s\",",
                    "\"entranceId\":\"%s\",\"entranceMarking\":\"%s\",",
                    "\"location\":{\"type\":\"Point\",\"coordinates\":%s},\"photoIds\":[\"%s\"]}"
                ).formatted(
                    id, this.building, EntranceMarkingSteps.ENTRANCE,
                    EntranceMarkingSteps.API.get(marking),
                    this.jdbc.sql(
                        String.join(
                            " ", "SELECT '['||ST_X(ST_PointOnSurface(outline))||','||",
                            "ST_Y(ST_PointOnSurface(outline))||']' FROM buildings WHERE symbol=:s"
                        )
                    ).param("s", this.building).query(String.class).single(),
                    EntranceMarkingSteps.PHOTO
                )
            ),
            Matchers.equalTo(201)
        );
        this.photos.put(id, EntranceMarkingSteps.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8));
    }

    private void decide(final UUID id, final String token, final String body) throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator nie wziął albo nie rozstrzygnął zmiany oznaczenia",
            List.of(
                this.post("/submissions/%s/claim".formatted(id), token, ""),
                this.post("/submissions/%s/review".formatted(id), token, body)
            ), Matchers.contains(200, 200)
        );
    }

    private int post(final String uri, final String token, final String body) throws Exception {
        final MockHttpServletRequestBuilder req = MockMvcRequestBuilders.post(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(token));
        if (!body.isEmpty()) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return this.http.perform(req).andReturn().getResponse().getStatus();
    }

    private String get(final String uri, final String token) throws Exception {
        final MockHttpServletRequestBuilder req = MockMvcRequestBuilders.get(uri);
        if (!token.isEmpty()) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(token));
        }
        return this.http.perform(req).andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    }

    private JsonNode entranceNode(final String symbol) throws Exception {
        final JsonMapper json = JsonMapper.builder().build();
        JsonNode found = json.createObjectNode();
        for (final JsonNode item : json.readTree(this.get("/buildings/".concat(symbol), ""))
            .path("entrances")) {
            if (EntranceMarkingSteps.ENTRANCE.equals(item.path("id").asString(""))) {
                found = item;
            }
        }
        MatcherAssert.assertThat(
            "Karta budynku %s nie zawiera wejścia scenariusza".formatted(symbol),
            found.path("id").asString(""), Matchers.equalTo(EntranceMarkingSteps.ENTRANCE)
        );
        return found;
    }

    private void checkMeta(final String symbol, final String meta) throws Exception {
        final JsonNode found = this.entranceNode(symbol);
        final int sep = meta.indexOf(' ');
        MatcherAssert.assertThat(
            "Data albo źródło wejścia budynku %s nie zgadza się".formatted(symbol),
            List.of(
                found.path("markingDate").asString(""), found.path("markingSource").asString("")
            ),
            Matchers.contains(
                LocalDate.parse(meta.substring(0, sep), EntranceMarkingSteps.DAY).toString(),
                EntranceMarkingSteps.API.get(
                    meta.substring(meta.indexOf('„', sep) + 1, meta.lastIndexOf('”'))
                )
            )
        );
    }

    private void checkOwn(final String who, final String status, final String expected)
        throws Exception {
        MatcherAssert.assertThat(
            "Lista własnych zgłoszeń nie zawiera odrzuconej zmiany z powodem",
            this.get(
                "/submissions?mine=true", "COLLECTOR.c".concat(EntranceMarkingSteps.NUM.get(who))
            ),
            Matchers.allOf(
                Matchers.containsString(EntranceMarkingSteps.IDS.get(who).toString()),
                Matchers.containsString("\"status\":\"%s\"".formatted(status)),
                Matchers.containsString("\"rejectionReason\":\"%s\"".formatted(expected))
            )
        );
    }

    private String exportCsv() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<String> reply = client.send(
                HttpRequest.newBuilder(
                    URI.create(
                        "http://localhost:%s/export.csv".formatted(
                            this.env.getRequiredProperty("local.server.port")
                        )
                    )
                ).header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR").build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            MatcherAssert.assertThat(
                "Eksport CSV nie dał 200", reply.statusCode(), Matchers.equalTo(200)
            );
            return reply.body();
        }
    }
}
