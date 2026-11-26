/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import io.github.dostepnykampus.inspection.domain.SyncedMarker;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adapter portu znaczników na tabeli inspection_markers; zapis to jedno atomowe zapytanie.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcMarkers implements MarkerRepository {

    /** Cofnięcie znacznika; chwila zmiany zostaje z pierwszego cofnięcia. */
    private static final String REVOKE = String.join(
        " ", "UPDATE inspection_markers SET revoked = TRUE,",
        "changed_at = CASE WHEN revoked THEN changed_at ELSE now() END WHERE id = :id"
    );

    /** Dostęp do tabeli inspection_markers. */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli inspection_markers
     */
    public JdbcMarkers(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void place(final NewMarker marker) {
        this.jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author)
            VALUES (:id, :building, :floor, :author)
            ON CONFLICT (id) DO NOTHING
            """
        ).param("id", marker.id()).param("building", marker.place().building())
            .param("floor", JdbcMarkers.floor(marker.place())).param("author", marker.author())
            .update();
    }

    @Override
    public boolean revoke(final UUID id, final String author) {
        return this.jdbc.sql(
            JdbcMarkers.REVOKE.concat(" AND author = :author")
        ).param("id", id).param("author", author).update() > 0;
    }

    @Override
    public Optional<String> authorOf(final UUID id) {
        return this.jdbc.sql("SELECT author FROM inspection_markers WHERE id = :id")
            .param("id", id)
            .query(String.class)
            .optional();
    }

    @Override
    public boolean revokeAny(final UUID id) {
        return this.jdbc.sql(JdbcMarkers.REVOKE)
            .param("id", id)
            .update() > 0;
    }

    @Override
    public List<SyncedMarker> changedSince(final Instant since, final String reader) {
        return this.jdbc.sql(
            """
            SELECT id, building_symbol, floor_id, author, revoked, created_at, changed_at
            FROM inspection_markers WHERE changed_at >= :since ORDER BY changed_at, id
            """
        ).param("since", OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
            .query((rows, number) -> JdbcMarkers.synced(rows, reader)).list();
    }

    private static SyncedMarker synced(final ResultSet rows, final String reader)
        throws SQLException {
        return new SyncedMarker(
            MarkerRows.id(rows), MarkerRows.place(rows), MarkerRows.instant(rows, "created_at"),
            MarkerRows.instant(rows, "changed_at"), rows.getBoolean("revoked"),
            reader.equals(MarkerRows.text(rows, "author"))
        );
    }

    private static @Nullable String floor(final MarkerPlace place) {
        return switch (place) {
            case MarkerPlace.Floor on -> on.floor();
            case MarkerPlace.Building _ -> null;
            case null -> throw new IllegalStateException("Brak miejsca znacznika");
        };
    }
}
