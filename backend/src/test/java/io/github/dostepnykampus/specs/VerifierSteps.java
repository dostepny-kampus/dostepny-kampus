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
import io.github.dostepnykampus.AsCollector;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Kroki scenariuszy decyzji Weryfikatora o zgłoszeniu z pomiaru precyzyjnego: przyjęcie
 * i odrzucenie trasą {@code POST /submissions/{id}/review} oraz lista podobnych zgłoszeń
 * trasą {@code GET /submissions/{id}/similar}.
 *
 * <p>Autorem zgłoszeń jest inne konto niż Weryfikator z tokenu testowego; krok, w którym autor
 * odczytuje zgłoszenie, podmienia autora na konto z tokenu. Dzień weryfikacji zapisuje zegar
 * bazy, więc krok przyjęcia z datą ustawia go po decyzji.</p>
 *
 * <p>Wymagania: US10.1, BR-13, BR-15, BR-16, BR-17</p>
 *
 * @since 0.1.0
 */
public final class VerifierSteps {

    /** Zgłoszenie sprawdzane przez Weryfikatora. */
    static final UUID CHECKED = UUID.fromString("b4c5d6e7-0001-4f8a-9b0c-1d2e3f4a5b6c");

    /** Jedyne zdjęcie sprawdzanego zgłoszenia. */
    static final UUID PHOTO = UUID.fromString("b4c5d6e7-0003-4f8a-9b0c-1d2e3f4a5b6c");

    /** Zgłoszenie zapisane obok sprawdzanego. */
    private static final UUID OTHER = UUID.fromString("b4c5d6e7-0002-4f8a-9b0c-1d2e3f4a5b6c");

    /** Jedyne zdjęcie drugiego zgłoszenia. */
    private static final UUID NEXT_PHOTO = UUID.fromString("b4c5d6e7-0004-4f8a-9b0c-1d2e3f4a5b6c");

    /** Miejsce sprawdzanego zgłoszenia. */
    private static final SubmissionPoint ORIGIN = new SubmissionPoint(17.05, 51.108);

    /** Zapis dnia w scenariuszach. */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Puste żądanie gościa bez tokenu. */
    private static final RequestPostProcessor GUEST = request -> request;

    /** Statusy serwera dla statusów ze scenariuszy. */
    private static final Map<String, String> STATUSES = Map.of(
        "wysłane", "SENT", "w weryfikacji", "IN_REVIEW", "przyjęte", "ACCEPTED",
        "odrzucone", "REJECTED", "archiwalne", "ARCHIVED"
    );

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Odpowiedź serwera z listą podobnych zgłoszeń. */
    private String listed;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     * @param photos Magazyn zdjęć zgłoszeń
     */
    public VerifierSteps(
        final MockMvc http, final JdbcClient jdbc, final JdbcSubmissions submissions,
        final PhotoStore photos
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.submissions = submissions;
        this.photos = photos;
        this.listed = "";
    }

    @Given("^pierwszy Weryfikator wziął zgłoszenie TB-1 „schody przy wejściu” przy wejściu .+$")
    void claimedAtEntrance() throws Exception {
        this.claimed(1, "A-1");
    }

    @Given("^pierwszy Weryfikator wziął zgłoszenie TB-1, a 2 m od niego jest przyjęte .+$")
    void claimedWithEarlier() throws Exception {
        this.claimed(1, "A-1");
        this.next(1, 2, 0, "ACCEPTED");
    }

    @Given("^pierwszy Weryfikator wziął zgłoszenie z pomiaru precyzyjnego TB-13 .+ C-13$")
    void claimedPrecise() throws Exception {
        this.claimed(13, "C-13");
    }

    @Given("^(\\d+) m od niego jest zgłoszenie TB-13 „próg” ze statusem „(.+)”$")
    void nearby(final String metres, final String status) {
        this.next(13, Double.parseDouble(metres), 0, VerifierSteps.STATUSES.get(status));
    }

