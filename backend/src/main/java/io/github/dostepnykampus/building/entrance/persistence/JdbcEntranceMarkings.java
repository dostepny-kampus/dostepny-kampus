/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.entrance.persistence;

import io.github.dostepnykampus.building.api.EntranceMarkings;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Zapis wyniku obchodu i chwili przyjęcia dla danego wejścia budynku.
 *
 * @since 0.1.0
 */
@Component
public final class JdbcEntranceMarkings implements EntranceMarkings {

    /** Dane wejść budynków. */
    private final JdbcClient jdbc;

    JdbcEntranceMarkings(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean apply(final Change change) {
        return this.jdbc.sql(
            """
            UPDATE entrances
            SET marking = :marking, marking_source = 'WALKTHROUGH',
                marking_date = CAST(:accepted AT TIME ZONE 'Europe/Warsaw' AS date),
                walkthrough_at = :accepted
            WHERE building_symbol = :building AND id = :entrance
                AND (walkthrough_at IS NULL OR walkthrough_at <= :submitted)
            """
        ).param("marking", change.marking().name())
            .param("accepted", change.accepted().atOffset(ZoneOffset.UTC))
            .param("building", change.building()).param("entrance", change.entrance())
            .param("submitted", change.submitted().atOffset(ZoneOffset.UTC)).update() == 1;
    }
}
