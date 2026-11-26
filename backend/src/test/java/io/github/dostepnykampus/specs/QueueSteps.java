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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kroki scenariuszy kolejki weryfikacji: kolejność, wzięcie, własne i podobne zgłoszenia.
 * Kolejka to {@code GET /review/queue}; kroki patrzą tylko na zgłoszenia scenariusza, bo baza
 * może mieć inne.
 *
 * <p>Pierwszy Weryfikator ma konto testowe z tokenu bez konta, drugi własne konto, a autor
 * zgłoszeń innych osób konto {@code autor-kolejki}.</p>
 *
 * <p>Wymagania: US10.4, US10.5, US10.7, US8.2, BR-1, BR-4, BR-11, BR-13</p>
 *
 * @since 0.1.0
 */
public final class QueueSteps {

    /** Pierwsze zgłoszenie scenariusza. */
    static final UUID ONE = UUID.fromString("e1f2a3b4-0001-4c5d-8e6f-7a8b9c0d1e2f");

    /** Drugie zgłoszenie scenariusza. */
    private static final UUID TWO = UUID.fromString("e1f2a3b4-0002-4c5d-8e6f-7a8b9c0d1e2f");

    /** Zgłoszenie przyjęte, którego dotyczy zmiana „bariera usunięta”. */
    private static final UUID PRIOR = UUID.fromString("e1f2a3b4-0003-4c5d-8e6f-7a8b9c0d1e2f");

    /** Jedyne zdjęcie każdego zgłoszenia scenariusza. */
    private static final UUID PHOTO = UUID.fromString("e1f2a3b4-0004-4c5d-8e6f-7a8b9c0d1e2f");

    /** Decyzje drugiego Weryfikatora według czynności ze scenariusza. */
    private static final Map<String, String> DECISIONS = Map.of(
        "przyjąć", "{\"decision\":\"ACCEPT\",\"version\":1}",
        "odrzucić", "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Nieczytelne\"}"
    );

    /** Token drugiego Weryfikatora. */
    private static final String SECOND = "VERIFIER.drugi-weryfikator";

    /** Miejsce przy wejściu głównym A-1. */
    private static final String ENTRANCE = "[17.06209,51.10785]";

    /** Grupy barier typów ze scenariuszy. */
    private static final Map<String, String> GROUPS = Map.of(
        "TB-1", "ENTRANCE_AND_DOORS", "TB-9", "VERTICAL_CIRCULATION",
        "TB-13", "HORIZONTAL_CIRCULATION"
    );

    /** Uciążliwość zgłoszeń scenariusza, poza zmianą „bariera usunięta”. */
    private static final String SEVERITY = ",\"severity\":2";

    /** Separator dnia i budynku w opisie wysłania. */
    private static final Pattern BUILDING = Pattern.compile(" z budynku ");

    /** Zapis dnia w scenariuszach. */
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Odczyt odpowiedzi JSON. */
    private final ObjectMapper mapper;

    /** Status odpowiedzi na próbę, wspólny z krokiem odmowy. */
    private final AttemptOutcome outcome;

    /** Zgłoszenia scenariusza według dnia wysłania. */
    private final Map<String, UUID> sent;

    /** Token pierwszego Weryfikatora. */
    private String first;

    /** Kolejka ostatnio otwarta przez pierwszego Weryfikatora, w JSON. */
    private String queue;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param photos Magazyn zdjęć zgłoszeń
     * @param mapper Odczyt odpowiedzi JSON
     * @param outcome Status odpowiedzi na próbę, wspólny z krokiem odmowy
     */
    public QueueSteps(
        final MockMvc http, final JdbcClient jdbc, final PhotoStore photos,
        final ObjectMapper mapper, final AttemptOutcome outcome
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.photos = photos;
        this.mapper = mapper;
        this.outcome = outcome;
        this.sent = new HashMap<>(2);
        this.first = "VERIFIER";
        this.queue = "[]";
    }

    /**
     * Autor wysłał dwa zgłoszenia w podane dni z podanych budynków.
     *
     * @param earlier Dzień i budynek pierwszego zgłoszenia, np. „27.10.2026 z budynku C-13”
     * @param later Dzień i budynek drugiego zgłoszenia
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^w kolejce są zgłoszenia wysłane (\\S+ z budynku \\S+) i (\\S+ z budynku \\S+)$")
    void sentOn(final String earlier, final String later) throws Exception {
        this.sentOn(QueueSteps.ONE, earlier);
        this.sentOn(QueueSteps.TWO, later);
    }

    /** Pierwszy Weryfikator ma też rolę Zbierającego; token testowy niesie obie. */
    @Given("pierwszy Weryfikator ma też rolę Zbierającego")
    void alsoCollector() {
        this.first = "COLLECTOR+VERIFIER";
    }

