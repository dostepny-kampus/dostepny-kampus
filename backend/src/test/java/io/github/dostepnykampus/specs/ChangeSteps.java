/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.barrier.submission.persistence.JdbcSubmissions;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariuszy zmian zgłoszenia przyjętego, edycji autora w trakcie weryfikacji i prób
 * zmiany danych przez Weryfikatora, przez trasy zgłoszeń, weryfikacji i podobnych zgłoszeń.
 *
 * <p>Autorem zgłoszeń jest konto {@code autor-zgloszenia} z własnym tokenem testowym,
 * a Weryfikatorem konto z tokenu domyślnego. Zgłoszenie pod weryfikacją ma identyfikator
 * ze scenariuszy Weryfikatora.</p>
 *
 * <p>Wymagania: US8.3, US8.4, US10.1, BR-3, BR-15, BR-18, BR-19, BR-23</p>
 *
 * @since 0.1.0
 */
public final class ChangeSteps {

    /** Przyjęte zgłoszenie, którego dotyczą zmiany. */
    private static final UUID ORIGINAL = UUID.fromString("b4c5d6e7-0011-4f8a-9b0c-1d2e3f4a5b6c");

    /** Zmiana „bariera usunięta” złożona niezależnie od zmiany sprawdzanej. */
    private static final UUID RIVAL = UUID.fromString("b4c5d6e7-0012-4f8a-9b0c-1d2e3f4a5b6c");

    /** Miejsce zgłoszeń scenariuszy. */
    private static final SubmissionPoint ORIGIN = new SubmissionPoint(17.05, 51.108);

    /** Typ, uciążliwość i współrzędne zmiany, którą Weryfikator próbuje wprowadzić. */
    private static final Map<String, List<String>> EDITS = Map.of(
        "typ bariery na TB-1 „schody przy wejściu”", List.of("TB-1", "2", "[17.05,51.108]"),
        "uciążliwość na 3", List.of("TB-13", "3", "[17.05,51.108]"),
        "miejsce", List.of("TB-13", "2", "[17.06,51.109]")
    );

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Status odpowiedzi na próbę zmiany. */
    private final AttemptOutcome outcome;

    /** Symbol budynku zgłoszeń. */
    private String building;

    /** Numer typu bariery zgłoszeń. */
    private int type;

    /** Miejsce zgłoszenia przed próbą zmiany, jako tekst. */
    private String place;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     * @param photos Magazyn zdjęć zgłoszeń
     * @param outcome Status odpowiedzi na próbę, wspólny z innymi krokami
     */
    public ChangeSteps(
        final MockMvc http, final JdbcClient jdbc, final JdbcSubmissions submissions,
        final PhotoStore photos, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.submissions = submissions;
        this.photos = photos;
        this.outcome = outcome;
        this.building = "";
        this.type = 0;
        this.place = "";
    }

    @Given("^na mapie jest przyjęte zgłoszenie TB-(\\d+) .+ z uciążliwością (\\d) w budynku (.+)$")
    void accepted(final String number, final String severity, final String symbol) {
        this.building = symbol;
        this.type = Integer.parseInt(number);
        this.store(ChangeSteps.ORIGINAL, Integer.parseInt(severity), null, "ACCEPTED");
    }

    @Given("^na mapie jest przyjęte zgłoszenie TB-(\\d+) „[^”]+” w budynku (\\S+)$")
    void acceptedPlain(final String number, final String symbol) {
        this.accepted(number, "2", symbol);
    }

    @Given("^pierwszy Weryfikator wziął zmianę tego zgłoszenia z uciążliwością (\\d)$")
    void claimedChange(final String severity) throws Exception {
        this.store(
            VerifierSteps.CHECKED, Integer.parseInt(severity), ChangeSteps.ORIGINAL, "SENT"
        );
        this.claim();
    }

    @Given("pierwszy Weryfikator wziął zgłoszenie „bariera usunięta” tej bariery")
    void claimedRemoval() throws Exception {
        this.store(VerifierSteps.CHECKED, null, ChangeSteps.ORIGINAL, "SENT");
        this.claim();
    }

    @Given("^pierwszy Weryfikator wziął zgłoszenie TB-13 „próg” z uciążliwością (\\d)$")
    void claimedOwn(final String severity) throws Exception {
        this.building = "C-13";
        this.type = 13;
        this.store(VerifierSteps.CHECKED, Integer.parseInt(severity), null, "SENT");
        this.claim();
    }

    @Given("^autor przyjętego zgłoszenia TB-9 .+ D-2 wysłał zmianę z uciążliwością 2, a inny .+$")
    void parallelChanges() {
        this.building = "D-2";
        this.type = 9;
        this.store(ChangeSteps.ORIGINAL, 2, null, "ACCEPTED");
        this.store(VerifierSteps.CHECKED, 2, ChangeSteps.ORIGINAL, "SENT");
        this.store(ChangeSteps.RIVAL, null, ChangeSteps.ORIGINAL, "SENT");
    }

    @When("pierwszy Weryfikator weźmie zmianę autora")
    void claimsChange() throws Exception {
        this.claim();
    }

