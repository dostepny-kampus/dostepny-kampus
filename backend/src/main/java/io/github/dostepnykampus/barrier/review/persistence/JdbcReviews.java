/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.persistence;

import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.ReviewRepository;
import io.github.dostepnykampus.barrier.review.SimilarSubmission;
import io.github.dostepnykampus.barrier.review.SubmissionAuthor;
import io.github.dostepnykampus.barrier.submission.AuthorRole;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.persistence.ReadableRows;
import io.github.dostepnykampus.barrier.submission.persistence.SubmissionRows;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adapter portu weryfikacji na tabeli submissions. Wzięcie i decyzja to pojedyncze
 * atomowe zapytania z warunkiem statusu, Weryfikatora i wersji; przy odmowie wynik
 * niesie bieżący stan zgłoszenia.
 *
 * <p>Wymagania: US10, US10.1, US10.4, BR-3, BR-5, BR-11, BR-13, BR-16</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcReviews implements ReviewRepository {

    /**
     * Wzięcie zgłoszenia do weryfikacji; wzięte już przez tego Weryfikatora zostaje.
     */
    private static final String CLAIM = String.format(
        """
            UPDATE submissions SET status = 'IN_REVIEW', reviewer = :reviewer
            WHERE id = :id AND (status = 'SENT' OR (status = 'IN_REVIEW' AND reviewer = :reviewer))
            RETURNING %s
        """,
        SubmissionRows.COLUMNS
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
        SubmissionRows.COLUMNS
    );

    /**
     * Dostęp do tabeli submissions.
     */
    private final JdbcClient jdbc;

    /**
     * Odczyt wiersza jako zgłoszenie.
     */
    private final SubmissionRows rows;

    /**
     * Bieżący stan zgłoszenia przy odmowie zapisu.
     */
    private final SubmissionFinder finder;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli submissions
     * @param mapper Odczyt treści zgłoszenia z JSON
     * @param finder Odczyt bieżącego stanu zgłoszenia
     */
    public JdbcReviews(
        final JdbcClient jdbc, final ObjectMapper mapper, final SubmissionFinder finder
    ) {
        this.jdbc = jdbc;
        this.rows = new SubmissionRows(mapper);
        this.finder = finder;
    }

    @Override
    public EditResult claim(final UUID id, final String reviewer) {
        return this.result(
            this.jdbc.sql(JdbcReviews.CLAIM).param("id", id).param("reviewer", reviewer)
                .query(this.rows).optional().orElse(null),
            id
        );
    }

    @Override
    public EditResult decide(final ReviewDecision decision) {
        return this.result(
            this.jdbc.sql(JdbcReviews.DECIDE)
                .param("id", decision.id())
                .param("version", decision.expectedVersion())
                .param("reviewer", decision.reviewer())
                .param("status", decision.outcome().name())
                .param("reason", JdbcReviews.reason(decision))
                .param("duplicate", JdbcReviews.duplicate(decision))
                .param("type", JdbcReviews.type(decision))
                .param("severity", JdbcReviews.severity(decision))
                .query(this.rows).optional().orElse(null),
            decision.id()
        );
    }

    @Override
    public boolean claimedBy(final UUID id, final String account) {
        return this.jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM submissions WHERE id = :id AND reviewer = :account)"
        ).param("id", id).param("account", account).query(Boolean.class).single();
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
                SubmissionRows.COLUMNS
            )
        ).param("id", id)
            .param("statuses", SimilarSubmission.STATUSES.stream().map(Enum::name).toList())
            .param("radius", SimilarSubmission.RADIUS_METERS).query(
                new ReadableRows<>(
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
                SubmissionRows.COLUMNS
            )
        ).param("reviewer", reviewer).query(new ReadableRows<>(this.rows));
    }

    @Override
    public Map<UUID, SubmissionAuthor> authors(final Collection<UUID> ids) {
        final Map<UUID, SubmissionAuthor> found = new HashMap<>(ids.size());
        if (!ids.isEmpty()) {
            this.jdbc.sql(
                """
                SELECT id, author, author_roles FROM submissions
                WHERE id IN (:ids) AND author IS NOT NULL
                """
            ).param("ids", ids).query(
                (RowCallbackHandler) row -> found.put(
                    row.getObject("id", UUID.class),
                    new SubmissionAuthor(
                        row.getString("author"),
                        Arrays.stream((String[]) row.getArray("author_roles").getArray())
                            .map(AuthorRole::valueOf).toList()
                    )
                )
            );
        }
        return found;
    }

    private EditResult result(final @Nullable Submission saved, final UUID id) {
        final EditResult result;
        if (saved == null) {
            result = this.finder.find(id).<EditResult>map(EditResult.Conflict::new)
                .orElseGet(() -> new EditResult.Missing(id));
        } else {
            result = new EditResult.Saved(saved);
        }
        return result;
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
}
