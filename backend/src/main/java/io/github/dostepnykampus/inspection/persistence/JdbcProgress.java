/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.inspection.domain.BuildingProgress;
import io.github.dostepnykampus.inspection.domain.ProgressMarker;
import io.github.dostepnykampus.inspection.domain.ProgressRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Odczyt postępu inwentaryzacji wprost z tabel budynków, pięter, wejść, zgłoszeń i znaczników.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-5, BR-20, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcProgress implements ProgressRepository {

    /**
     * Dostęp do bazy PostgreSQL.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter odczytu postępu.
     *
     * @param jdbc Dostęp do bazy
     */
    public JdbcProgress(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<BuildingProgress> read(final String viewer) {
        final Map<String, List<ProgressMarker>> grouped = this.activeMarkers(viewer);
        return this.jdbc.sql(
            """
            SELECT b.symbol, b.detail_level,
                (SELECT COUNT(*) FROM floors f WHERE f.building_symbol = b.symbol) AS floors_total,
                (SELECT COUNT(DISTINCT m.floor_id) FROM inspection_markers m
                    JOIN floors f ON f.building_symbol = m.building_symbol AND f.id = m.floor_id
                    WHERE m.building_symbol = b.symbol AND NOT m.revoked) AS floors_inspected,
                EXISTS (SELECT 1 FROM inspection_markers m
                    WHERE m.building_symbol = b.symbol AND NOT m.revoked) AS inspected,
                (SELECT COUNT(*) FROM submissions s
                    WHERE s.building_symbol = b.symbol
                    AND s.status IN ('SENT', 'IN_REVIEW')) AS pending,
                (SELECT COUNT(*) FROM submissions s
                    WHERE s.building_symbol = b.symbol AND s.status = 'ACCEPTED') AS accepted,
                (SELECT COUNT(*) FROM entrances e
                    WHERE e.building_symbol = b.symbol) AS entrances_total,
                (SELECT COUNT(*) FROM entrances e
                    WHERE e.building_symbol = b.symbol
                    AND e.marking_source = 'WALKTHROUGH') AS checked
            FROM buildings b
            ORDER BY b.symbol
            """
        ).query((rows, num) -> JdbcProgress.building(rows, grouped)).list();
    }

    private Map<String, List<ProgressMarker>> activeMarkers(final String viewer) {
        return this.jdbc.sql(
            """
            SELECT id, building_symbol, floor_id, author, created_at
            FROM inspection_markers
            WHERE NOT revoked
            ORDER BY building_symbol, created_at, id
            """
        ).query((rows, num) -> JdbcProgress.marker(rows, viewer)).list().stream()
            .collect(Collectors.groupingBy(ProgressMarker::building));
    }

    private static ProgressMarker marker(final ResultSet rows, final String viewer)
        throws SQLException {
        final String author = JdbcProgress.text(rows, "author");
        final OffsetDateTime created = rows.getObject("created_at", OffsetDateTime.class);
        if (created == null) {
            throw new SQLException("Brak wartości kolumny created_at");
        }
        return new ProgressMarker(
            UUID.fromString(JdbcProgress.text(rows, "id")),
            JdbcProgress.text(rows, "building_symbol"),
            rows.getString("floor_id"),
            created.toInstant(),
            author.equals(viewer),
            JdbcProgress.pseudonym(author)
        );
    }

    private static BuildingProgress building(
        final ResultSet rows, final Map<String, List<ProgressMarker>> grouped
    ) throws SQLException {
        final String symbol = JdbcProgress.text(rows, "symbol");
        final BuildingProgress.Coverage coverage;
        if ("DETAILED".equals(JdbcProgress.text(rows, "detail_level"))) {
            coverage = new BuildingProgress.Coverage.Floors(
                rows.getInt("floors_total"), rows.getInt("floors_inspected")
            );
        } else {
            coverage = new BuildingProgress.Coverage.Whole(rows.getBoolean("inspected"));
        }
        return new BuildingProgress(
            symbol,
            coverage,
            new BuildingProgress.Counts(
                rows.getInt("pending"),
                rows.getInt("accepted"),
                rows.getInt("entrances_total"),
                rows.getInt("checked")
            ),
            grouped.getOrDefault(symbol, List.of())
        );
    }

    private static String pseudonym(final String author) {
        return String.format("Autor %d", Math.floorMod(author.hashCode(), 999) + 1);
    }

    private static String text(final ResultSet rows, final String column) throws SQLException {
        final String value = rows.getString(column);
        if (value == null) {
            throw new SQLException(String.format("Brak wartości kolumny %s", column));
        }
        return value;
    }
}
