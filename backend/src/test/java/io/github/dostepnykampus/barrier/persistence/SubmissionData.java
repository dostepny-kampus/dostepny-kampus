/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Dane wspólne testów adaptera: budynki próbne, treść, identyfikatory z jednym
 * przedrostkiem (po nim sprząta {@link #clean()}) i odczyt wiersza z bazy.
 *
 * @since 0.1.0
 */
final class SubmissionData {

    /** Budynek próbny, w którym leżą zgłoszenia testów. */
    static final String BUILDING = "J-900";

    /** Drugi budynek próbny. */
    static final String OTHER = "J-901";

    /** Konto, które składa zgłoszenia testów. */
    static final String AUTHOR = "konto-testowe";

    /** Dostęp do bazy. */
    private final JdbcClient jdbc;

    /** Badany adapter. */
    private final JdbcSubmissions adapter;

    private SubmissionData(final JdbcClient jdbc, final JdbcSubmissions adapter) {
        this.jdbc = jdbc;
        this.adapter = adapter;
    }

    static SubmissionData of(final ApplicationContext context) {
        final SubmissionData data = new SubmissionData(
            context.getBean(JdbcClient.class), context.getBean(JdbcSubmissions.class)
        );
        for (final String symbol : List.of(SubmissionData.BUILDING, SubmissionData.OTHER)) {
            new SampleBuilding(symbol, "Budynek próbny", "BASIC")
                .wkt(data.jdbc, "POLYGON((0 0,0.001 0,0.001 0.001,0 0.001,0 0))");
        }
        return data;
    }

    static UUID id(final long number) {
        return new UUID(0x4A5A000000004000L, number);
    }

    static SubmissionContent content(final String description) {
        return SubmissionData.content(SubmissionData.BUILDING, "p1", description);
    }

    static SubmissionContent content(final String building, final String floor, final String text) {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(
                new SubmissionPoint(17.06, 51.1), 4.5, true, building,
                new SubmissionFloor(floor, 1, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, null, 2, text, false),
            null, null, null,
            List.of(UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
        );
    }

    JdbcSubmissions repo() {
        return this.adapter;
    }

    JdbcClient jdbc() {
        return this.jdbc;
    }

    void clean() {
        this.jdbc.sql("DELETE FROM submissions WHERE id::text LIKE '4a5a0000-%'").update();
        this.jdbc.sql("DELETE FROM buildings WHERE symbol IN ('J-900', 'J-901')").update();
    }

    UUID seed(final SubmissionStatus status) {
        return this.seed(status.name().hashCode(), SubmissionData.content("opis"), status);
    }

    UUID seed(final long number, final SubmissionContent content, final SubmissionStatus status) {
        final UUID id = SubmissionData.id(number);
        this.adapter.create(new NewSubmission(id, content, SubmissionData.AUTHOR));
        this.jdbc.sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status.name()).param("id", id).update();
        return id;
    }

    String author(final UUID id) {
        return this.jdbc.sql("SELECT author FROM submissions WHERE id = :id")
            .param("id", id).query(String.class).single();
    }

    Instant created(final UUID id) {
        return this.jdbc.sql("SELECT created_at FROM submissions WHERE id = :id")
            .param("id", id).query(OffsetDateTime.class).single().toInstant();
    }

    Map<String, Object> row(final UUID id) {
        return this.jdbc.sql(
            """
            SELECT version, status, kind, building_symbol, ST_AsText(location) AS location,
                content::text AS content, created_at::text AS created_at
            FROM submissions WHERE id = :id
            """
        ).param("id", id).query().singleRow();
    }
}
