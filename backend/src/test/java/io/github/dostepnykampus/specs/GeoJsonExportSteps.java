/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.barrier.submission.persistence.JdbcSubmissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kroki scenariuszy eksportu GeoJSON: plik w QGIS bez ustawiania układu, czyli bez pola
 * {@code crs} i w WGS84 według RFC 7946, oraz bariera usunięta poza plikiem. Plik pobierają
 * kroki {@link ExportSteps}.
 *
 * <p>Wymagania: US13.1, US13.3, BR-19, BR-22</p>
 *
 * @since 0.1.0
 */
public final class GeoJsonExportSteps {

    /** Zgłoszenie TB-9 niesprawnej windy w budynku D-2, zastąpione zmianą. */
    private static final UUID ELEVATOR =
        UUID.fromString("7a3b4c5d-6e7f-4a81-8c9d-1e2f3a4b5c6d");

    /** Przyjęta zmiana „bariera usunięta” dla windy TB-9 w budynku D-2. */
    private static final UUID REMOVAL =
        UUID.fromString("8b4c5d6e-7f80-4b92-9dae-2f3a4b5c6d7e");

    /** Kroki z pobranym plikiem. */
    private final ExportSteps export;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /** Zapis zgłoszeń jak na serwerze. */
    private final JdbcSubmissions submissions;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param export Kroki z pobranym plikiem
     * @param jdbc Baza aplikacji
     * @param submissions Zapis zgłoszeń jak na serwerze
     */
    public GeoJsonExportSteps(
        final ExportSteps export, final JdbcClient jdbc, final JdbcSubmissions submissions
    ) {
        this.export = export;
        this.jdbc = jdbc;
        this.submissions = submissions;
    }

    /**
     * W budynku D-2 przyjęto zmianę „bariera usunięta” dla niesprawnej windy TB-9, która
     * zastąpiła wcześniejsze zgłoszenie windy.
     */
    @Given("przyjęto zgłoszenie „bariera usunięta” dla niesprawnej windy TB-9 w budynku D-2")
    public void removedElevator() {
        this.add(GeoJsonExportSteps.ELEVATOR, "ARCHIVED", null, 3, false);
        this.add(GeoJsonExportSteps.REMOVAL, "ACCEPTED", GeoJsonExportSteps.ELEVATOR, null, true);
    }

    /** Plik GeoJSON otwarty w QGIS daje warstwę punktów ze zgłoszeniami. */
    @When("otworzy plik w QGIS")
    public void opensFile() {
        MatcherAssert.assertThat(
            "Plik nie ma punktów, więc warstwa w QGIS byłaby pusta",
            this.export.loaded().rows().keySet(), Matchers.not(Matchers.empty())
        );
    }

    /** Plik nie podaje własnego układu, więc QGIS bierze WGS84 z RFC 7946 bez pytania. */
    @Then("QGIS nie pyta o układ współrzędnych")
    public void needsNoCrs() {
        MatcherAssert.assertThat(
            "Plik ma pole crs, a GeoJSON według RFC 7946 ma być w WGS84 bez niego",
            this.export.loaded().ownCrs(), Matchers.is(false)
        );
    }

    /** Każdy punkt ma długość i szerokość geograficzną w zakresie WGS84. */
    @Then("plik ma współrzędne w WGS84")
    public void usesDegrees() {
        for (final Map<String, String> row : this.export.loaded().rows().values()) {
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
    public void liesOnMap() {
        final Map<String, String> row =
            this.export.loaded().rows().get(ExportSteps.STAIRS.toString());
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
    public void hasNoElevator() {
        MatcherAssert.assertThat(
            "Plik ma zgłoszenie windy z budynku D-2 albo zmianę „bariera usunięta”",
            this.export.loaded().rows().keySet(),
            Matchers.not(
                Matchers.anyOf(
                    Matchers.hasItem(GeoJsonExportSteps.ELEVATOR.toString()),
                    Matchers.hasItem(GeoJsonExportSteps.REMOVAL.toString())
                )
            )
        );
    }

    /** W pliku jest bariera TB-1 z budynku A-1. */
    @Then("w pliku jest bariera TB-1 przy wejściu głównym A-1")
    public void hasStairs() {
        MatcherAssert.assertThat(
            "Pliku nie ma bariery TB-1 przy wejściu głównym A-1",
            this.export.loaded().rows().get(ExportSteps.STAIRS.toString()),
            Matchers.allOf(
                Matchers.hasEntry("barrierType", "TB-1"),
                Matchers.hasEntry("buildingSymbol", "A-1")
            )
        );
    }

    /** Usuwa zgłoszenia windy założone przez scenariusz. */
    @After
    public void cleanUp() {
        this.jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)").param(
            "ids", List.of(GeoJsonExportSteps.REMOVAL, GeoJsonExportSteps.ELEVATOR)
        ).update();
    }

    private void add(
        final UUID id, final String status, final @Nullable UUID replaces,
        final @Nullable Integer severity, final boolean removed
    ) {
        this.submissions.create(
            new NewSubmission(
                id,
                new SubmissionContent(
                    SubmissionKind.PRECISE,
                    new SubmissionPlace(
                        new SubmissionPoint(17.053, 51.103), null, false, null, null
                    ),
                    new BarrierDetails(
                        BarrierGroup.VERTICAL, new BarrierType(9), severity, null, removed
                    ),
                    null, null, replaces, List.of(UUID.randomUUID())
                ),
                "sub-autora-windy", Set.of()
            )
        );
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
    }
}
