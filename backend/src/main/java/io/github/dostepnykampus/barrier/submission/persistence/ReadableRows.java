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

    /** Budowa wartości z wiersza. */
    private final RowMapper<T> mapping;

    /**
     * Tworzy odczyt listy.
     *
     * @param mapping Budowa wartości z wiersza; nieczytelne zgłoszenie zgłasza wyjątkiem
     *  UnreadableSubmission
     */
    public ReadableRows(final RowMapper<T> mapping) {
        this.mapping = mapping;
    }

    @Override
    public List<T> extractData(final ResultSet rows) throws SQLException {
        final List<T> results = new ArrayList<>(16);
        SubmissionRows.fill(rows, this.mapping, Integer.MAX_VALUE, results);
        return results;
    }
}
