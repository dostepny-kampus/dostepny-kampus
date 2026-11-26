/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.api.SubmissionCounts;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Liczniki zgłoszeń według budynków i statusów z tabeli zgłoszeń.
 *
 * <p>Wymagania: US11.2, BR-21</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissionCounts implements SubmissionCounts {

    /** Dostęp do tabeli zgłoszeń. */
    private final JdbcClient jdbc;

    JdbcSubmissionCounts(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, Counts> byBuilding() {
        return this.jdbc.sql(
            """
            SELECT building_symbol,
                COUNT(*) FILTER (WHERE status IN ('SENT', 'IN_REVIEW')) AS pending,
                COUNT(*) FILTER (WHERE status = 'ACCEPTED') AS accepted
            FROM submissions
            WHERE building_symbol IS NOT NULL AND status IN ('SENT', 'IN_REVIEW', 'ACCEPTED')
            GROUP BY building_symbol
            """
        ).query(
            (rows, num) -> Map.entry(
                rows.getString("building_symbol"),
                new Counts(rows.getInt("pending"), rows.getInt("accepted"))
            )
        ).list().stream().collect(
            Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)
        );
    }
}