    @Given("^5 m od niego jest przyjęte zgłoszenie TB-13 „próg” z innego piętra$")
    void onOtherFloor() {
        this.next(13, 5, 1, "ACCEPTED");
    }

    @When("^przyjmie je (\\d{2}\\.\\d{2}\\.\\d{4})$")
    void acceptsOn(final String day) throws Exception {
        this.decided("{\"decision\":\"ACCEPT\",\"version\":1}");
        this.jdbc.sql(
            """
            UPDATE submissions
            SET verified_at = TO_DATE(:day, 'DD.MM.YYYY') + INTERVAL '12 hours'
            WHERE id = :id
            """
        ).param("day", day).param("id", VerifierSteps.CHECKED).update();
    }

    @When("odrzuci sprawdzane zgłoszenie jako duplikat")
    void rejectsDuplicate() throws Exception {
        this.decided(
            String.join(
                "", "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Duplikat\",",
                "\"duplicateOfSubmissionId\":\"", VerifierSteps.OTHER.toString(), "\"}"
            )
        );
    }

    @When("^odrzuci (?:je|zmianę) z powodem „(.+)”$")
    void rejects(final String reason) throws Exception {
        this.decided(
            String.join(
                "", "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"", reason, "\"}"
            )
        );
    }

    @When("pierwszy Weryfikator przejrzy podobne zgłoszenia")
    void listsSimilar() throws Exception {
        this.listed = this.read("/similar", new AsVerifier());
    }

    @Then("bariera jest na mapie i na karcie budynku A-1 bez osobnego kroku publikacji")
    void onMapAndCard() throws Exception {
        MatcherAssert.assertThat(
            "Mapa albo karta budynku A-1 nie pokazuje zgłoszenia przyjętego przez Weryfikatora",
            List.of(
                this.get("/submissions", VerifierSteps.GUEST),
                this.get("/submissions?buildingSymbol=A-1", VerifierSteps.GUEST)
            ),
            Matchers.everyItem(Matchers.containsString(VerifierSteps.CHECKED.toString()))
        );
    }

