/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.SubmissionRetention;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Usuwanie zgłoszeń odrzuconych w PostgreSQL według kolumny rejected_at.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcRetention implements SubmissionRetention {

    /**
     * Dostęp do tabeli submissions.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli submissions
     */
    public JdbcRetention(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> purgeRejected(final int days) {
        return this.jdbc.sql(
            """
            DELETE FROM submissions
            WHERE status = 'REJECTED' AND rejected_at < now() - make_interval(days => :days)
            RETURNING id
            """
        ).param("days", days).query(UUID.class).list().stream()
            .map(Objects::requireNonNull).toList();
    }
}
