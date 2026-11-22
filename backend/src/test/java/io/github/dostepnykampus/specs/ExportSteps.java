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
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.barrier.submission.persistence.JdbcSubmissions;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariuszy eksportu zgłoszeń przyjętych do QGIS. Otwarcie pliku w QGIS to odczyt CSV
 * jako warstwy punktów ze współrzędnymi z kolumn {@code x} i {@code y}.
 *
 * <p>Zgłoszenia powstają przez zapis produkcyjny, a budynki tylko wtedy, gdy ich w bazie nie
 * ma. Po scenariuszu znika wszystko, co on założył.</p>
 *
 * <p>Wymagania: US13.2, US13.3, BR-6, BR-22</p>
 *
 * @since 0.1.0
 */
public final class ExportSteps {

    /** Zgłoszenie TB-1 przy wejściu głównym A-1 z założeń scenariuszy. */
    private static final UUID STAIRS =
        UUID.fromString("5e1f2a3b-4c5d-4e6f-8a7b-9c0d1e2f3a4b");

    /** Zgłoszenie TB-13 w budynku C-13. */
    private static final UUID THRESHOLD =
        UUID.fromString("6f2a3b4c-5d6e-4f70-9b8c-0d1e2f3a4b5c");

    /** Wejście główne A-1 założone przez scenariusz. */
    private static final String ENTRANCE = "wejscie-eksport-a1";

    /** Konto autora zgłoszeń, które nie może trafić do pliku. */
    private static final String AUTHOR = "sub-autora-eksportu";

    /** Opis zgłoszenia TB-1 z polskimi znakami. */
    private static final String STAIRS_TEXT = "schody przy wejściu";

    /** Statusy serwera dla statusów ze scenariuszy. */
    private static final Map<String, String> STATUSES = Map.of(
        "przyjęte", "ACCEPTED", "wysłane", "SENT", "w weryfikacji", "IN_REVIEW",
        "odrzucone", "REJECTED", "archiwalne", "ARCHIVED"
    );

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /** Budynki założone przez scenariusz. */
    private final List<String> buildings;

    /** Format pobranego pliku. */
    private String format;

    /** Treść pobranego pliku. */
    private String body;

    /** Wiersze pliku CSV po otwarciu w QGIS. */
    private List<CSVRecord> rows;

    /** Kolumny pliku CSV po otwarciu w QGIS. */
    private List<String> columns;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     */
    public ExportSteps(
        final MockMvc http, final JdbcClient jdbc, final JdbcSubmissions submissions
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.submissions = submissions;
        this.buildings = new ArrayList<>(2);
        this.format = "";
        this.body = "";
        this.rows = List.of();
        this.columns = List.of();
    }

    /** Przy wejściu głównym A-1 jest przyjęte zgłoszenie TB-1. */
    @Given(
        "przy wejściu głównym A-1 jest przyjęte zgłoszenie TB-1 „schody przy wejściu” "
            + "z uciążliwością 3"
    )
    public void acceptedAtMainEntrance() {
        this.building("A-1", "1");
        this.jdbc.sql(
            String.join(
                " ",
                "INSERT INTO entrances (id, building_symbol, main, location, floor_id, marking,",
                "marking_date, marking_source)",
                "SELECT :id, symbol, TRUE, ST_PointOnSurface(outline), 'P0', 'NO_DATA',",
                "DATE '2026-11-01', 'IMPORT' FROM buildings WHERE symbol = 'A-1'"
            )
        ).param("id", ExportSteps.ENTRANCE).update();
        final double longitude = this.coordinate("ST_X(location)");
        final double latitude = this.coordinate("ST_Y(location)");
        this.add(
            ExportSteps.STAIRS, "ACCEPTED",
            new SubmissionPlace(
                new SubmissionPoint(longitude, latitude), null, false, "A-1", null
            ),
            new BarrierDetails(
                BarrierGroup.ENTRANCE, new BarrierType(1), 3, ExportSteps.STAIRS_TEXT, false
            )
        );
    }

    /**
     * W budynku C-13 jest zgłoszenie TB-13 w podanym statusie.
     *
     * @param status Status ze scenariusza, np. „w weryfikacji”
     */
    @Given("^w budynku C-13 jest zgłoszenie TB-13 „próg” ze statusem „(.+)”$")
    public void thresholdWithStatus(final String status) {
        this.building("C-13", "2");
        this.add(
            ExportSteps.THRESHOLD, ExportSteps.STATUSES.get(status),
            new SubmissionPlace(new SubmissionPoint(17.052, 51.102), null, false, "C-13", null),
            new BarrierDetails(BarrierGroup.OTHER, new BarrierType(13), 2, "próg", false)
        );
    }