    @Then("^przy zgłoszeniu widać zdjęcie i datę weryfikacji (\\d{2}\\.\\d{2}\\.\\d{4})$")
    void photoAndDate(final String day) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie przyjęte nie pokazuje zdjęcia albo daty weryfikacji",
            this.read("", VerifierSteps.GUEST),
            Matchers.allOf(
                Matchers.containsString(
                    String.format("\"photoIds\":[\"%s\"]", VerifierSteps.PHOTO)
                ),
                Matchers.containsString(
                    String.format("\"verifiedAt\":\"%s", LocalDate.parse(day, VerifierSteps.DAY))
                )
            )
        );
    }

    @Then("powód odrzucenia wskazuje wcześniejsze zgłoszenie przyjęte")
    @Then("sprawdzane zgłoszenie ma status „odrzucone”")
    void rejectedAsDuplicate() throws Exception {
        MatcherAssert.assertThat(
            "Duplikat nie jest odrzucony ze wskazaniem wcześniejszego przyjętego zgłoszenia",
            this.read("", new AsVerifier()),
            Matchers.allOf(
                Matchers.containsString("\"status\":\"REJECTED\""),
                Matchers.containsString(
                    String.format("\"duplicateOfSubmissionId\":\"%s\"", VerifierSteps.OTHER)
                )
            )
        );
    }

    @Then("^zgłoszenie TB-13 (jest|nie jest) widoczne na liście podobnych zgłoszeń$")
    void visibility(final String visible) {
        MatcherAssert.assertThat(
            String.format("Serwer oddał podobne zgłoszenia: %s", this.listed),
            this.listed.contains(VerifierSteps.OTHER.toString()),
            Matchers.equalTo("jest".equals(visible))
        );
    }

    @Then("przy zgłoszeniu TB-13 widzi typ bariery, zdjęcie, odległość 5 m i piętro")
    void details() {
        MatcherAssert.assertThat(
            String.format("Zgłoszenie bez typu, zdjęcia, odległości albo piętra: %s", this.listed),
            this.listed,
            Matchers.allOf(
                Matchers.containsString("\"barrierType\":\"TB-13\""),
                Matchers.containsString(
                    String.format("\"photoIds\":[\"%s\"]", VerifierSteps.NEXT_PHOTO)
                ),
                Matchers.containsString("\"distanceMeters\":5.0"),
                Matchers.containsString("\"floor\":{\"number\":1")
            )
        );
    }

    @Then("^autor widzi zgłoszenie ze statusem „odrzucone” i powodem odrzucenia „(.+)”$")
    void authorSees(final String reason) throws Exception {
        this.jdbc.sql("UPDATE submissions SET author = 'konto-testowe' WHERE id = :id")
            .param("id", VerifierSteps.CHECKED).update();
        MatcherAssert.assertThat(
            "Autor nie widzi zgłoszenia ze statusem REJECTED i powodem odrzucenia",
            this.read("", new AsCollector()),
            Matchers.allOf(
                Matchers.containsString("\"status\":\"REJECTED\""),
                Matchers.containsString(String.format("\"rejectionReason\":\"%s\"", reason))
            )
        );
    }

    @After
    void remove() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param("ids", List.of(VerifierSteps.CHECKED, VerifierSteps.OTHER)).update();
    }

    private void claimed(final int type, final String building) throws Exception {
        this.store(
            VerifierSteps.CHECKED, VerifierSteps.PHOTO, type, VerifierSteps.ORIGIN, building, 0,
            "SENT"
        );
        MatcherAssert.assertThat(
            "Weryfikator nie wziął zgłoszenia, serwer nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions/{id}/claim", VerifierSteps.CHECKED)
                    .with(new AsVerifier())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    private void next(final int type, final double metres, final int floor, final String status) {
        this.store(
            VerifierSteps.OTHER, VerifierSteps.NEXT_PHOTO, type, this.north(metres), "A-1", floor,
            status
        );
    }

    private void store(
        final UUID id, final UUID photo, final int type, final SubmissionPoint point,
        final String building, final int floor, final String status
    ) {
        this.submissions.create(
            new NewSubmission(
                id,
                new SubmissionContent(
                    SubmissionKind.PRECISE,
                    new SubmissionPlace(
                        point, null, false, building, new SubmissionFloor(null, floor, false)
                    ),
                    new BarrierDetails(null, new BarrierType(type), 2, null, false),
                    null, null, null, List.of(photo)
                ),
                "autor-zgloszenia"
            )
        );
        this.photos.put(id, photo, "zdjęcie".getBytes(StandardCharsets.UTF_8));
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }

    private SubmissionPoint north(final double metres) {
        return this.jdbc.sql(
            """
            SELECT ST_X(CAST(moved AS geometry)), ST_Y(CAST(moved AS geometry)) FROM (
                SELECT ST_Project(origin, :metres, 0) AS moved
                FROM (
                    SELECT CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography) AS origin
                ) AS spot
            ) AS shifted
            """
        ).param("metres", metres).param("lon", VerifierSteps.ORIGIN.longitude())
            .param("lat", VerifierSteps.ORIGIN.latitude())
            .query((row, num) -> new SubmissionPoint(row.getDouble(1), row.getDouble(2)))
            .single();
    }

    private void decided(final String body) throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął decyzji Weryfikatora o wziętym zgłoszeniu, nie dał 200",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions/{id}/review", VerifierSteps.CHECKED)
                    .contentType(MediaType.APPLICATION_JSON).with(new AsVerifier()).content(body)
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(200)
        );
    }

    private String read(final String suffix, final RequestPostProcessor who) throws Exception {
        return this.get(String.format("/submissions/%s%s", VerifierSteps.CHECKED, suffix), who);
    }

    private String get(final String uri, final RequestPostProcessor who) throws Exception {
        return this.http.perform(MockMvcRequestBuilders.get(uri).with(who))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
