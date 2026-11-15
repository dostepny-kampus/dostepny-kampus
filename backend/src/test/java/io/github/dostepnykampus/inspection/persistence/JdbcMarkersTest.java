/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zapis znaczników piętra: znacznik zapisuje się raz, także po powtórzeniu, a w budynku
 * podstawowym bez piętra.
 *
 * <p>Wymagania: US15.1, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class JdbcMarkersTest {

    @Test
    void brNineSavesRepeatedMarkerOnce(final WebApplicationContext context) {
        final UUID id = UUID.randomUUID();
        final MarkerRepository markers = context.getBean(MarkerRepository.class);
        markers.place(new NewMarker(id, new MarkerPlace.Floor("C-13", "0"), "konto-a"));
        markers.place(new NewMarker(id, new MarkerPlace.Floor("C-13", "0"), "konto-a"));
        Assertions.assertEquals(
            1,
            context.getBean(JdbcClient.class)
                .sql("SELECT count(*) FROM inspection_markers WHERE id = :id").param("id", id)
                .query(Integer.class).single(),
            "Powtórzony znacznik zapisał się drugi raz"
        );
    }

    @Test
    void usFifteenOneSavesMarkerWithoutFloor(final WebApplicationContext context) {
        final UUID id = UUID.randomUUID();
        final MarkerPlace place = new MarkerPlace.Building("C-13");
        context.getBean(MarkerRepository.class).place(new NewMarker(id, place, "konto-a"));
        Assertions.assertEquals(
            "C-13, bez piętra, konto-a, false",
            context.getBean(JdbcClient.class).sql(
                String.join(
                    " ",
                    "SELECT concat_ws(', ', building_symbol, COALESCE(floor_id, 'bez piętra'),",
                    "author, revoked::text) FROM inspection_markers WHERE id = :id"
                )
            ).param("id", id).query(String.class).single(),
            "Znacznik budynku podstawowego zapisał się z innymi danymi"
        );
    }
}