    /**
     * Pierwszy Weryfikator jako Zbierający wysłał zgłoszenie schodów przy A-1.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given(
        "jako Zbierający wysłał zgłoszenie TB-1 „schody przy wejściu” przy wejściu głównym A-1"
    )
    void sentOwn() throws Exception {
        this.send(
            this.first, QueueSteps.ONE,
            QueueSteps.body("A-1", "TB-1", QueueSteps.SEVERITY)
        );
    }

    /**
     * Pierwszy Weryfikator wysłał zmianę „bariera usunięta” cudzego zgłoszenia przyjętego.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("wysłał zmianę „bariera usunięta” zgłoszenia TB-9 „niesprawna winda” w budynku D-2")
    void sentRemoval() throws Exception {
        this.barrier(QueueSteps.PRIOR, "D-2", "TB-9");
        this.jdbc.sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", QueueSteps.PRIOR).update();
        this.send(
            this.first, QueueSteps.TWO,
            QueueSteps.body(
                "D-2", "TB-9",
                String.format(
                    ",\"replacesSubmissionId\":\"%s\",\"barrierRemoved\":true",
                    QueueSteps.PRIOR
                )
            )
        );
    }

    /**
     * Autor wysłał dwa zgłoszenia schodów 2 m od siebie przy wejściu głównym A-1.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given(
        "2 m od siebie przy wejściu głównym A-1 wysłano dwa zgłoszenia TB-1 „schody przy wejściu”"
    )
    void sentNearby() throws Exception {
        this.barrier(QueueSteps.ONE, "A-1", "TB-1");
        this.send(
            "COLLECTOR.autor-kolejki", QueueSteps.TWO,
            QueueSteps.body("A-1", "TB-1", QueueSteps.SEVERITY)
                .replace(QueueSteps.ENTRANCE, "[17.06209,51.107868]")
        );
    }

    /**
     * Pierwszy Weryfikator otwiera kolejkę weryfikacji.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("pierwszy Weryfikator otworzy kolejkę weryfikacji")
    @When("wyjdzie ze zgłoszenia i otworzy kolejkę weryfikacji")
    void opensQueue() throws Exception {
        this.queue = this.queue(this.first);
    }

    /**
     * Zgłoszenia scenariusza stoją w kolejce od najdawniej wysłanego.
     *
     * @param earlier Dzień wysłania zgłoszenia, które ma być pierwsze
     * @param later Dzień wysłania zgłoszenia, które ma być drugie
     */
    @Then("^pierwsze jest zgłoszenie z (\\S+), a drugie z (\\S+)$")
    void ordered(final String earlier, final String later) {
        MatcherAssert.assertThat(
            "Kolejka nie stawia najdawniej wysłanego zgłoszenia na początku",
            this.ids(this.queue),
            Matchers.contains(
                this.sent.get(earlier).toString(), this.sent.get(later).toString()
            )
        );
    }

    /** Każde zgłoszenie scenariusza w kolejce ma rodzaj, budynek i datę wysłania. */
    @Then("przy każdym widzi rodzaj zgłoszenia, budynek i datę wysłania")
    void describesEach() {
        MatcherAssert.assertThat(
            "Zgłoszeniu w kolejce brakuje rodzaju, budynku albo daty wysłania",
            this.own(this.queue).stream().map(
                item -> String.join(
                    " ", item.path("kind").asString(), item.path("buildingSymbol").asString(),
                    item.path("createdAt").asString().substring(0, 10)
                )
            ).toList(),
            Matchers.containsInAnyOrder(
                String.join(" ", "PRECISE", "A-1", "2026-10-26"),
                String.join(" ", "PRECISE", "C-13", "2026-10-27")
            )
        );
    }

    /** W kolejce pierwszego Weryfikatora nie ma jego zgłoszeń. */
    @Then("nie widzi w niej żadnego z tych zgłoszeń")
    void seesNoneOwn() {
        MatcherAssert.assertThat(
            "Weryfikator widzi w kolejce własne zgłoszenie albo własną zmianę",
            this.own(this.queue), Matchers.empty()
        );
    }

