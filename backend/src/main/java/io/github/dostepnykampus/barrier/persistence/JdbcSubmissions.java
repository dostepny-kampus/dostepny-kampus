/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.json.SubmissionContentJson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter portów zgłoszeń na tabeli submissions (BR-9, BR-10, BR-18). Zapis
 * i edycja to pojedyncze atomowe zapytania, bez odczytu przed zapisem. Klasa
 * nie jest repozytorium Springa, bo proxy tłumaczące wyjątki nie powstaje
 * dla klasy finalnej, a JdbcClient tłumaczy wyjątki sam.
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
     * Stan SQL naruszenia klucza obcego; jedynym kluczem tabeli jest budynek.
     */
    private static final String FOREIGN_KEY = "23503";

    /**
     * Dostęp do tabeli submissions.
     */
    private final JdbcClient jdbc;

    /**
     * Zapis i odczyt treści zgłoszenia jako JSON.
     */
    private final ObjectMapper mapper;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli submissions
     * @param mapper Zapis i odczyt treści zgłoszenia jako JSON
     */
    public JdbcSubmissions(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public CreateResult create(final NewSubmission submission) {
        final CreateResult result;
        final Optional<Submission> created = this.write(
            String.format(
                """
                INSERT INTO submissions (id, kind, building_symbol, location, content)
                VALUES (:id, :kind, :building,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), CAST(:content AS JSONB))
                ON CONFLICT (id) DO NOTHING
                RETURNING %s
                """,
                JdbcSubmissions.COLUMNS
            ),
            submission.id(), 1, submission.content()
        );
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
            String.format(
                """
                UPDATE submissions SET version = version + 1, status = edited.new_status,
                    kind = :kind, building_symbol = :building,
                    location = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
                    content = CAST(:content AS JSONB)
                FROM unnest(CAST(:from AS TEXT[]), CAST(:to AS TEXT[]))
                    AS edited (old_status, new_status)
                WHERE id = :id AND version = :version AND status = edited.old_status
                RETURNING %s
                """,
                JdbcSubmissions.COLUMNS
            ),
            edit.id(), edit.expectedVersion(), edit.content()
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
        ).param("id", id).query(this::row).optional();
    }

    @Override
    public List<Submission> list(final SubmissionQuery query) {
        return this.jdbc.sql(
            String.format(
                """
                SELECT %s FROM submissions WHERE status IN (:statuses)
                    AND (CAST(:building AS TEXT) IS NULL OR building_symbol = :building)
                    AND (CAST(:floor AS TEXT) IS NULL OR content #>> '{floor,floorId}' = :floor)
                ORDER BY created_at, id
                """,
                JdbcSubmissions.COLUMNS
            )
        ).param("statuses", query.statuses().stream().map(Enum::name).toList())
            .param("building", query.buildingSymbol())
            .param("floor", query.floorId())
            .query(this::row)
            .list();
    }

    private Optional<Submission> write(
        final String sql, final UUID id, final int version, final SubmissionContent content
    ) {
        final SubmissionPlace place = content.place();
        try {
            return this.jdbc.sql(sql)
                .param("id", id)
                .param("version", version)
                .param("kind", content.kind().name())
                .param("building", place.buildingSymbol())
                .param("lon", place.point().longitude())
                .param("lat", place.point().latitude())
                .param(
                    "content", this.mapper.writeValueAsString(new SubmissionContentJson(content))
                )
                .param("from", JdbcSubmissions.editable(Function.identity()))
                .param("to", JdbcSubmissions.editable(SubmissionStatus::afterEdit))
                .query(this::row)
                .optional();
        } catch (final DataIntegrityViolationException ex) {
            if (
                ex.getMostSpecificCause() instanceof SQLException cause
                    && JdbcSubmissions.FOREIGN_KEY.equals(cause.getSQLState())
            ) {
                throw new InvalidSubmission("Nieznany budynek zgłoszenia");
            }
            throw ex;
        }
    }

    private Submission row(final ResultSet rows, final int num) throws SQLException {
        final UUID id = rows.getObject("id", UUID.class);
        try {
            return new Submission(
                id,
                rows.getInt("version"),
                SubmissionStatus.valueOf(rows.getString("status")),
                rows.getObject("created_at", OffsetDateTime.class).toInstant(),
                this.mapper.readValue(rows.getString("content"), SubmissionContentJson.class)
                    .toContent()
            );
        } catch (final JacksonException | InvalidSubmission ex) {
            throw new IllegalStateException(
                String.format("Zgłoszenia %s nie da się odczytać", id), ex
            );
        }
    }

    private static String[] editable(final Function<SubmissionStatus, SubmissionStatus> after) {
        return Arrays.stream(SubmissionStatus.values())
            .filter(SubmissionStatus::editable)
            .map(after)
            .map(Enum::name)
            .toArray(String[]::new);
    }
}
