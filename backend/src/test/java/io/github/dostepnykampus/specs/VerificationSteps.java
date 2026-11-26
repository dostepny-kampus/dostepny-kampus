/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
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
 * Kroki scenariuszy odmowy decyzji Weryfikatora: odrzucenie bez powodu i przyjęcie zgłoszenia
 * szybkiego bez typu albo uciążliwości. Decyzje idą trasami {@code POST /submissions/{id}/claim}
 * i {@code POST /submissions/{id}/review}.
 *
 * <p>Token testowy ma zawsze to samo konto, więc autorem zgłoszenia jest inne konto w bazie,
 * a Weryfikatorem konto z tokenu. Po scenariuszu znika zgłoszenie, które on założył.</p>
 *
 * <p>Wymagania: US10.2, US10.3, BR-14, BR-16</p>
 *
 * @since 0.1.0
 */
public final class VerificationSteps {

    /** Zgłoszenie scenariusza. */
    static final UUID SUBMISSION =
        UUID.fromString("8d3e4f5a-6b7c-4d8e-9f0a-1b2c3d4e5f6a");

    /** Jedyne zdjęcie zgłoszenia scenariusza. */
    private static final UUID PHOTO =
        UUID.fromString("2e3f4a5b-6c7d-4e8f-9a0b-1c2d3e4f5a6b");

    /** Numery typów bariery zgłoszeń ze scenariuszy. */
    private static final Map<String, Integer> TYPES = Map.of(
        "zgłoszenie TB-13 „próg” ze zdjęciem", 13, "duplikat zgłoszenia", 1
    );

    /** Pola decyzji ustalone przez Weryfikatora. */
    private static final Map<String, String> FIELDS = Map.of(
        "typ TB-9 „niesprawna winda”", ",\"barrierType\":\"TB-9\"",
        "uciążliwość 3", ",\"severity\":3",
        "typ TB-13 „próg” z grupy „komunikacja pozioma” i uciążliwość 2",
        ",\"barrierType\":\"TB-13\",\"severity\":2"
    );

    /** Grupy barier zgłoszeń szybkich ze scenariuszy. */
    private static final Map<String, BarrierGroup> GROUPS = Map.of(
        "komunikacja pionowa", BarrierGroup.VERTICAL, "wejście i drzwi", BarrierGroup.ENTRANCE
    );

    /** Statusy serwera dla statusów ze scenariuszy. */
    private static final Map<String, String> STATUSES = Map.of(
        "odrzucone", "REJECTED", "przyjęte", "ACCEPTED"
    );

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Status odpowiedzi na próbę decyzji. */
    private final AttemptOutcome outcome;

    /** Token Weryfikatora ze scenariusza. */
    private String token;

    /** Pola przyjęcia ustalone przez Weryfikatora, od przecinka. */
    private String fixed;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     * @param photos Magazyn zdjęć zgłoszeń
     * @param outcome Status odpowiedzi na próbę, wspólny z innymi krokami
     */
    public VerificationSteps(
        final MockMvc http, final JdbcClient jdbc, final JdbcSubmissions submissions,
        final PhotoStore photos, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.submissions = submissions;
        this.photos = photos;
        this.outcome = outcome;
        this.token = "";
        this.fixed = "";
    }

    /** Weryfikatorzy działają z rolą Weryfikatora; token testowy ją niesie. */
    @Given("pierwszy i drugi Weryfikator są zalogowani z rolą Weryfikatora")
    public void verifiersLoggedIn() {
        this.token = "VERIFIER";
    }

    /**
     * Pierwszy Weryfikator bierze zgłoszenie szybkie z typem i uciążliwością.
     *
     * @param name Zgłoszenie słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given(
        "^pierwszy Weryfikator wziął (zgłoszenie TB-13 „próg” ze zdjęciem|duplikat zgłoszenia) .+$"
    )
    public void claimedClassified(final String name) throws Exception {
        this.claimed(BarrierGroup.OTHER, new BarrierType(VerificationSteps.TYPES.get(name)), 2);
    }

    /**
     * Pierwszy Weryfikator bierze zgłoszenie szybkie bez typu i uciążliwości.
     *
     * @param group Grupa barier słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^pierwszy Weryfikator wziął zgłoszenie szybkie z grupą barier „(.+)” z budynku \\S+$")
    public void claimedQuick(final String group) throws Exception {
        this.claimed(VerificationSteps.GROUPS.get(group), null, null);
    }

    /**
     * Weryfikator ustala tylko jedno z pól przyjęcia.
     *
     * @param field Ustalone pole słowami scenariusza
     */
    @Given("^ustalił tylko (typ TB-9 „niesprawna winda”|uciążliwość 3)$")
    @When("^ustali (typ TB-13 „próg” z grupy „komunikacja pozioma” i uciążliwość 2)$")
    public void fixedOnly(final String field) {
        this.fixed = VerificationSteps.FIELDS.get(field);
    }

