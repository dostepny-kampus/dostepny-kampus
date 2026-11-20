/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adapter portu znaczników na tabeli inspection_markers; zapis to jedno atomowe zapytanie.
 *
 * <p>Wymagania: US15.1, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcMarkers implements MarkerRepository {

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
            "UPDATE inspection_markers SET revoked = TRUE WHERE id = :id AND author = :author"
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
        return this.jdbc.sql("UPDATE inspection_markers SET revoked = TRUE WHERE id = :id")
            .param("id", id)
            .update() > 0;
    }

    private static @Nullable String floor(final MarkerPlace place) {
        return switch (place) {
            case MarkerPlace.Floor on -> on.floor();
            case MarkerPlace.Building _ -> null;
            case null -> throw new IllegalStateException("Brak miejsca znacznika");
        };
    }
}
