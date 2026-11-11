/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczytuje zgłoszenia z JDBC. Listy pomijają nieczytelną treść pojedynczego wiersza.
 *
 * @since 0.1.0
 */
final class SubmissionRows implements RowMapper<Submission> {

    /** Diagnostyka wierszy pomijanych w listach. */
    private static final Logger LOG = LoggerFactory.getLogger(SubmissionRows.class);

    /** Odczyt treści z kolumny JSON. */
    private final ObjectMapper mapper;

    SubmissionRows(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Submission mapRow(final ResultSet rows, final int number) throws SQLException {
        final UUID id = rows.getObject("id", UUID.class);
        try {
            return new Submission(
                id,
                rows.getInt("version"),
                SubmissionStatus.valueOf(rows.getString("status")),
                rows.getObject("created_at", OffsetDateTime.class).toInstant(),
                Optional.ofNullable(
                    this.mapper.readValue(rows.getString("content"), SubmissionContentJson.class)
                ).orElseThrow(() -> new InvalidSubmission("Brak zapisanej treści zgłoszenia"))
                    .toContent()
            );
        } catch (final JacksonException | InvalidSubmission ex) {
            throw new UnreadableSubmission(
                id, String.format("Zgłoszenia %s nie da się odczytać", id), ex
            );
        }
    }

    static <T> ResultSetExtractor<List<T>> readable(final RowMapper<T> mapping) {
        return rows -> {
            final List<T> results = new ArrayList<>(16);
            int number = 0;
            while (rows.next()) {
                try {
                    results.add(mapping.mapRow(rows, number));
                } catch (final UnreadableSubmission ex) {
                    SubmissionRows.LOG.error("Pominięto nieczytelne zgłoszenie {}", ex.id());
                }
                ++number;
            }
            return results;
        };
    }
}
