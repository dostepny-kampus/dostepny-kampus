/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.review.SimilarSubmission;
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
 * Adapter portów zgłoszeń na tabeli submissions. Zapis
 * i edycja to pojedyncze atomowe zapytania, bez odczytu przed zapisem.
 * Zapisuje tylko treść JSON; rodzaj, budynek, punkt i piętro baza liczy
 * z niej w kolumnach generowanych, więc mają jedno źródło. Klasa
 * nie jest repozytorium Springa, bo proxy tłumaczące wyjątki nie powstaje
 * dla klasy finalnej, a JdbcClient tłumaczy wyjątki sam.
 * TODO(#3998): port weryfikacji przechodzi do adaptera w temacie review.
 *
 * <p>Wymagania: BR-9, BR-10, BR-18, BR-11</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissions
    implements SubmissionRepository, SubmissionFinder, ReviewRepository {

    /**
     * Kolumny zgłoszenia czytane do modelu.
     */
    static final String COLUMNS =
        "id, version, status, created_at, verified_at, content::text AS content, rejection_reason, duplicate_of";

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
                rejection_reason = NULL, duplicate_of = NULL,
                content = CAST(:content AS JSONB)
            FROM unnest(CAST(:from AS TEXT[]), CAST(:to AS TEXT[]))
                AS edited (old_status, new_status)
            WHERE id = :id AND version = :version AND status = edited.old_status
            RETURNING %s
        """,
        JdbcSubmissions.COLUMNS
    );

    /**
     * Wzięcie zgłoszenia do weryfikacji; wzięte już przez tego Weryfikatora zostaje.
     */
    private static final String CLAIM = String.format(
        """
            UPDATE submissions SET status = 'IN_REVIEW', reviewer = :reviewer
            WHERE id = :id AND (status = 'SENT' OR (status = 'IN_REVIEW' AND reviewer = :reviewer))
            RETURNING %s
        """,
        JdbcSubmissions.COLUMNS
    );

    /**
     * Decyzja o zgłoszeniu wziętym przez Weryfikatora, w oczekiwanej wersji.
     */
    private static final String DECIDE = String.format(
        """
            UPDATE submissions SET status = :status, rejection_reason = :reason,
                duplicate_of = CAST(:duplicate AS UUID),
                content = CASE WHEN CAST(:type AS TEXT) IS NULL THEN content
                    ELSE content || JSONB_BUILD_OBJECT(
                        'barrierType', CAST(:type AS TEXT), 'severity', CAST(:severity AS INT)
                    ) END
            WHERE id = :id AND version = :version AND status = 'IN_REVIEW'
                AND reviewer = :reviewer
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
    public EditResult claim(final UUID id, final String reviewer) {
        return this.written(
            this.jdbc.sql(JdbcSubmissions.CLAIM).param("id", id).param("reviewer", reviewer)
                .query(this.rows).optional().orElse(null),
            id
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
                JdbcSubmissions.COLUMNS
            )
        ).param("id", id).query(this.rows).optional();
    }

    @Override
    public EditResult decide(final ReviewDecision decision) {
        return this.written(
            this.jdbc.sql(JdbcSubmissions.DECIDE)
                .param("id", decision.id())
                .param("version", decision.expectedVersion())
                .param("reviewer", decision.reviewer())
                .param("status", decision.outcome().name())
                .param("reason", JdbcSubmissions.reason(decision))
                .param("duplicate", JdbcSubmissions.duplicate(decision))
                .param("type", JdbcSubmissions.type(decision))
                .param("severity", JdbcSubmissions.severity(decision))
                .query(this.rows).optional().orElse(null),
            decision.id()
        );
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
    public boolean claimedBy(final UUID id, final String account) {
        return this.jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM submissions WHERE id = :id AND reviewer = :account)"
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

    @Override
    public List<Submission> reviewQueue(final String reviewer) {
        return this.jdbc.sql(
            String.format(
                """
                SELECT %s FROM submissions
                WHERE (status = 'SENT' OR (status = 'IN_REVIEW' AND reviewer = :reviewer))
                    AND author IS DISTINCT FROM :reviewer
                ORDER BY created_at, id
                """,
                JdbcSubmissions.COLUMNS
            )
        ).param("reviewer", reviewer).query(SubmissionRows.readable(this.rows));
    }

    private static @Nullable String reason(final ReviewDecision decision) {
        final String result;
        if (decision instanceof ReviewDecision.Rejected rejected) {
            result = rejected.reason();
        } else {
            result = null;
        }
        return result;
    }

    private static @Nullable UUID duplicate(final ReviewDecision decision) {
        final UUID result;
        if (decision instanceof ReviewDecision.Rejected rejected) {
            result = rejected.duplicateOf();
        } else {
            result = null;
        }
        return result;
    }

    private static @Nullable String type(final ReviewDecision decision) {
        final String result;
        if (decision instanceof ReviewDecision.Classified classified) {
            result = classified.type().code();
        } else {
            result = null;
        }
        return result;
    }

    private static @Nullable Integer severity(final ReviewDecision decision) {
        final Integer result;
        if (decision instanceof ReviewDecision.Classified classified) {
            result = classified.severity();
        } else {
            result = null;
        }
        return result;
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
                    JdbcSubmissions.COLUMNS, SubmissionSelection.WHERE
                )
            ),
            query, author
        ).query(SubmissionRows.readable(this.rows));
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