    /**
     * Koordynator pobiera eksport w podanym formacie.
     *
     * @param name Nazwa formatu: GeoJSON albo CSV
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("^Koordynator pobierze eksport (GeoJSON|CSV)$")
    public void downloads(final String name) throws Exception {
        final String path;
        if ("CSV".equals(name)) {
            path = "/export.csv";
        } else {
            path = "/export.geojson";
        }
        MvcResult result = this.http.perform(
            MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
        ).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = this.http.perform(MockMvcRequestBuilders.asyncDispatch(result)).andReturn();
        }
        MatcherAssert.assertThat(
            "Serwer nie dał pliku eksportu ze statusem 200",
            result.getResponse().getStatus(), Matchers.equalTo(200)
        );
        this.format = name;
        this.body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Plik CSV otwarty w QGIS jako warstwa punktów ze współrzędnymi z kolumn x i y.
     *
     * @throws Exception Gdy treść nie jest poprawnym CSV
     */
    @When("otworzy plik w QGIS ze wskazanym układem EPSG:2180")
    public void opensInQgis() throws Exception {
        this.read();
        MatcherAssert.assertThat(
            "Plik nie ma kolumn x i y, z których QGIS czyta współrzędne punktów",
            this.columns, Matchers.hasItems("x", "y")
        );
    }

    /** Każdy wiersz pliku ma punkt o liczbowych współrzędnych. */
    @Then("QGIS pokazuje warstwę punktów")
    public void showsPointLayer() {
        MatcherAssert.assertThat(
            "Plik nie ma wierszy, więc warstwa punktów byłaby pusta",
            this.rows, Matchers.not(Matchers.empty())
        );
        for (final CSVRecord row : this.rows) {
            MatcherAssert.assertThat(
                "Współrzędna wiersza nie jest liczbą",
                List.of(row.get("x"), row.get("y")),
                Matchers.everyItem(Matchers.matchesPattern("\\d+(\\.\\d+)?"))
            );
        }
    }

    /** Bariera TB-1 leży w punkcie wejścia głównego A-1, z dokładnością do metra. */
    @Then("bariera TB-1 leży przy wejściu głównym A-1")
    public void liesAtMainEntrance() {
        final CSVRecord row = this.row(ExportSteps.STAIRS);
        final double easting = this.coordinate("ST_X(ST_Transform(location, 2180))");
        final double northing = this.coordinate("ST_Y(ST_Transform(location, 2180))");
        MatcherAssert.assertThat(
            "Bariera TB-1 leży dalej niż metr od wejścia głównego A-1 w PUWG 1992",
            List.of(
                Double.parseDouble(row.get("x")) - easting,
                Double.parseDouble(row.get("y")) - northing
            ),
            Matchers.everyItem(Matchers.closeTo(0.0, 1.0))
        );
    }

    /** Typ i opis zgłoszenia TB-1 mają polskie znaki bez zniekształceń. */
    @Then("typ „schody przy wejściu” ma polskie znaki bez zniekształceń")
    public void keepsPolishLetters() {
        final CSVRecord row = this.row(ExportSteps.STAIRS);
        MatcherAssert.assertThat(
            "Typ i opis TB-1 w pliku mają zniekształcone polskie znaki",
            List.of(row.get("barrierType"), row.get("description")),
            Matchers.contains("TB-1", ExportSteps.STAIRS_TEXT)
        );
        MatcherAssert.assertThat(
            "Plik ma znak zastępczy w miejscu polskiego znaku",
            this.body, Matchers.not(Matchers.containsString("�"))
        );
    }

    /**
     * Zgłoszenie TB-13 jest w pliku albo go w nim nie ma.
     *
     * @param presence „jest” albo „nie występuje”
     * @throws Exception Gdy treść nie jest poprawnym CSV
     */
    @Then("^zgłoszenie TB-13 (jest|nie występuje) w pliku$")
    public void thresholdPresence(final String presence) throws Exception {
        this.read();
        MatcherAssert.assertThat(
            "Obecność zgłoszenia TB-13 w pliku nie zgadza się ze statusem zgłoszenia",
            this.rows.stream().map(row -> row.get("submissionId")).toList()
                .contains(ExportSteps.THRESHOLD.toString()),
            Matchers.equalTo("jest".equals(presence))
        );
    }

