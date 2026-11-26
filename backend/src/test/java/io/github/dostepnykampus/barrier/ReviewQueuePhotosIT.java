/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.application.ReviewQueue;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Usługa kolejki weryfikacji pomija zgłoszenie, któremu brakuje zdjęcia w magazynie.
 *
 * <p>Wymagania: BR-7</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ReviewQueuePhotosIT {

    /**
     * Zdjęcie zgłoszeń próbnych.
     */
    private static final UUID PHOTO = UUID.fromString("6e7f8091-a2b3-4c4d-8e5f-6a7b8c9d0e1f");

    @Test
    void skipsSubmissionWithMissingPhoto(final WebApplicationContext context) {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final UUID complete =
            samples.add("SENT", SampleSubmissions.quick(ReviewQueuePhotosIT.PHOTO));
        final UUID missing =
            samples.add("SENT", SampleSubmissions.quick(ReviewQueuePhotosIT.PHOTO));
        context.getBean(PhotoStore.class).put(
            complete, ReviewQueuePhotosIT.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
        try {
            MatcherAssert.assertThat(
                "Kolejka zawiera zgłoszenie bez zdjęcia w magazynie albo pomija kompletne",
                context.getBean(ReviewQueue.class).forReviewer("weryfikator-kolejki").stream()
                    .map(Submission::id).filter(id -> List.of(complete, missing).contains(id))
                    .toList(),
                Matchers.contains(complete)
            );
        } finally {
            context.getBean(JdbcClient.class)
                .sql("DELETE FROM submissions WHERE id IN (:ids)")
                .param("ids", List.of(complete, missing)).update();
        }
    }
}
