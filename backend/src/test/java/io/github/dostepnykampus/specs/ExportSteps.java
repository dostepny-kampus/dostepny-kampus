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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Kroki scenariuszy eksportu zgłoszeń przyjętych do QGIS. Otwarcie pliku CSV w QGIS to odczyt
 * warstwy punktów ze współrzędnymi z kolumn {@code x} i {@code y}, a pliku GeoJSON odczyt
 * kolekcji punktów bez pola {@code crs}, czyli w WGS84 według RFC 7946.
 *
 * <p>Zgłoszenia powstają przez zapis produkcyjny, a budynki tylko wtedy, gdy ich w bazie nie
 * ma. Plik pobiera zwykły klient HTTP: strumień eksportu nie przechodzi przez {@code MockMvc}.
 * Po scenariuszu znika wszystko, co on założył.</p>
 *
 * <p>Wymagania: US13.1, US13.2, US13.3, BR-6, BR-19, BR-22</p>
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

    /** Zgłoszenie TB-9 niesprawnej windy w budynku D-2, zastąpione zmianą. */
    private static final UUID ELEVATOR =
        UUID.fromString("7a3b4c5d-6e7f-4a81-8c9d-1e2f3a4b5c6d");

    /** Przyjęta zmiana „bariera usunięta” dla windy TB-9 w budynku D-2. */
    private static final UUID REMOVAL =
        UUID.fromString("8b4c5d6e-7f80-4b92-9dae-2f3a4b5c6d7e");

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

    /** Port serwera w środowisku aplikacji. */
    private final Environment environment;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /** Parser pliku GeoJSON. */
    private final ObjectMapper mapper;

    /** Budynki założone przez scenariusz. */
    private final List<String> buildings;

    /** Pobrany plik. */
    private @Nullable ExportFile file;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param environment Port serwera w środowisku aplikacji
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     * @param mapper Parser pliku GeoJSON
     */
    public ExportSteps(
        final Environment environment, final JdbcClient jdbc, final JdbcSubmissions submissions,
        final ObjectMapper mapper
    ) {
        this.environment = environment;
        this.jdbc = jdbc;
        this.submissions = submissions;
        this.mapper = mapper;
        this.buildings = new ArrayList<>(3);
    }

    /**
     * Przy wejściu głównym A-1 jest przyjęte zgłoszenie TB-1.
     *
     * @param severity Uciążliwość zgłoszenia
     */
    @Given("^przy wejściu głównym A-1 jest przyjęte zgłoszenie .+ z uciążliwością (\\d)$")
    public void acceptedAtMainEntrance(final int severity) {
        this.building("A-1", 17.051, 51.101);
        this.jdbc.sql(
            String.join(
                " ",
                "INSERT INTO entrances (id, building_symbol, main, location, floor_id, marking,",
                "marking_date, marking_source)",
                "SELECT :id, symbol, TRUE, ST_PointOnSurface(outline), 'P0', 'NO_DATA',",
                "DATE '2026-11-01', 'IMPORT' FROM buildings WHERE symbol = 'A-1'"
            )
        ).param("id", ExportSteps.ENTRANCE).update();
        this.add(
            ExportSteps.STAIRS, "ACCEPTED", null,
            new SubmissionPlace(
                new SubmissionPoint(
                    this.coordinate("ST_X(location)"), this.coordinate("ST_Y(location)")
                ),
                null, false, "A-1", null
            ),
            new BarrierDetails(
                BarrierGroup.ENTRANCE, new BarrierType(1), severity, ExportSteps.STAIRS_TEXT,
                false
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
        this.building("C-13", 17.052, 51.102);
        this.add(
            ExportSteps.THRESHOLD, ExportSteps.STATUSES.get(status), null,
            new SubmissionPlace(new SubmissionPoint(17.052, 51.102), null, false, "C-13", null),
            new BarrierDetails(BarrierGroup.OTHER, new BarrierType(13), 2, "próg", false)
        );
    }

    /**
     * W budynku D-2 przyjęto zmianę „bariera usunięta” dla niesprawnej windy TB-9, która
     * zastąpiła wcześniejsze zgłoszenie windy.
     */
    @Given("przyjęto zgłoszenie „bariera usunięta” dla niesprawnej windy TB-9 w budynku D-2")
    public void removedElevator() {
        this.building("D-2", 17.053, 51.103);
        final SubmissionPlace place =
            new SubmissionPlace(new SubmissionPoint(17.053, 51.103), null, false, "D-2", null);
        this.add(
            ExportSteps.ELEVATOR, "ARCHIVED", null, place,
            new BarrierDetails(BarrierGroup.VERTICAL, new BarrierType(9), 3, null, false)
        );
        this.add(
            ExportSteps.REMOVAL, "ACCEPTED", ExportSteps.ELEVATOR, place,
            new BarrierDetails(BarrierGroup.VERTICAL, new BarrierType(9), null, null, true)
        );
    }

    /**
     * Koordynator pobiera eksport w podanym formacie.
     *
     * @param name Nazwa formatu: GeoJSON albo CSV
     * @throws Exception Gdy żądanie albo odczyt pliku się nie uda
     */
    @When("^Koordynator pobierze eksport (GeoJSON|CSV)$")
    public void downloads(final String name) throws Exception {
        this.file = ExportFile.download(
            this.environment.getRequiredProperty("local.server.port"), name, this.mapper
        );
    }

    /** Plik GeoJSON otwarty w QGIS daje warstwę punktów ze zgłoszeniami. */
    @When("otworzy plik w QGIS")
    public void opensGeoJson() {
        MatcherAssert.assertThat(
            "Plik nie ma punktów, więc warstwa w QGIS byłaby pusta",
            this.rows().keySet(), Matchers.not(Matchers.empty())
        );
    }

    /** Plik nie podaje własnego układu, więc QGIS bierze WGS84 z RFC 7946 bez pytania. */
    @Then("QGIS nie pyta o układ współrzędnych")
    public void needsNoCrs() {
        MatcherAssert.assertThat(
            "Plik ma pole crs, a GeoJSON według RFC 7946 ma być w WGS84 bez niego",
            this.loaded().ownCrs(), Matchers.is(false)
        );
    }

    /** Każdy punkt ma długość i szerokość geograficzną w zakresie WGS84. */
    @Then("plik ma współrzędne w WGS84")
    public void hasWgs84() {
        for (final Map<String, String> row : this.rows().values()) {
            MatcherAssert.assertThat(
                "Punkt ma współrzędne spoza zakresu WGS84",
                List.of(
                    Math.abs(Double.parseDouble(row.get("lon"))) / 180.0,
                    Math.abs(Double.parseDouble(row.get("lat"))) / 90.0
                ),
                Matchers.everyItem(Matchers.lessThanOrEqualTo(1.0))
            );
        }
    }

    /** Bariera TB-1 leży w punkcie wejścia głównego A-1 w WGS84, z dokładnością do metra. */
    @Then("bariera TB-1 leży przy wejściu głównym A-1 na podkładzie OpenStreetMap")
    public void liesAtMainEntranceOnMap() {
        final Map<String, String> row = this.rows().get(ExportSteps.STAIRS.toString());
        MatcherAssert.assertThat(
            "Bariera TB-1 leży dalej niż metr od wejścia głównego A-1 w WGS84",
            this.jdbc.sql(
                String.join(
                    " ",
                    "SELECT ST_Distance(location::geography,",
                    "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography)",
                    "FROM entrances WHERE id = :id"
                )
            ).param("lon", Double.parseDouble(row.get("lon")))
                .param("lat", Double.parseDouble(row.get("lat")))
                .param("id", ExportSteps.ENTRANCE).query(Double.class).single(),
            Matchers.lessThan(1.0)
        );
    }

    /** W pliku nie ma windy z budynku D-2: ani zgłoszenia windy, ani jego usunięcia. */
    @Then("w pliku nie ma niesprawnej windy z budynku D-2")
    public void hasNoRemovedElevator() {
        MatcherAssert.assertThat(
            "Plik ma zgłoszenie windy z budynku D-2 albo zmianę „bariera usunięta”",
            this.rows().values().stream().map(row -> row.get("buildingSymbol")).toList(),
            Matchers.not(Matchers.hasItem("D-2"))
        );
    }

    /** W pliku jest bariera TB-1 z budynku A-1. */
    @Then("w pliku jest bariera TB-1 przy wejściu głównym A-1")
    public void hasStairs() {
        MatcherAssert.assertThat(
            "Pliku nie ma bariery TB-1 przy wejściu głównym A-1",
            this.rows().get(ExportSteps.STAIRS.toString()),
            Matchers.allOf(
                Matchers.hasEntry("barrierType", "TB-1"),
                Matchers.hasEntry("buildingSymbol", "A-1")
            )
        );
    }

    /**
     * Plik CSV otwarty w QGIS daje warstwę punktów: kolumny x i y, a w każdym wierszu
     * liczbowe współrzędne.
     */
    @Then(
        "^(?:otworzy plik w QGIS ze wskazanym układem EPSG:2180|QGIS pokazuje warstwę punktów)$"
    )
    public void showsPointLayer() {
        MatcherAssert.assertThat(
            "Plik nie ma kolumn x i y, z których QGIS czyta współrzędne punktów",
            this.loaded().columns(), Matchers.hasItems("x", "y")
        );
        MatcherAssert.assertThat(
            "Plik nie ma wierszy, więc warstwa punktów byłaby pusta",
            this.rows().keySet(), Matchers.not(Matchers.empty())
        );
        for (final Map<String, String> row : this.rows().values()) {
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
        final Map<String, String> row = this.rows().get(ExportSteps.STAIRS.toString());
        MatcherAssert.assertThat(
            "Bariera TB-1 leży dalej niż metr od wejścia głównego A-1 w PUWG 1992",
            List.of(
                Double.parseDouble(row.get("x"))
                    - this.coordinate("ST_X(ST_Transform(location, 2180))"),
                Double.parseDouble(row.get("y"))
                    - this.coordinate("ST_Y(ST_Transform(location, 2180))")
            ),
            Matchers.everyItem(Matchers.closeTo(0.0, 1.0))
        );
    }

    /** Typ i opis zgłoszenia TB-1 mają polskie znaki bez zniekształceń. */
    @Then("typ „schody przy wejściu” ma polskie znaki bez zniekształceń")
    public void keepsPolishLetters() {
        final Map<String, String> row = this.rows().get(ExportSteps.STAIRS.toString());
        MatcherAssert.assertThat(
            "Typ i opis TB-1 w pliku mają zniekształcone polskie znaki",
            List.of(row.get("barrierType"), row.get("description")),
            Matchers.contains("TB-1", ExportSteps.STAIRS_TEXT)
        );
        MatcherAssert.assertThat(
            "Plik ma znak zastępczy w miejscu polskiego znaku",
            this.loaded().body(), Matchers.not(Matchers.containsString("�"))
        );
    }

    /**
     * Zgłoszenie TB-13 jest w pliku albo go w nim nie ma.
     *
     * @param presence Obecność: „jest” albo „nie występuje”
     */
    @Then("^zgłoszenie TB-13 (jest|nie występuje) w pliku$")
    public void thresholdPresence(final String presence) {
        MatcherAssert.assertThat(
            "Obecność zgłoszenia TB-13 w pliku nie zgadza się ze statusem zgłoszenia",
            this.rows().containsKey(ExportSteps.THRESHOLD.toString()),
            Matchers.equalTo("jest".equals(presence))
        );
    }

    /** Plik nie ma kolumn ani wartości z danymi autora. */
    @Then("w pliku nie ma imienia, adresu e-mail, identyfikatora konta ani pseudonimu autora")
    public void hasNoAuthorData() {
        MatcherAssert.assertThat(
            "Plik ma kolumnę z danymi autora",
            this.loaded().columns().stream().map(name -> name.toLowerCase(Locale.ROOT)).toList(),
            Matchers.everyItem(
                Matchers.not(Matchers.matchesPattern(".*(author|name|mail|account|pseudonym).*"))
            )
        );
        MatcherAssert.assertThat(
            "Plik ma konto autora albo adres e-mail",
            this.loaded().body(),
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
            this.rows().get(ExportSteps.STAIRS.toString()).get("buildingSymbol"),
            Matchers.equalTo("A-1")
        );
        MatcherAssert.assertThat(
            "Plik ma kontur budynku", this.loaded().body(),
            Matchers.not(Matchers.containsStringIgnoringCase("polygon"))
        );
    }

    /** Usuwa wszystko, co założył scenariusz. */
    @After
    public void cleanUp() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)")
            .param(
                "ids",
                List.of(
                    ExportSteps.STAIRS, ExportSteps.THRESHOLD, ExportSteps.REMOVAL,
                    ExportSteps.ELEVATOR
                )
            ).update();
        this.jdbc.sql("DELETE FROM entrances WHERE id = :id")
            .param("id", ExportSteps.ENTRANCE).update();
        for (final String symbol : this.buildings) {
            this.jdbc.sql("DELETE FROM buildings WHERE symbol = :symbol")
                .param("symbol", symbol).update();
        }
    }

    private ExportFile loaded() {
        final ExportFile result = this.file;
        if (result == null) {
            throw new IllegalStateException("Scenariusz nie pobrał eksportu");
        }
        return result;
    }

    private Map<String, Map<String, String>> rows() {
        return this.loaded().rows();
    }

    private double coordinate(final String expression) {
        return this.jdbc.sql(
            String.format("SELECT %s FROM entrances WHERE id = :id", expression)
        ).param("id", ExportSteps.ENTRANCE).query(Double.class).single();
    }

    private void building(
        final String symbol, final double longitude, final double latitude
    ) {
        final int present = this.jdbc.sql("SELECT count(*) FROM buildings WHERE symbol = :symbol")
            .param("symbol", symbol).query(Integer.class).single();
        if (present == 0) {
            new SampleBuilding(symbol, symbol, "BASIC").wkt(
                this.jdbc,
                String.format(
                    Locale.ROOT, "POLYGON((%1$f %2$f,%3$f %2$f,%3$f %4$f,%1$f %2$f))",
                    longitude, latitude, longitude + 0.0001, latitude + 0.0001
                )
            );
            this.buildings.add(symbol);
        }
    }

    private void add(
        final UUID id, final String status, final @Nullable UUID replaces,
        final SubmissionPlace place, final BarrierDetails barrier
    ) {
        this.submissions.create(
            new NewSubmission(
                id,
                new SubmissionContent(
                    SubmissionKind.QUICK, place, barrier, null, null, replaces,
                    List.of(UUID.randomUUID())
                ),
                ExportSteps.AUTHOR, Set.of()
            )
        );
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }
}
