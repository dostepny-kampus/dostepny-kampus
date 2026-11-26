/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.Rejection;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.json.SubmissionContentJson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczytuje zgłoszenia z JDBC. Listy pomijają nieczytelną treść pojedynczego wiersza.
 *
 * @since 0.1.0
 */
public final class SubmissionRows implements RowMapper<Submission> {

    /**
     * Kolumny zgłoszenia czytane do modelu.
     */
    public static final String COLUMNS =
        "id, version, status, created_at, verified_at, content::text AS content, rejection_reason, duplicate_of";

    /** Odczyt treści z kolumny JSON. */
    private final ObjectMapper mapper;

    /**
     * Tworzy odczyt wierszy zgłoszeń.
     *
     * @param mapper Odczyt treści z kolumny JSON
     */
    public SubmissionRows(final ObjectMapper mapper) {
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
                    .toContent(),
                Optional.ofNullable(rows.getObject("verified_at", OffsetDateTime.class))
                    .map(OffsetDateTime::toInstant).orElse(Instant.MIN),
                SubmissionRows.rejection(rows)
            );
        } catch (final JacksonException | InvalidSubmission ex) {
            throw new UnreadableSubmission(
                id, String.format("Zgłoszenia %s nie da się odczytać", id), ex
            );
        }
    }

    static ResultSetExtractor<SubmissionFinder.Page> page(
        final RowMapper<Submission> mapping, final SubmissionFinder.Window window
    ) {
        return rows -> {
            final List<Submission> items = new ArrayList<>(16);
            return new SubmissionFinder.Page(
                items,
                Objects.requireNonNullElse(
                    new ReadableRows<>(mapping, window.limit()).fill(rows, items),
                    window.after()
                ),
                rows.next()
            );
        };
    }

    private static @Nullable Rejection rejection(final ResultSet rows) throws SQLException {
        final String reason = rows.getString("rejection_reason");
        final Rejection result;
        if (reason == null) {
            result = null;
        } else {
            result = new Rejection(reason, rows.getObject("duplicate_of", UUID.class));
        }
        return result;
    }
}
