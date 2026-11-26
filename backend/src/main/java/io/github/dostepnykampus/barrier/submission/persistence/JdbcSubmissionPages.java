/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Strony listy zgłoszeń z PostgreSQL: zapytanie po kursorze identyfikatora z limitem,
 * o jeden wiersz dłuższe, żeby wiedzieć, czy jest dalsza strona.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissionPages implements SubmissionFinder.Pages {

    /** Dostęp do bazy. */
    private final JdbcClient jdbc;

    /** Odczyt wiersza zgłoszenia. */
    private final SubmissionRows rows;

    /**
     * Ctor.
     *
     * @param jdbc Dostęp do bazy
     * @param mapper Odczyt treści zgłoszenia z JSON
     */
    public JdbcSubmissionPages(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.rows = new SubmissionRows(mapper);
    }

    @Override
    public SubmissionFinder.Page page(
        final SubmissionQuery query, final SubmissionFinder.Window window
    ) {
        return this.slice(query, null, window);
    }

    @Override
    public SubmissionFinder.Page own(
        final String author, final SubmissionQuery query, final SubmissionFinder.Window window
    ) {
        return this.slice(query, author, window);
    }

    private SubmissionFinder.Page slice(
        final SubmissionQuery query, final @Nullable String author,
        final SubmissionFinder.Window window
    ) {
        return Objects.requireNonNull(
            SubmissionSelection.bind(
                this.jdbc.sql(
                    String.format(
                        """
                        SELECT %s FROM submissions WHERE %s
                            AND id > :after
                        ORDER BY id LIMIT :fetch
                        """,
                        SubmissionRows.COLUMNS, SubmissionSelection.WHERE
                    )
                ),
                query, author
            ).param("after", window.after())
                .param("fetch", window.limit() + 1)
                .query(SubmissionRows.page(this.rows, window))
        );
    }
}
