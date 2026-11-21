/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.entrance.EntranceChangeArchive;
import io.github.dostepnykampus.barrier.submission.Submission;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Archiwizuje zastosowaną zmianę oznaczenia, zachowując historię decyzji.
 *
 * @since 0.1.0
 */
@Component
public final class JdbcEntranceChangeArchive implements EntranceChangeArchive {

    /** Dane zgłoszeń. */
    private final JdbcClient jdbc;

    /** Odczyt zapisanego zgłoszenia. */
    private final SubmissionRows rows;

    JdbcEntranceChangeArchive(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.rows = new SubmissionRows(mapper);
    }

    @Override
    public Submission archiveEntranceChange(final UUID id) {
        return this.jdbc.sql(
            String.format(
                """
                UPDATE submissions SET status = 'ARCHIVED'
                WHERE id = :id AND status = 'ACCEPTED' AND kind = 'ENTRANCE_MARKING'
                RETURNING %s
                """,
                JdbcSubmissions.COLUMNS
            )
        ).param("id", id).query(this.rows).single();
    }
}
