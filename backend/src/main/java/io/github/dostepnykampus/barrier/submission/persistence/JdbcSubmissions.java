/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.CreateResult;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.Reader;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.json.SubmissionContentJson;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter portów zgłoszeń na tabeli submissions. Zapis i edycja to pojedyncze atomowe
 * zapytania, bez odczytu przed zapisem. Zapisuje tylko treść JSON; rodzaj, budynek, punkt
 * i piętro baza liczy z niej w kolumnach generowanych, więc mają jedno źródło. Klasa nie jest
 * repozytorium Springa, bo proxy tłumaczące wyjątki nie powstaje dla klasy finalnej, a
 * JdbcClient tłumaczy wyjątki sam.
 *
 * <p>Wymagania: BR-9, BR-10, BR-18, BR-11</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissions implements SubmissionRepository, SubmissionFinder {

    /**
     * Wstawienie nowego zgłoszenia; istniejący identyfikator niczego nie zmienia.
     */
    private static final String INSERT = String.format(
        """
            INSERT INTO submissions (id, content, author, author_roles)
            VALUES (:id, CAST(:content AS JSONB), :author, :roles)
            ON CONFLICT (id) DO NOTHING
            RETURNING %s
        """,
        SubmissionRows.COLUMNS
    );

    /**
     * Edycja zgłoszenia z oczekiwaną wersją i dozwolonym przejściem statusu.
     */
    private static final String UPDATE = String.format(
        """
            UPDATE submissions SET version = version + 1, status = edited.new_status,
                rejection_reason = NULL, duplicate_of = NULL,
                content = CAST(:content AS JSONB)
            FROM unnest(CAST(:from AS TEXT[]), CAST(:to AS TEXT[]))
                AS edited (old_status, new_status)
            WHERE id = :id AND version = :version AND status = edited.old_status
            RETURNING %s
        """,
        SubmissionRows.COLUMNS
    );

    /**
     * Dostęp do tabeli submissions.
     */
    private final JdbcClient jdbc;

    /**
     * Zapis i odczyt treści zgłoszenia jako JSON.
     */
    private final ObjectMapper mapper;

    /**
     * Odczyt wiersza jako zgłoszenie.
     */
    private final SubmissionRows rows;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli submissions
     * @param mapper Zapis i odczyt treści zgłoszenia jako JSON
     */
    public JdbcSubmissions(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.rows = new SubmissionRows(mapper);
    }

    @Override
    public void lock(final UUID id) {
        this.jdbc.sql("SELECT id FROM submissions WHERE id = :id FOR UPDATE")
            .param("id", id).query(UUID.class).optional();
    }

    @Override
    public CreateResult create(final NewSubmission submission) {
        final Optional<Submission> created = this.write(
            this.jdbc.sql(JdbcSubmissions.INSERT).param("author", submission.author()).param(
                "roles", submission.roles().stream().sorted().map(Enum::name).toArray(String[]::new)
            ),
            submission.id(), submission.content()
        );
        if (created.isEmpty() && this.foreign(submission.id(), submission.author())) {
            throw new ForbiddenChange("Zgłoszenie o tym identyfikatorze złożyła inna osoba");
        }
        final CreateResult result;
        if (created.isPresent()) {
            result = new CreateResult.Created(created.get());
        } else {
            result = new CreateResult.Existing(
                this.find(submission.id()).orElseThrow(
                    () -> new IllegalStateException(
                        String.format("Zgłoszenie %s zniknęło w trakcie zapisu", submission.id())
                    )
                )
            );
        }
        return result;
    }

    @Override
    public EditResult update(final SubmissionEdit edit) {
        if (this.find(edit.id()).filter(
            saved -> !Objects.equals(saved.content().replaces(), edit.content().replaces())
        ).isPresent()) {
            throw new InvalidSubmission("Edycja nie zmienia pola replacesSubmissionId");
        }
        return this.written(
            this.write(
                this.jdbc.sql(JdbcSubmissions.UPDATE)
                    .param("version", edit.expectedVersion())
                    .param("from", EditTransition.from())
                    .param("to", EditTransition.to()),
                edit.id(),
                edit.content()
            ).orElse(null),
            edit.id()
        );
    }

    @Override
    public Optional<Submission> archive(final UUID id) {
        return this.jdbc.sql(
            String.format(
                """
                UPDATE submissions SET status = 'ARCHIVED'
                WHERE id = :id AND status = 'ACCEPTED'
                RETURNING %s
                """,
                SubmissionRows.COLUMNS
            )
        ).param("id", id).query(this.rows).optional();
    }

    @Override
    public Optional<Submission> find(final UUID id) {
        return this.jdbc.sql(
            String.format("SELECT %s FROM submissions WHERE id = :id", SubmissionRows.COLUMNS)
        ).param("id", id).query(this.rows).optional();
    }

    @Override
    public Optional<Submission> visible(final UUID id, final Reader reader) {
        return this.find(id).filter(
            found -> found.status().publiclyVisible() || reader.verifier()
                || this.wrote(id, reader.account())
        );
    }

    @Override
    public boolean foreign(final UUID id, final String account) {
        return this.jdbc.sql(
            """
            SELECT EXISTS (
                SELECT 1 FROM submissions WHERE id = :id AND author IS DISTINCT FROM :account
            )
            """
        ).param("id", id).param("account", account).query(Boolean.class).single();
    }

    @Override
    public boolean authorOnly(final UUID id, final String account) {
        return this.jdbc.sql(
            """
            SELECT EXISTS (
                SELECT 1 FROM submissions
                WHERE id = :id AND kind <> 'QUICK' AND author IS DISTINCT FROM :account
            )
            """
        ).param("id", id).param("account", account).query(Boolean.class).single();
    }

    @Override
    public List<Submission> list(final SubmissionQuery query) {
        return this.select(query, null);
    }

    @Override
    public List<Submission> own(final String author, final SubmissionQuery query) {
        return this.select(query, author);
    }

    private EditResult written(@Nullable final Submission saved, final UUID id) {
        final EditResult result;
        if (saved != null) {
            result = new EditResult.Saved(saved);
        } else {
            result = this.find(id).<EditResult>map(EditResult.Conflict::new)
                .orElseGet(() -> new EditResult.Missing(id));
        }
        return result;
    }

    private boolean wrote(final UUID id, final String account) {
        return !account.isEmpty() && !this.foreign(id, account);
    }

    private List<Submission> select(final SubmissionQuery query, @Nullable final String author) {
        return SubmissionSelection.bind(
            this.jdbc.sql(
                String.format(
                    "SELECT %s FROM submissions WHERE %s ORDER BY created_at, id",
                    SubmissionRows.COLUMNS, SubmissionSelection.WHERE
                )
            ),
            query, author
        ).query(new ReadableRows<>(this.rows));
    }

    private Optional<Submission> write(
        final JdbcClient.StatementSpec statement, final UUID id, final SubmissionContent content
    ) {
        return statement
            .param("id", id)
            .param("content", this.json(content))
            .query(this.rows)
            .optional();
    }

    private String json(final SubmissionContent content) {
        return this.mapper.writeValueAsString(new SubmissionContentJson(content));
    }
}
