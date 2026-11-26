/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.review.ReviewDecision;
import io.github.dostepnykampus.barrier.review.application.ReviewConflict;
import io.github.dostepnykampus.barrier.review.application.SubmissionReview;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Usługa weryfikacji sama sprawdza reguły wzięcia i decyzji: własne zgłoszenie, wzięcie przez
 * innego Weryfikatora, komplet zdjęć w magazynie i decyzję dopuszczalną dla rodzaju zgłoszenia;
 * cudze zgłoszenie z kompletem zdjęć bierze konto Weryfikatora z polecenia.
 *
 * <p>Wymagania: BR-4, BR-7, BR-11, BR-14</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionReviewRulesIT {

    /** Zdjęcie zgłoszeń próbnych. */
    private static final UUID PHOTO = UUID.fromString("3c4d5e6f-7a8b-4c9d-8e0f-1a2b3c4d5e6f");

    /** Konto Weryfikatora próbnego. */
    private static final String REVIEWER = "weryfikator-regul";

    @Test
    void claimsForeignSubmissionWithPhotos(final WebApplicationContext context) {
        final UUID id = SubmissionReviewRulesIT.sent(context);
        context.getBean(PhotoStore.class).put(
            id, SubmissionReviewRulesIT.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
        context.getBean(SubmissionReview.class).claim(id, SubmissionReviewRulesIT.REVIEWER);
        Assertions.assertEquals(
            "IN_REVIEW weryfikator-regul",
            context.getBean(JdbcClient.class)
                .sql("SELECT status || ' ' || reviewer FROM submissions WHERE id = :id")
                .param("id", id).query(String.class).single(),
            "Wzięcie nie zapisało statusu albo konta Weryfikatora"
        );
    }

    @Test
    void refusesOwnSubmission(final WebApplicationContext context) {
        final UUID id = SubmissionReviewRulesIT.sent(context);
        Assertions.assertThrows(
            ForbiddenChange.class,
            () -> context.getBean(SubmissionReview.class).claim(id, "konto-testowe"),
            "Autor wziął własne zgłoszenie do weryfikacji"
        );
    }

    @Test
    void refusesDecisionOnSubmissionClaimedByOther(final WebApplicationContext context) {
        final UUID id = SubmissionReviewRulesIT.claimed(context, "inny-weryfikator");
        Assertions.assertThrows(
            ForbiddenChange.class,
            () -> context.getBean(SubmissionReview.class).decide(
                new ReviewDecision.Rejected(id, 1, SubmissionReviewRulesIT.REVIEWER, "Duplikat")
            ),
            "Weryfikator rozstrzygnął zgłoszenie wzięte przez innego"
        );
    }

    @Test
    void refusesClaimWithoutStoredPhoto(final WebApplicationContext context) {
        final UUID id = SubmissionReviewRulesIT.sent(context);
        Assertions.assertEquals(
            id,
            Assertions.assertThrows(
                ReviewConflict.class,
                () -> context.getBean(SubmissionReview.class)
                    .claim(id, SubmissionReviewRulesIT.REVIEWER),
                "Weryfikator wziął zgłoszenie bez zdjęcia w magazynie"
            ).current().id(),
            "Konflikt nie niesie bieżącego zgłoszenia"
        );
    }

    @Test
    void refusesDecisionNotAdmittedForKind(final WebApplicationContext context) {
        final UUID id = SubmissionReviewRulesIT.claimed(context, SubmissionReviewRulesIT.REVIEWER);
        context.getBean(PhotoStore.class).put(
            id, SubmissionReviewRulesIT.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
        Assertions.assertThrows(
            InvalidSubmission.class,
            () -> context.getBean(SubmissionReview.class).decide(
                new ReviewDecision.Accepted(id, 1, SubmissionReviewRulesIT.REVIEWER)
            ),
            "Przyjęcie zgłoszenia szybkiego bez typu i uciążliwości przeszło"
        );
    }

    private static UUID sent(final WebApplicationContext context) {
        return new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(SubmissionReviewRulesIT.PHOTO));
    }

    private static UUID claimed(final WebApplicationContext context, final String reviewer) {
        final UUID id = new SampleSubmissions(context)
            .add("IN_REVIEW", SampleSubmissions.quick(SubmissionReviewRulesIT.PHOTO));
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET reviewer = :reviewer WHERE id = :id")
            .param("reviewer", reviewer).param("id", id).update();
        return id;
    }
}
