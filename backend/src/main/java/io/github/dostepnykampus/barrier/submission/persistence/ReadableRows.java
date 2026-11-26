/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

/**
 * Lista wartości z wierszy zgłoszeń; wiersz z nieczytelną treścią zgłoszenia jest pomijany
 * i zapisywany w dzienniku, a reszta listy zostaje.
 *
 * @param <T> Typ wartości z wiersza
 * @since 0.1.0
 */
public final class ReadableRows<T> implements ResultSetExtractor<List<T>> {

    /** Diagnostyka wierszy pomijanych w listach. */
    private static final Logger LOG = LoggerFactory.getLogger(ReadableRows.class);

    /** Budowa wartości z wiersza. */
    private final RowMapper<T> mapping;

    /** Najwięcej wierszy czytanych do listy. */
    private final int limit;

    /**
     * Tworzy odczyt listy bez limitu wierszy.
     *
     * @param mapping Budowa wartości z wiersza; nieczytelne zgłoszenie zgłasza wyjątkiem
     *  UnreadableSubmission
     */
    public ReadableRows(final RowMapper<T> mapping) {
        this(mapping, Integer.MAX_VALUE);
    }

    /**
     * Tworzy odczyt listy z limitem wierszy.
     *
     * @param mapping Budowa wartości z wiersza; nieczytelne zgłoszenie zgłasza wyjątkiem
     *  UnreadableSubmission
     * @param limit Najwięcej wierszy czytanych do listy, także pominiętych
     */
    ReadableRows(final RowMapper<T> mapping, final int limit) {
        this.mapping = mapping;
        this.limit = limit;
    }

    @Override
    public List<T> extractData(final ResultSet rows) throws SQLException {
        final List<T> results = new ArrayList<>(16);
        this.fill(rows, results);
        return results;
    }

    /**
     * Czyta do listy najwyżej {@code limit} wierszy, pomijając nieczytelne.
     *
     * @param rows Wiersze zapytania
     * @param into Lista, do której trafiają wartości
     * @return Identyfikator ostatniego przeczytanego wiersza albo {@code null} bez wierszy
     * @throws SQLException Gdy odczyt wiersza się nie uda
     */
    @Nullable UUID fill(final ResultSet rows, final List<T> into) throws SQLException {
        @Nullable UUID last = null;
        int number = 0;
        while (number < this.limit && rows.next()) {
            last = rows.getObject("id", UUID.class);
            try {
                into.add(this.mapping.mapRow(rows, number));
            } catch (final UnreadableSubmission ex) {
                ReadableRows.LOG.error("Pominięto nieczytelne zgłoszenie {}", ex.id());
            }
            ++number;
        }
        return last;
    }
}
