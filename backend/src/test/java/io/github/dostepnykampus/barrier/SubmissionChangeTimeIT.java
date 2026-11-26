/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Baza zapisuje chwilę ostatniej zmiany zgłoszenia: przy zapisie i przy każdej zmianie wiersza.
 *
 * <p>Wymagania: US8.1, US1.3</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SubmissionChangeTimeIT {

    /**
     * Zdjęcie zgłoszeń próbnych.
     */
    private static final UUID PHOTO = UUID.fromString("5d6e7f80-91a2-4b3c-8d4e-5f6a7b8c9d0e");

    @Test
    void storesChangeTimeOnInsert(final WebApplicationContext context) {
        final UUID id = new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(SubmissionChangeTimeIT.PHOTO));
        try {
            Assertions.assertTrue(
                context.getBean(JdbcClient.class).sql(
                    """
                    SELECT changed_at >= created_at AND changed_at <= statement_timestamp()
                    FROM submissions WHERE id = :id
                    """
                ).param("id", id).query(Boolean.class).single(),
                "Nowe zgłoszenie nie ma chwili zmiany między zapisem a odczytem"
            );
        } finally {
            SubmissionChangeTimeIT.remove(context, id);
        }
    }

    @Test
    void movesChangeTimeOnUpdate(final WebApplicationContext context) {
        final UUID id = new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(SubmissionChangeTimeIT.PHOTO));
        try {
            final JdbcClient jdbc = context.getBean(JdbcClient.class);
            jdbc.sql(
                """
                UPDATE submissions SET status = 'ACCEPTED',
                    changed_at = TIMESTAMPTZ '2026-01-01T00:00:00Z'
                WHERE id = :id
                """
            ).param("id", id).update();
            Assertions.assertTrue(
                jdbc.sql(
                    """
                    SELECT changed_at >= created_at AND changed_at <= statement_timestamp()
                    FROM submissions WHERE id = :id
                    """
                ).param("id", id).query(Boolean.class).single(),
                "Zmiana wiersza nie zapisała chwili zmiany, została wartość wpisana w poleceniu"
            );
        } finally {
            SubmissionChangeTimeIT.remove(context, id);
        }
    }

    private static void remove(final WebApplicationContext context, final UUID id) {
        context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id = :id")
            .param("id", id).update();
    }
}
