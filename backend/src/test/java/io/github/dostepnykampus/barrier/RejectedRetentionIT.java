/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.application.RejectedRetention;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zgłoszenie odrzucone ponad 90 dni temu znika razem ze zdjęciami, a odrzucone 89 dni temu
 * i przyjęte zostają; chwilę odrzucenia zapisuje baza przy zmianie statusu.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class RejectedRetentionIT {

    /**
     * Zdjęcie zgłoszeń próbnych.
     */
    private static final UUID PHOTO = UUID.fromString("7e2f1a3b-4c5d-4e6f-8a7b-9c0d1e2f3a4b");

    @Test
    void purgesRejectedOlderThanNinetyDays(final WebApplicationContext context)
        throws Exception {
        final UUID old = RejectedRetentionIT.rejected(context, 91);
        final UUID young = RejectedRetentionIT.rejected(context, 89);
        final UUID accepted = new SampleSubmissions(context)
            .add("ACCEPTED", SampleSubmissions.quick(RejectedRetentionIT.PHOTO));
        try {
            context.getBean(RejectedRetention.class).purge();
            Assertions.assertEquals(
                "0 0 1 1",
                String.format(
                    "%d %d %d %d", RejectedRetentionIT.count(context, old),
                    context.getBean(PhotoStore.class).stored(old).size(),
                    RejectedRetentionIT.count(context, young),
                    RejectedRetentionIT.count(context, accepted)
                ),
                "Stare odrzucone zgłoszenie albo jego zdjęcie zostało, albo zniknęło młodsze"
            );
        } finally {
            context.getBean(JdbcClient.class)
                .sql("DELETE FROM submissions WHERE id IN (:young, :accepted)")
                .param("young", young).param("accepted", accepted).update();
        }
    }

    private static UUID rejected(final WebApplicationContext context, final int days)
        throws Exception {
        final UUID id = new SampleSubmissions(context)
            .add("REJECTED", SampleSubmissions.quick(RejectedRetentionIT.PHOTO));
        context.getBean(PhotoStore.class)
            .put(id, RejectedRetentionIT.PHOTO, JpegCleanerTest.photo());
        context.getBean(JdbcClient.class).sql(
            """
            UPDATE submissions SET rejected_at = rejected_at - make_interval(days => :days)
            WHERE id = :id AND rejected_at IS NOT NULL
            """
        ).param("days", days).param("id", id).update();
        return id;
    }

    private static int count(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT count(*) FROM submissions WHERE id = :id")
            .param("id", id).query(Integer.class).single();
    }
}