    /**
     * Drugi Weryfikator ma oba zgłoszenia scenariusza w kolejce.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("drugi Weryfikator widzi w swojej kolejce oba zgłoszenia")
    void secondSeesBoth() throws Exception {
        MatcherAssert.assertThat(
            "Drugi Weryfikator nie widzi obu zgłoszeń pierwszego",
            this.own(this.queue(QueueSteps.SECOND)), Matchers.hasSize(2)
        );
    }

    /** Oba zgłoszenia scenariusza są w kolejce osobno. */
    @Then("widzi w niej oba zgłoszenia osobno")
    void seesBoth() {
        MatcherAssert.assertThat(
            "Kolejka nie pokazuje obu podobnych zgłoszeń osobno",
            this.ids(this.queue),
            Matchers.hasItems(QueueSteps.ONE.toString(), QueueSteps.TWO.toString())
        );
    }

    /** Podobne zgłoszenia są nadal wysłane, bez wskazania duplikatu. */
    @Then("żadne z nich nie jest odrzucone ani połączone z drugim")
    void untouched() {
        MatcherAssert.assertThat(
            "System sam odrzucił albo połączył podobne zgłoszenia",
            this.own(this.queue).stream().map(
                item -> String.join(
                    " ", item.path("status").asString(),
                    String.valueOf(item.has("duplicateOfSubmissionId"))
                )
            ).toList(),
            Matchers.contains("SENT false", "SENT false")
        );
    }

    /**
     * Autor wysłał zgłoszenie progu z budynku.
     *
     * @param building Budynek zgłoszenia
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^w kolejce jest zgłoszenie TB-13 „próg” z budynku (\\S+) ze statusem „wysłane”$")
    void threshold(final String building) throws Exception {
        this.barrier(QueueSteps.ONE, building, "TB-13");
    }

    /**
     * Pierwszy Weryfikator wziął zgłoszenie progu i nie podjął decyzji.
     *
     * @param building Budynek zgłoszenia
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given(
        "^pierwszy Weryfikator wziął zgłoszenie TB-13 „próg” z budynku (\\S+)(?: i nie podjął .+)?$"
    )
    void claimedThreshold(final String building) throws Exception {
        this.threshold(building);
        this.claims();
    }

    /**
     * Pierwszy Weryfikator bierze zgłoszenie scenariusza.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("pierwszy Weryfikator weźmie to zgłoszenie")
    void claims() throws Exception {
        MatcherAssert.assertThat(
            "Pierwszy Weryfikator nie wziął zgłoszenia, serwer nie dał 200",
            this.status(
                MockMvcRequestBuilders.post("/submissions/{id}/claim", QueueSteps.ONE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.first))
            ),
            Matchers.equalTo(200)
        );
    }

    /**
     * Drugi Weryfikator próbuje obsłużyć zgłoszenie wzięte przez pierwszego.
     *
     * @param action Czynność słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^drugi Weryfikator spróbuje (wziąć|przyjąć|odrzucić) to zgłoszenie$")
    void secondTries(final String action) throws Exception {
        if ("wziąć".equals(action)) {
            this.outcome.record(
                this.status(
                    MockMvcRequestBuilders.post("/submissions/{id}/claim", QueueSteps.ONE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(QueueSteps.SECOND))
                ),
                409
            );
        } else {
            this.outcome.record(
                this.status(
                    MockMvcRequestBuilders.post("/submissions/{id}/review", QueueSteps.ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(QueueSteps.SECOND))
                        .content(QueueSteps.DECISIONS.get(action))
                ),
                403
            );
        }
    }

    /** Zgłoszenie jest nadal w weryfikacji u pierwszego Weryfikatora. */
    @Then("zgłoszenie nadal obsługuje pierwszy Weryfikator")
    void stillFirst() {
        MatcherAssert.assertThat(
            "Zgłoszenie zmieniło Weryfikatora albo status po próbie drugiego",
            this.stored("status || ' ' || reviewer"),
            Matchers.equalTo("IN_REVIEW konto-testowe")
        );
    }

    /** Wzięte zgłoszenie jest w weryfikacji. */
    @Then("zgłoszenie ma status „w weryfikacji”")
    void inReview() {
        MatcherAssert.assertThat(
            "Wzięte zgłoszenie nie ma statusu „w weryfikacji”",
            this.stored("status"), Matchers.equalTo("IN_REVIEW")
        );
    }