    /**
     * Plik nie ma kolumn ani wartości z danymi autora.
     *
     * @throws Exception Gdy treść nie jest poprawnym CSV
     */
    @Then("w pliku nie ma imienia, adresu e-mail, identyfikatora konta ani pseudonimu autora")
    public void hasNoAuthorData() throws Exception {
        this.read();
        MatcherAssert.assertThat(
            "Plik ma kolumnę z danymi autora",
            this.columns.stream().map(name -> name.toLowerCase(Locale.ROOT)).toList(),
            Matchers.everyItem(
                Matchers.not(Matchers.matchesPattern(".*(author|name|mail|account|pseudonym).*"))
            )
        );
        MatcherAssert.assertThat(
            "Plik ma konto autora albo adres e-mail",
            this.body,
            Matchers.not(
                Matchers.anyOf(
                    Matchers.containsString(ExportSteps.AUTHOR), Matchers.containsString("@")
                )
            )
        );
    }

    /** Zgłoszenie ma tylko symbol budynku, bez konturu. */
    @Then(
        "zgłoszenie jest powiązane z budynkiem tylko symbolem budynku „A-1”, bez konturu budynku"
    )
    public void hasOnlyBuildingSymbol() {
        MatcherAssert.assertThat(
            "Zgłoszenie nie ma symbolu budynku A-1",
            this.row(ExportSteps.STAIRS).get("buildingSymbol"), Matchers.equalTo("A-1")
        );
        MatcherAssert.assertThat(
            "Plik ma kontur budynku", this.body, Matchers.not(Matchers.containsString("POLYGON"))
        );
    }

    /** Usuwa wszystko, co założył scenariusz. */
    @After
    public void cleanUp() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param("ids", List.of(ExportSteps.STAIRS, ExportSteps.THRESHOLD)).update();
        this.jdbc.sql("DELETE FROM entrances WHERE id = :id")
            .param("id", ExportSteps.ENTRANCE).update();
        for (final String symbol : this.buildings) {
            this.jdbc.sql("DELETE FROM buildings WHERE symbol = :symbol")
                .param("symbol", symbol).update();
        }
    }

    private double coordinate(final String expression) {
        return this.jdbc.sql(
            String.format("SELECT %s FROM entrances WHERE id = :id", expression)
        ).param("id", ExportSteps.ENTRANCE).query(Double.class).single();
    }

    private void building(final String symbol, final String shift) {
        final int present = this.jdbc.sql("SELECT count(*) FROM buildings WHERE symbol = :symbol")
            .param("symbol", symbol).query(Integer.class).single();
        if (present == 0) {
            new SampleBuilding(symbol, symbol, "BASIC")
                .wkt(
                    this.jdbc,
                    String.format(
                        "POLYGON((17.05%1$s 51.10%1$s,17.05%1$s1 51.10%1$s,17.05%1$s1 51.10%1$s1,"
                            + "17.05%1$s 51.10%1$s))",
                        shift
                    )
                );
            this.buildings.add(symbol);
        }
    }

    private void add(
        final UUID id, final String status, final SubmissionPlace place,
        final BarrierDetails barrier
    ) {
        this.submissions.create(
            new NewSubmission(
                id,
                new SubmissionContent(
                    SubmissionKind.QUICK, place, barrier, null, null, null,
                    List.of(UUID.randomUUID())
                ),
                ExportSteps.AUTHOR
            )
        );
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }

    private void read() throws Exception {
        MatcherAssert.assertThat(
            "Scenariusz czyta plik CSV, a pobrano inny format",
            this.format, Matchers.equalTo("CSV")
        );
        final CSVFormat parsing = CSVFormat.RFC4180.builder().setHeader()
            .setSkipHeaderRecord(true).get();
        try (CSVParser parser = parsing.parse(new StringReader(this.body))) {
            this.columns = parser.getHeaderNames();
            this.rows = parser.getRecords();
        }
    }

    private CSVRecord row(final UUID id) {
        return this.rows.stream().filter(row -> id.toString().equals(row.get("submissionId")))
            .findFirst().orElseThrow();
    }
}