    /**
     * Weryfikator próbuje odrzucić zgłoszenie z pustym powodem.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("spróbuje je odrzucić z pustym powodem odrzucenia")
    public void rejectsWithoutReason() throws Exception {
        this.decide("{\"decision\":\"REJECT\",\"version\":1}");
    }

    /**
     * Weryfikator próbuje przyjąć zgłoszenie z tym, co ustalił.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("spróbuje przyjąć zgłoszenie")
    @When("przyjmie zgłoszenie")
    public void accepts() throws Exception {
        this.decide(
            String.join("", "{\"decision\":\"ACCEPT\",\"version\":1", this.fixed, "}")
        );
    }

    /**
     * Serwer odmówił decyzji, a zgłoszenie nie ma statusu z decyzji.
     *
     * @param decision Decyzja ze scenariusza: odrzucone albo przyjęte
     */
    @Then("^zgłoszenie nie zostaje (odrzucone|przyjęte)$")
    public void notDecided(final String decision) {
        MatcherAssert.assertThat(
            "Zgłoszenie ma status z decyzji, której serwer nie powinien przyjąć",
            this.status(), Matchers.not(VerificationSteps.STATUSES.get(decision))
        );
        MatcherAssert.assertThat(
            "Serwer nie odmówił decyzji statusem 400",
            this.outcome.status(), Matchers.equalTo(400)
        );
    }

    /**
     * Zgłoszenie przyjęte ma na mapie typ i uciążliwość ustalone przez Weryfikatora.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("^na mapie jest bariera TB-13 „próg” z uciążliwością 2$")
    public void onMap() throws Exception {
        MatcherAssert.assertThat(
            "Gość nie widzi zgłoszenia przyjętego z typem TB-13 i uciążliwością 2",
            this.http.perform(
                MockMvcRequestBuilders.get("/submissions/{id}", VerificationSteps.SUBMISSION)
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString("\"barrierType\":\"TB-13\""),
                Matchers.containsString("\"severity\":2")
            )
        );
    }

    /** Zgłoszenie jest nadal w weryfikacji. */
    @Then("zgłoszenie ma nadal status „w weryfikacji”")
    public void stillInReview() {
        MatcherAssert.assertThat(
            "Status zgłoszenia zmienił się po odmowie decyzji",
            this.status(), Matchers.equalTo("IN_REVIEW")
        );
    }

    /** Usuwa zgłoszenie scenariusza. */
    @After
    public void removeSubmission() {
        this.jdbc.sql("DELETE FROM submissions WHERE id = :id")
            .param("id", VerificationSteps.SUBMISSION).update();
    }

    private void claimed(
        final BarrierGroup group, final @Nullable BarrierType type,
        final @Nullable Integer severity
    ) throws Exception {
        this.submissions.create(
            new NewSubmission(
                VerificationSteps.SUBMISSION,
                new SubmissionContent(
                    SubmissionKind.QUICK,
                    new SubmissionPlace(
                        new SubmissionPoint(17.05, 51.108), null, false, null, null
                    ),
                    new BarrierDetails(group, type, severity, null, false),
                    null, null, null, List.of(VerificationSteps.PHOTO)
                ),
                "autor-zgloszenia"
            )
        );
        this.photos.put(
            VerificationSteps.SUBMISSION, VerificationSteps.PHOTO,
            "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
        this.jdbc.sql("UPDATE submissions SET status = 'SENT' WHERE id = :id")
            .param("id", VerificationSteps.SUBMISSION).update();
        MatcherAssert.assertThat(
            "Weryfikator nie wziął zgłoszenia, serwer nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post(
                    "/submissions/{id}/claim", VerificationSteps.SUBMISSION
                ).header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.token))
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    private void decide(final String body) throws Exception {
        this.outcome.record(
            this.http.perform(
                MockMvcRequestBuilders.post(
                    "/submissions/{id}/review", VerificationSteps.SUBMISSION
                ).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.token))
                    .content(body)
            ).andReturn().getResponse().getStatus()
        );
    }

    private String status() {
        return this.jdbc.sql("SELECT status FROM submissions WHERE id = :id")
            .param("id", VerificationSteps.SUBMISSION).query(String.class).single();
    }
}