    /**
     * Drugi Weryfikator nie ma wziętego zgłoszenia w kolejce.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("drugi Weryfikator nie widzi go w swojej kolejce")
    void hiddenFromSecond() throws Exception {
        MatcherAssert.assertThat(
            "Drugi Weryfikator widzi zgłoszenie wzięte przez pierwszego",
            this.ids(this.queue(QueueSteps.SECOND)),
            Matchers.not(Matchers.hasItem(QueueSteps.ONE.toString()))
        );
    }

    /** Wzięte zgłoszenie jest w kolejce pierwszego Weryfikatora jako jego wzięte. */
    @Then("widzi to zgłoszenie osobno, wśród wziętych przez siebie")
    void seesClaimed() {
        MatcherAssert.assertThat(
            "Kolejka pierwszego Weryfikatora nie pokazuje wziętego zgłoszenia jako wziętego",
            this.own(this.queue).stream().map(item -> item.path("status").asString()).toList(),
            Matchers.contains("IN_REVIEW")
        );
    }

    /**
     * Pierwszy Weryfikator otwiera wzięte zgłoszenie i odrzuca je z powodem.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("może je otworzyć i podjąć decyzję")
    void decides() throws Exception {
        MatcherAssert.assertThat(
            "Pierwszy Weryfikator nie otworzył albo nie rozstrzygnął wziętego zgłoszenia",
            List.of(
                this.status(
                    MockMvcRequestBuilders.get("/submissions/{id}", QueueSteps.ONE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.first))
                ),
                this.status(
                    MockMvcRequestBuilders.post("/submissions/{id}/review", QueueSteps.ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(this.first))
                        .content(this.rejection())
                )
            ),
            Matchers.contains(200, 200)
        );
    }

    /** Usuwa zgłoszenia scenariusza. */
    @After
    void remove() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param("ids", List.of(QueueSteps.ONE, QueueSteps.TWO, QueueSteps.PRIOR)).update();
    }

    private void sentOn(final UUID id, final String sending) throws Exception {
        final List<String> parts = List.of(QueueSteps.BUILDING.split(sending, 2));
        this.barrier(id, parts.getLast(), "TB-13");
        this.jdbc.sql(
            "UPDATE submissions SET created_at = CAST(:day AS TIMESTAMPTZ) WHERE id = :id"
        ).param("day", LocalDate.parse(parts.getFirst(), QueueSteps.DAY).toString())
            .param("id", id).update();
        this.sent.put(parts.getFirst(), id);
    }

    private void send(final String token, final UUID id, final String body) throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia scenariusza, nie dał 201",
            this.status(
                MockMvcRequestBuilders.post("/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(token))
                    .content(String.format("{\"id\":\"%s\",%s", id, body.substring(1)))
            ),
            Matchers.equalTo(201)
        );
        this.photos.put(id, QueueSteps.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8));
    }

    private String stored(final String column) {
        return this.jdbc.sql(
            String.format("SELECT %s FROM submissions WHERE id = :id", column)
        ).param("id", QueueSteps.ONE).query(String.class).single();
    }

    private String rejection() {
        return String.format(
            "{\"decision\":\"REJECT\",\"version\":%d,%s}",
            this.own(this.queue).getFirst().path("version").asInt(),
            "\"rejectionReason\":\"Zdjęcie nie pokazuje bariery\""
        );
    }

    private String queue(final String token) throws Exception {
        return this.http.perform(
            MockMvcRequestBuilders.get("/review/queue")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(token))
        ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int status(final RequestBuilder request) throws Exception {
        return this.http.perform(request).andReturn().getResponse().getStatus();
    }

    private void barrier(final UUID id, final String building, final String type)
        throws Exception {
        this.send(
            "COLLECTOR.autor-kolejki", id, QueueSteps.body(building, type, QueueSteps.SEVERITY)
        );
    }

    private static String body(final String building, final String type, final String tail) {
        return String.join(
            "",
            "{\"kind\":\"PRECISE\",\"location\":{\"type\":\"Point\",\"coordinates\":",
            QueueSteps.ENTRANCE, "},\"buildingSymbol\":\"", building, "\",",
            "\"floor\":{\"number\":0},\"barrierGroup\":\"", QueueSteps.GROUPS.get(type),
            "\",\"barrierType\":\"", type, "\"", tail, ",\"photoIds\":[\"",
            QueueSteps.PHOTO.toString(), "\"]}"
        );
    }

    private List<JsonNode> own(final String json) {
        return StreamSupport.stream(this.mapper.readTree(json).spliterator(), false).filter(
            item -> List.of(QueueSteps.ONE.toString(), QueueSteps.TWO.toString())
                .contains(item.path("id").asString())
        ).toList();
    }

    private List<String> ids(final String json) {
        return this.own(json).stream().map(item -> item.path("id").asString()).toList();
    }
}
