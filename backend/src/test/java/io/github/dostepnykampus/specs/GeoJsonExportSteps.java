/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kroki scenariusza eksportu GeoJSON w QGIS: plik bez ustawiania układu, czyli bez pola
 * {@code crs} i w WGS84 według RFC 7946. Plik pobierają kroki {@link ExportSteps}.
 *
 * <p>Wymagania: US13.1</p>
 *
 * @since 0.1.0
 */
public final class GeoJsonExportSteps {

    /** Kroki z pobranym plikiem. */
    private final ExportSteps export;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param export Kroki z pobranym plikiem
     * @param jdbc Baza aplikacji
     */
    public GeoJsonExportSteps(final ExportSteps export, final JdbcClient jdbc) {
        this.export = export;
        this.jdbc = jdbc;
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
}
