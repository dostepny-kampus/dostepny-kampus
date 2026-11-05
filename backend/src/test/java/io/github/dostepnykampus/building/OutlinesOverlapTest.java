/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testy BR-14 dla nachodzących konturów: gdy punkt leży w kilku konturach,
 * budynkiem zgłoszenia jest ten o najmniejszym symbolu.
 *
 * <p>Kontury przy równiku nie zależą od danych kampusu. Budynek o większym
 * symbolu jest wstawiony jako pierwszy, więc wynik nie zależy od kolejności
 * wierszy w tabeli.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class OutlinesOverlapTest {

    @Test
    void brFourteenPicksSmallestSymbolOfOverlappingOutlines(
        final ApplicationContext context
    ) {
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES ('G-902', 'Budynek drugi', 'BASIC',
                ST_Multi(ST_GeomFromText(
                    'POLYGON((0.001 0,0.003 0,0.003 0.001,0.001 0.001,0.001 0))',
                    4326)),
                '© OpenStreetMap contributors'),
                ('G-901', 'Budynek pierwszy', 'BASIC',
                ST_Multi(ST_GeomFromText(
                    'POLYGON((0 0,0.002 0,0.002 0.001,0 0.001,0 0))', 4326)),
                '© OpenStreetMap contributors')
            """
        ).update();
        MatcherAssert.assertThat(
            "Punkt w dwóch konturach nie należy do budynku o najmniejszym symbolu",
            context.getBean(Outlines.class).containing(0.0005, 0.0015),
            Matchers.equalTo(Optional.of("G-901"))
        );
    }
}
