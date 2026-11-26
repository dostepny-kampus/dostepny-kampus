/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.api.ChangedSubmission;
import io.github.dostepnykampus.barrier.api.SubmissionChanges;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * API zmian zgłoszeń daje od kursora własne zgłoszenia konta w każdym statusie oraz cudze
 * przyjęte i archiwalne, w kształcie schematu Submission kontraktu.
 *
 * <p>Wymagania: US8.1, US1.3, US10.6</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SubmissionChangesIT {

    /** Zdjęcie zgłoszeń próbnych. */
    private static final UUID PHOTO = UUID.fromString("8a9b0c1d-2e3f-4a5b-8c6d-7e8f9a0b1c2d");

    /** Konto czytające zmiany; autor zgłoszeń próbnych. */
    private static final String READER = "konto-testowe";

    @Test
    void givesOwnAndPublicChangedSinceCursor(final WebApplicationContext context) {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final UUID old = samples.add("SENT", SampleSubmissions.quick(SubmissionChangesIT.PHOTO));
        final Instant cursor = jdbc.sql("SELECT clock_timestamp()")
            .query(OffsetDateTime.class).single().toInstant();
        final List<UUID> ids = List.of(
            old,
            samples.add("REJECTED", SampleSubmissions.quick(SubmissionChangesIT.PHOTO)),
            SubmissionChangesIT.foreign(samples, jdbc, "SENT"),
            SubmissionChangesIT.foreign(samples, jdbc, "ACCEPTED"),
            SubmissionChangesIT.foreign(samples, jdbc, "ARCHIVED")
        );
        try {
            MatcherAssert.assertThat(
                "Zmiany od kursora mają zły zestaw zgłoszeń",
                SubmissionChangesIT.changed(context, cursor, ids).stream()
                    .map(ids::indexOf).toList(),
                Matchers.contains(1, 3, 4)
            );
        } finally {
            jdbc.sql("DELETE FROM submissions WHERE id IN (:ids)").param("ids", ids).update();
        }
    }

    @Test
    void writesSubmissionSchema(final WebApplicationContext context) {
        final UUID id = new SampleSubmissions(context)
            .add("ACCEPTED", SampleSubmissions.quick(SubmissionChangesIT.PHOTO));
        try {
            MatcherAssert.assertThat(
                "Zgłoszenie z API zmian nie jest zgodne ze schematem Submission",
                context.getBean(SubmissionChanges.class)
                    .changedSince(Instant.EPOCH, SubmissionChangesIT.READER).stream()
                    .map(change -> change.submission().value())
                    .filter(json -> json.contains(id.toString()))
                    .flatMap(json -> new OpenApiContract().violations("Submission", json).stream())
                    .toList(),
                Matchers.empty()
            );
        } finally {
            context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id = :id")
                .param("id", id).update();
        }
    }

    private static UUID foreign(
        final SampleSubmissions samples, final JdbcClient jdbc, final String status
    ) {
        final UUID id = samples.add(status, SampleSubmissions.quick(SubmissionChangesIT.PHOTO));
        jdbc.sql("UPDATE submissions SET author = 'inny-autor' WHERE id = :id")
            .param("id", id).update();
        return id;
    }

    private static List<UUID> changed(
        final WebApplicationContext context, final Instant cursor, final List<UUID> ids
    ) {
        return context.getBean(SubmissionChanges.class)
            .changedSince(cursor, SubmissionChangesIT.READER).stream()
            .map(ChangedSubmission::submission)
            .map(json -> context.getBean(ObjectMapper.class).readTree(json.value()))
            .map(node -> UUID.fromString(node.path("id").asString()))
            .filter(ids::contains).toList();
    }
}
