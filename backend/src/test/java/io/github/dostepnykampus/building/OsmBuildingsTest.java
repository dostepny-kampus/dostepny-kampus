/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy US1: po starcie serwera tabela buildings ma budynki z pliku
 * konturów OpenStreetMap (ADR-0008), a punkty końcowe je zwracają.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
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
    void usOneNamesBuildingWithoutName(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Budynek bez nazwy w danych nie dostał symbolu za nazwę",
            context.getBean(JdbcClient.class).sql(
                "SELECT name FROM buildings WHERE symbol = 'L-1'"
            ).query(String.class).single(),
            Matchers.equalTo("L-1")
        );
    }

    @Test
    void usSixTwoLocatesRealBuilding(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Punkt w konturze A-1 z pliku nie wskazuje A-1",
            MockMvcBuilders.webAppContextSetup(context).build().perform(
                MockMvcRequestBuilders.get("/buildings/locate").param("lat", "51.10785")
                    .param("lon", "17.06209")
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString("\"buildingSymbol\":\"A-1\"")
        );
    }

    @Test
    void usOneRunsAgainWhenFileChanges(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Historia Flyway nie ma sumy kontrolnej pliku konturów",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT checksum FROM flyway_schema_history
                WHERE description = 'budynki z OpenStreetMap' AND success
                """
            ).query(Integer.class).single(),
            Matchers.equalTo(context.getBean(OsmBuildings.class).getChecksum())
        );
    }
}
