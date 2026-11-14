/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Reader;
import io.github.dostepnykampus.barrier.domain.SimilarSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter portów zgłoszeń na tabeli submissions. Zapis
 * i edycja to pojedyncze atomowe zapytania, bez odczytu przed zapisem.
 * Zapisuje tylko treść JSON; rodzaj, budynek, punkt i piętro baza liczy
 * z niej w kolumnach generowanych, więc mają jedno źródło. Klasa
 * nie jest repozytorium Springa, bo proxy tłumaczące wyjątki nie powstaje
 * dla klasy finalnej, a JdbcClient tłumaczy wyjątki sam.
 *
 * <p>Wymagania: BR-9, BR-10, BR-18</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissions implements SubmissionRepository, SubmissionFinder {

    /**
     * Kolumny zgłoszenia czytane do modelu.
     */
    private static final String COLUMNS =
        "id, version, status, created_at, content::text AS content";

    /**
     * Wstawienie nowego zgłoszenia; istniejący identyfikator niczego nie zmienia.
     */
    private static final String INSERT = String.format(
        """
            INSERT INTO submissions (id, content, author)
            VALUES (:id, CAST(:content AS JSONB), :author)
            ON CONFLICT (id) DO NOTHING
            RETURNING %s
        """,
        JdbcSubmissions.COLUMNS
    );

    /**
     * Edycja zgłoszenia z oczekiwaną wersją i dozwolonym przejściem statusu.
     */
    private static final String UPDATE = String.format(
        """
            UPDATE submissions SET version = version + 1, status = edited.new_status,
                content = CAST(:content AS JSONB)
            FROM unnest(CAST(:from AS TEXT[]), CAST(:to AS TEXT[]))
                AS edited (old_status, new_status)
            WHERE id = :id AND version = :version AND status = edited.old_status
            RETURNING %s
        """,
        JdbcSubmissions.COLUMNS
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
            this.jdbc.sql(JdbcSubmissions.INSERT).param("author", submission.author()),
            submission.id(), submission.content()
        );
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
        final Optional<Submission> saved = this.write(
            this.jdbc.sql(JdbcSubmissions.UPDATE)
                .param("version", edit.expectedVersion())
                .param("from", JdbcSubmissions.editable(Function.identity()))
                .param("to", JdbcSubmissions.editable(SubmissionStatus::afterEdit)),
            edit.id(),
            edit.content()
        );
        final EditResult result;
        if (saved.isPresent()) {
            result = new EditResult.Saved(saved.get());
        } else {
            result = this.find(edit.id()).<EditResult>map(EditResult.Conflict::new)
                .orElseGet(() -> new EditResult.Missing(edit.id()));
        }
        return result;
    }

    @Override
    public Optional<Submission> find(final UUID id) {
        return this.jdbc.sql(
            String.format("SELECT %s FROM submissions WHERE id = :id", JdbcSubmissions.COLUMNS)
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

    @Override
    public List<SimilarSubmission> similar(final UUID id) {
        return this.jdbc.sql(
            String.format(
                """
                SELECT %s, distance FROM (
                    SELECT other.*, ROUND(CAST(ST_Distance(other.location::geography,
                        checked.location::geography) AS NUMERIC), 2) AS distance
                    FROM submissions AS other
                    JOIN submissions AS checked ON checked.id = :id AND other.id <> checked.id
                    WHERE other.status IN (:statuses)
                        -- zapas na zaokrąglenie do centymetra: granica promienia widoczna
                        AND ST_DWithin(other.location::geography,
                            checked.location::geography, :radius + 0.005)
                ) AS near
                WHERE distance <= :radius
                ORDER BY distance, created_at, id
                """,
                JdbcSubmissions.COLUMNS
            )
        ).param("id", id)
            .param("statuses", SimilarSubmission.STATUSES.stream().map(Enum::name).toList())
            .param("radius", SimilarSubmission.RADIUS_METERS).query(
                SubmissionRows.readable(
                    (row, num) -> new SimilarSubmission(
                        this.rows.mapRow(row, num), row.getDouble("distance")
                    )
                )
            );
    }

    private boolean wrote(final UUID id, final String account) {
        return !account.isEmpty() && !this.foreign(id, account);
    }

    private List<Submission> select(final SubmissionQuery query, @Nullable final String author) {
        return this.jdbc.sql(
            String.format(
                """
                SELECT %s FROM submissions WHERE status IN (:statuses)
                    AND (CAST(:building AS TEXT) IS NULL OR building_symbol = :building)
                    AND (CAST(:floor AS TEXT) IS NULL OR floor_id = :floor)
                    AND (CAST(:author AS TEXT) IS NULL OR author = :author)
                ORDER BY created_at, id
                """,
                JdbcSubmissions.COLUMNS
            )
        ).param("statuses", query.statuses().stream().map(Enum::name).toList())
            .param("building", query.buildingSymbol())
            .param("floor", query.floorId())
            .param("author", author)
            .query(SubmissionRows.readable(this.rows));
    }

    private Optional<Submission> write(
        final JdbcClient.StatementSpec statement, final UUID id, final SubmissionContent content
    ) {
        try {
            return statement
                .param("id", id)
                .param("content", this.json(content))
                .query(this.rows)
                .optional();
        } catch (final DataIntegrityViolationException ex) {
            throw JdbcSubmissions.translated(ex);
        }
    }

    private String json(final SubmissionContent content) {
        return this.mapper.writeValueAsString(new SubmissionContentJson(content));
    }

    private static RuntimeException translated(final DataIntegrityViolationException error) {
        final RuntimeException result;
        if (
            error.getMostSpecificCause() instanceof SQLException cause
                && "23503".equals(cause.getSQLState())
        ) {
            result = new InvalidSubmission("Nieznany budynek zgłoszenia");
        } else {
            result = error;
        }
        return result;
    }

    private static String[] editable(final Function<SubmissionStatus, SubmissionStatus> after) {
        return Arrays.stream(SubmissionStatus.values())
            .filter(SubmissionStatus::editable)
            .map(after)
            .map(Enum::name)
            .toArray(String[]::new);
    }
}
