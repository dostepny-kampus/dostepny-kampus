/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Odczyt kolumn wiersza tabeli inspection_markers; brak wymaganej wartości to błąd SQL.
 *
 * <p>Wymagania: US15.1, BR-20</p>
 *
 * @since 0.1.0
 */
final class MarkerRows {

    private MarkerRows() {
    }

    /**
     * Identyfikator znacznika z kolumny id.
     *
     * @param rows Wiersz wyniku
     * @return Identyfikator znacznika
     * @throws SQLException Gdy kolumny brak albo jest pusta
     */
    static UUID id(final ResultSet rows) throws SQLException {
        return UUID.fromString(MarkerRows.text(rows, "id"));
    }

    /**
     * Miejsce znacznika z kolumn building_symbol i floor_id.
     *
     * @param rows Wiersz wyniku
     * @return Cały budynek, gdy piętra brak, inaczej piętro budynku
     * @throws SQLException Gdy symbolu budynku brak
     */
    static MarkerPlace place(final ResultSet rows) throws SQLException {
        final String building = MarkerRows.text(rows, "building_symbol");
        final String floor = rows.getString("floor_id");
        final MarkerPlace place;
        if (floor == null) {
            place = new MarkerPlace.Building(building);
        } else {
            place = new MarkerPlace.Floor(building, floor);
        }
        return place;
    }

    /**
     * Chwila z kolumny typu TIMESTAMPTZ.
     *
     * @param rows Wiersz wyniku
     * @param column Nazwa kolumny
     * @return Chwila
     * @throws SQLException Gdy kolumny brak albo jest pusta
     */
    static Instant instant(final ResultSet rows, final String column) throws SQLException {
        final OffsetDateTime value = rows.getObject(column, OffsetDateTime.class);
        if (value == null) {
            throw new SQLException(String.format("Brak wartości kolumny %s", column));
        }
        return value.toInstant();
    }

    /**
     * Tekst z kolumny.
     *
     * @param rows Wiersz wyniku
     * @param column Nazwa kolumny
     * @return Wartość kolumny
     * @throws SQLException Gdy kolumny brak albo jest pusta
     */
    static String text(final ResultSet rows, final String column) throws SQLException {
        final String value = rows.getString(column);
        if (value == null) {
            throw new SQLException(String.format("Brak wartości kolumny %s", column));
        }
        return value;
    }
}