    @When("^autor zmieni uciążliwość na (\\d)$")
    void authorEdits(final String severity) throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął edycji autora zgłoszenia w weryfikacji, nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.put("/submissions/{id}", VerifierSteps.CHECKED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR.autor-zgloszenia")
                    .content(ChangeSteps.edit(List.of("TB-13", severity, "[17.05,51.108]")))
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    @When("pierwszy Weryfikator przyjmie zgłoszenie")
    void verifierAccepts() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął decyzji Weryfikatora o zgłoszeniu po edycji, nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions/{id}/review", VerifierSteps.CHECKED)
                    .contentType(MediaType.APPLICATION_JSON).with(new AsVerifier())
                    .content("{\"decision\":\"ACCEPT\",\"version\":2}")
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    @When("^spróbuje zmienić w zgłoszeniu (typ bariery na TB-1 .+|uciążliwość na 3|miejsce)$")
    void triesToEdit(final String data) throws Exception {
        this.place = this.stored();
        this.outcome.record(
            this.http.perform(
                MockMvcRequestBuilders.put("/submissions/{id}", VerifierSteps.CHECKED)
                    .contentType(MediaType.APPLICATION_JSON).with(new AsVerifier())
                    .content(ChangeSteps.edit(ChangeSteps.EDITS.get(data)))
            ).andReturn().getResponse().getStatus()
        );
    }

    @Then("^zgłoszenie przyjęte ma uciążliwość (\\d)$")
    void acceptedSeverity(final String severity) throws Exception {
        MatcherAssert.assertThat(
            "Gość nie widzi zgłoszenia przyjętego z uciążliwością z edycji autora",
            this.get(String.format("/submissions/%s", VerifierSteps.CHECKED)),
            Matchers.containsString(String.format("\"severity\":%s", severity))
        );
    }

    @Then("^na mapie jest nadal bariera TB-13 z uciążliwością 2$")
    void barrierKept() throws Exception {
        MatcherAssert.assertThat(
            "Gość nie widzi poprzedniego zgłoszenia z typem TB-13 i uciążliwością 2",
            this.get(String.format("/submissions/%s", ChangeSteps.ORIGINAL)),
            Matchers.allOf(
                Matchers.containsString("\"barrierType\":\"TB-13\""),
                Matchers.containsString("\"severity\":2")
            )
        );
    }

    @Then("to zgłoszenie ma nadal status „przyjęte”")
    void stillAccepted() {
        MatcherAssert.assertThat(
            "Poprzednie zgłoszenie straciło status po odrzuceniu zmiany",
            this.jdbc.sql("SELECT status FROM submissions WHERE id = :id")
                .param("id", ChangeSteps.ORIGINAL).query(String.class).single(),
            Matchers.equalTo("ACCEPTED")
        );
    }

    @Then("niesprawna winda jest nadal na mapie i na karcie budynku D-2")
    void elevatorKept() throws Exception {
        MatcherAssert.assertThat(
            "Mapa albo karta budynku D-2 nie pokazuje bariery po odrzuceniu jej usunięcia",
            List.of(
                this.get("/submissions"),
                this.get(String.format("/submissions?buildingSymbol=%s", this.building))
            ),
            Matchers.everyItem(Matchers.containsString(ChangeSteps.ORIGINAL.toString()))
        );
    }

    @Then("na liście podobnych zgłoszeń widzi zmianę „bariera usunięta”")
    void similarRemoval() throws Exception {
        MatcherAssert.assertThat(
            "Podobne zgłoszenia nie zawierają zmiany „bariera usunięta”",
            this.http.perform(
                MockMvcRequestBuilders.get("/submissions/{id}/similar", VerifierSteps.CHECKED)
                    .with(new AsVerifier())
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString(ChangeSteps.RIVAL.toString())
        );
    }

    @Then("zgłoszenie ma nadal typ TB-13 „próg”, uciążliwość 2 i miejsce podane przez Zbierającego")
    void dataKept() {
        MatcherAssert.assertThat(
            "Weryfikator zmienił typ, uciążliwość albo miejsce zgłoszenia z pomiaru precyzyjnego",
            this.jdbc.sql(
                """
                SELECT CONCAT_WS(
                    ',', content ->> 'barrierType', content ->> 'severity', ST_AsText(location)
                ) FROM submissions WHERE id = :id
                """
            ).param("id", VerifierSteps.CHECKED).query(String.class).single(),
            Matchers.equalTo(String.join(",", "TB-13", "2", this.place))
        );
    }

    @After
    void remove() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)").param(
            "ids", List.of(ChangeSteps.ORIGINAL, VerifierSteps.CHECKED, ChangeSteps.RIVAL)
        ).update();
    }

    private void store(
        final UUID id, final @Nullable Integer severity, final @Nullable UUID replaces,
        final String status
    ) {
        this.submissions.create(
            new NewSubmission(
                id,
                new SubmissionContent(
                    SubmissionKind.PRECISE,
                    new SubmissionPlace(
                        ChangeSteps.ORIGIN, null, false, this.building,
                        new SubmissionFloor(null, 0, false)
                    ),
                    new BarrierDetails(
                        null, new BarrierType(this.type), severity, null, severity == null
                    ),
                    null, null, replaces, List.of(VerifierSteps.PHOTO)
                ),
                "autor-zgloszenia"
            )
        );
        this.photos.put(id, VerifierSteps.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8));
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }

    private void claim() throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator nie wziął zgłoszenia, serwer nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions/{id}/claim", VerifierSteps.CHECKED)
                    .with(new AsVerifier())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    private static String edit(final List<String> data) {
        return String.join(
            "",
            "{\"version\":1,\"kind\":\"PRECISE\",\"location\":{\"type\":\"Point\",",
            "\"coordinates\":", data.get(2), "},\"buildingSymbol\":\"C-13\",",
            "\"floor\":{\"number\":0},\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",",
            "\"barrierType\":\"", data.get(0), "\",\"severity\":", data.get(1),
            ",\"photoIds\":[\"", VerifierSteps.PHOTO.toString(), "\"]}"
        );
    }

    private String stored() {
        return this.jdbc.sql("SELECT ST_AsText(location) FROM submissions WHERE id = :id")
            .param("id", VerifierSteps.CHECKED).query(String.class).single();
    }

    private String get(final String uri) throws Exception {
        return this.http.perform(MockMvcRequestBuilders.get(uri))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
