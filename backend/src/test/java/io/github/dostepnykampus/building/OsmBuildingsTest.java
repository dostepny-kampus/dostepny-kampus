/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Po starcie serwera tabela buildings ma budynki z pliku
 * konturów OpenStreetMap (ADR-0008), a punkty końcowe je zwracają.
 *
 * <p>Wymagania: US1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class OsmBuildingsTest {

    @Test
    void usOneMergesOutlinesOfOneSymbol(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Dwa kontury H-14 nie tworzą jednego wielokąta złożonego",
            context.getBean(JdbcClient.class).sql(
                "SELECT ST_NumGeometries(outline) FROM buildings WHERE symbol = 'H-14'"
            ).query(Integer.class).single(),
            Matchers.greaterThan(1)
        );
    }

    @Test
    void usOneSharesOutlineBetweenSymbols(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "C-3 i C-4 nie mają wspólnego konturu",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT ST_Equals(c3.outline, c4.outline) FROM buildings c3, buildings c4
                WHERE c3.symbol = 'C-3' AND c4.symbol = 'C-4'
                """
            ).query(Boolean.class).single(),
            Matchers.is(true)
        );
    }

    @Test
    void usOneLeavesBuildingWithoutNameWhenDataHasNone(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Budynek bez nazwy w danych dostał nazwę",
            OsmBuildingsTest.names(context, "L-1"),
            Matchers.contains(Matchers.nullValue())
        );
    }

    @Test
    void usOneDropsNameThatIsOwnSymbol(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Nazwa równa własnemu symbolowi C-13 została zapisana",
            OsmBuildingsTest.names(context, "C-13"),
            Matchers.contains(Matchers.nullValue())
        );
    }

    @Test
    void usOneDropsNameThatIsOtherSymbol(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Nazwa T-22 cudzego symbolu została zapisana dla T-18 i T-22",
            OsmBuildingsTest.names(context, "T-18", "T-22"),
            Matchers.contains(Matchers.nullValue(), Matchers.nullValue())
        );
    }

    @Test
    void usOneDropsNameThatListsSymbols(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Nazwa C-3/C-4 złożona z samych symboli została zapisana",
            OsmBuildingsTest.names(context, "C-3", "C-4"),
            Matchers.contains(Matchers.nullValue(), Matchers.nullValue())
        );
    }

    @Test
    void usTwoSavesNameWithoutOwnSymbol(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Nazwa budynku na serwerze powtarza symbol albo różni się od nazwy w aplikacji",
            OsmBuildingsTest.names(context, "A-1", "M-6", "T-6"),
            Matchers.contains("Budynek główny", "M-6 bis", "Dom Studencki \"Alcatraz\"")
        );
    }

    @Test
    void usSixTwoLocatesRealBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Punkt w konturze A-1 z pliku nie wskazuje A-1",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get("/buildings/locate").param("lat", "51.10785")
                    .param("lon", "17.06209")
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString("\"buildingSymbol\":\"A-1\"")
        );
    }

    @Test
    void usOneRunsAgainWhenFileChanges(
        final WebApplicationContext context, @TempDir final Path directory
    ) throws IOException {
        final Path file = directory.resolve("buildings.geojson");
        OsmBuildingsTest.writeBuilding(file, "Pierwsza nazwa");
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        try {
            final Flyway flyway = OsmBuildingsTest.flyway(context, file);
            flyway.migrate();
            final String original = OsmBuildingsTest.importedName(jdbc);
            final int repeated = flyway.migrate().migrationsExecuted;
            OsmBuildingsTest.writeBuilding(file, "Druga nazwa");
            flyway.migrate();
            Assertions.assertAll(
                () -> Assertions.assertEquals(
                    "Pierwsza nazwa", original, "Pierwszy import nie wczytał nazwy z pliku"
                ),
                () -> Assertions.assertEquals(
                    0, repeated, "Flyway ponowił import pliku o tej samej treści"
                ),
                () -> Assertions.assertEquals(
                    "Druga nazwa", OsmBuildingsTest.importedName(jdbc),
                    "Flyway nie wczytał nowej nazwy po zmianie treści pliku"
                )
            );
        } finally {
            jdbc.sql("DROP SCHEMA IF EXISTS osm_reimport CASCADE").update();
        }
    }

    @Test
    void usSeventeenRemovesStaleImportedEntrances(
        final WebApplicationContext context, @TempDir final Path directory
    ) throws IOException {
        final Path file = directory.resolve("buildings.geojson");
        OsmBuildingsTest.writeBuilding(file, "Pierwsza nazwa");
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        try {
            final Flyway flyway = OsmBuildingsTest.flyway(context, file);
            flyway.migrate();
            jdbc.sql(
                """
                INSERT INTO osm_reimport.entrances
                    (id, building_symbol, main, location, marking, marking_date, marking_source)
                SELECT id, symbol, false, ST_SetSRID(ST_MakePoint(17, 51), 4326),
                    marking, DATE '2026-11-01', source
                FROM (VALUES
                    ('node/1', 'T-98', 'NO_DATA', 'IMPORT'),
                    ('node/2', 'T-99', 'NO_DATA', 'IMPORT'),
                    ('node/3', 'T-99', 'ACCESSIBLE', 'WALKTHROUGH')
                ) AS stale(id, symbol, marking, source)
                """
            ).update();
            OsmBuildingsTest.writeBuilding(file, "Druga nazwa");
            flyway.migrate();
            MatcherAssert.assertThat(
                "Import nie uzgodnił par wejście–budynek albo zmienił oznaczenie z obchodu",
                jdbc.sql(
                    """
                    SELECT concat_ws(' ', id, building_symbol, marking, marking_source)
                    FROM osm_reimport.entrances ORDER BY id, building_symbol
                    """
                ).query(String.class).list(),
                Matchers.contains(
                    "node/1 T-99 NO_DATA IMPORT", "node/3 T-99 ACCESSIBLE WALKTHROUGH"
                )
            );
        } finally {
            jdbc.sql("DROP SCHEMA IF EXISTS osm_reimport CASCADE").update();
        }
    }

    @Test
    void usOneChangesChecksumWhenImportFormatChanges() {
        final byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            "Zmiana wersji importu nie zmienia sumy kontrolnej",
            OsmBuildings.checksum(content, OsmBuildings.FORMAT),
            Matchers.not(OsmBuildings.checksum(content, OsmBuildings.FORMAT + 1))
        );
    }

    private static Flyway flyway(final WebApplicationContext context, final Path file) {
        return Flyway.configure().dataSource(context.getBean(DataSource.class))
            .schemas("osm_reimport").javaMigrations(
                new OsmBuildings(context.getBean(ObjectMapper.class), new FileSystemResource(file))
            ).load();
    }

    private static String importedName(final JdbcClient jdbc) {
        return jdbc.sql("SELECT name FROM osm_reimport.buildings WHERE symbol = 'T-99'")
            .query(String.class).single();
    }

    private static void writeBuilding(final Path file, final String name) throws IOException {
        Files.writeString(
            file,
            """
            {
                "type": "FeatureCollection",
                "attribution": "Dane testowe",
                "features": [{
                    "type": "Feature",
                    "properties": {
                        "kind": "building", "symbols": ["T-98", "T-99"],
                        "name": "T-99 %s", "display_name": "%s"
                    },
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [[[17,51],[17.001,51],[17.001,51.001],[17,51]]]
                    }
                }, {
                    "type": "Feature",
                    "properties": {"kind": "entrance", "symbols": ["T-99"], "osm_id": "node/1"},
                    "geometry": {"type": "Point", "coordinates": [17,51]}
                }]
            }
            """.formatted(name, name)
        );
    }

    private static List<String> names(
        final WebApplicationContext context, final String... symbols
    ) {
        return context.getBean(JdbcClient.class).sql(
            "SELECT name FROM buildings WHERE symbol IN (:symbols) ORDER BY symbol"
        ).param("symbols", List.of(symbols)).query(
            (rs, row) -> rs.getString(1)
        ).list();
    }
}
