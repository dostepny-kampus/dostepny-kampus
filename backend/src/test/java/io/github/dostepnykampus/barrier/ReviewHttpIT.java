/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Weryfikator bierze cudze zgłoszenie do weryfikacji i rozstrzyga je, ale nie własne
 * ani takie, któremu brakuje zdjęcia w magazynie.
 *
 * <p>Wymagania: US10.3, BR-4, BR-11, BR-3, BR-14, BR-16, BR-7, BR-8</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ReviewHttpIT {

    /**
     * Zdjęcie zgłoszenia próbnego.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void refusesReviewOfOwnSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "konto-testowe");
        MatcherAssert.assertThat(
            "Decyzja o własnym zgłoszeniu nie dała 403 albo zmieniła jego status",
            List.of(
                ReviewHttpIT.review(context, id), ReviewHttpIT.status(context, id)
            ),
            Matchers.contains(403, "SENT")
        );
    }

    @Test
    void refusesReviewOfSubmissionNotClaimed(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Decyzja o zgłoszeniu niewziętym przez Weryfikatora nie dała 403",
            ReviewHttpIT.review(context, ReviewHttpIT.sent(context, "inne-konto")),
            Matchers.equalTo(403)
        );
    }

    @Test
    void acceptsSubmissionClaimedByReviewer(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        MatcherAssert.assertThat(
            "Przyjęcie wziętego zgłoszenia nie dało 200 i statusu „przyjęte”",
            List.of(
                ReviewHttpIT.review(context, id), ReviewHttpIT.status(context, id)
            ),
            Matchers.contains(200, "ACCEPTED")
        );
    }

    @Test
    void savesClassificationOfAcceptedQuickSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        MatcherAssert.assertThat(
            "Przyjęcie zgłoszenia szybkiego nie dało 200 i typu z uciążliwością w treści",
            List.of(
                ReviewHttpIT.review(context, id),
                ReviewHttpIT.field(context, id, "barrierType"),
                ReviewHttpIT.field(context, id, "severity")
            ),
            Matchers.contains(200, "TB-3", "2")
        );
    }

    @Test
    void refusesQuickSubmissionWithoutClassification(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Przyjęcie zgłoszenia szybkiego bez typu i uciążliwości nie dało 400 i IN_REVIEW",
            ReviewHttpIT.refused(context, "{\"decision\":\"ACCEPT\",\"version\":1}"),
            Matchers.contains(400, "IN_REVIEW")
        );
    }

    @Test
    void refusesSeverityOutsideRange(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Uciążliwość 4 przy przyjęciu nie dała 400 i statusu IN_REVIEW",
            ReviewHttpIT.refused(
                context,
                "{\"decision\":\"ACCEPT\",\"version\":1,\"barrierType\":\"TB-3\",\"severity\":4}"
            ),
            Matchers.contains(400, "IN_REVIEW")
        );
    }

    @Test
    void reportsMissingSubmissionOnClaim(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Wzięcie nieistniejącego zgłoszenia nie dało 404",
            ReviewHttpIT.claim(context, UUID.randomUUID()), Matchers.equalTo(404)
        );
    }

    @Test
    void conflictsOnReviewOfStaleVersion(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        MatcherAssert.assertThat(
            "Decyzja o innej wersji niż aktualna nie dała 409",
            ReviewHttpIT.review(
                context, id,
                "{\"decision\":\"ACCEPT\",\"version\":2,\"barrierType\":\"TB-3\",\"severity\":2}"
            ),
            Matchers.equalTo(409)
        );
    }

    @Test
    void refusesRejectionWithoutReason(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        MatcherAssert.assertThat(
            "Odrzucenie bez powodu nie dało 400",
            ReviewHttpIT.review(
                context, id, "{\"decision\":\"REJECT\",\"version\":1}"
            ),
            Matchers.equalTo(400)
        );
    }

    @Test
    void rejectsClaimedSubmissionWithReason(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        MatcherAssert.assertThat(
            "Odrzucenie z powodem nie dało 200 i statusu „odrzucone”",
            List.of(
                ReviewHttpIT.review(
                    context, id,
                    "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Nieczytelne\"}"
                ),
                ReviewHttpIT.status(context, id)
            ),
            Matchers.contains(200, "REJECTED")
        );
    }

    @Test
    void returnsRejectionReasonAndDuplicateInResponse(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        final UUID earlier = UUID.fromString("0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");
        ReviewHttpIT.claim(context, id);
        ReviewHttpIT.review(
            context, id,
            String.format(
                "{\"decision\":\"REJECT\",\"version\":1,\"rejectionReason\":\"Duplikat\",\"duplicateOfSubmissionId\":\"%s\"}",
                earlier
            )
        );
        final JsonNode body = ReviewHttpIT.fetched(context, id);
        MatcherAssert.assertThat(
            "Odpowiedź odrzuconego zgłoszenia nie ma powodu i duplikatu",
            List.of(
                body.path("rejectionReason").asString(),
                body.path("duplicateOfSubmissionId").asString()
            ),
            Matchers.contains("Duplikat", earlier.toString())
        );
    }

    @Test
    void omitsRejectionFieldsOfSubmissionNotRejected(final WebApplicationContext context)
        throws Exception {
        final JsonNode body = ReviewHttpIT.fetched(
            context, ReviewHttpIT.sent(context, "inne-konto")
        );
        MatcherAssert.assertThat(
            "Odpowiedź zgłoszenia nieodrzuconego ma pola odrzucenia",
            List.of(body.has("rejectionReason"), body.has("duplicateOfSubmissionId")),
            Matchers.contains(false, false)
        );
    }

    @Test
    void refusesClaimOfOwnSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "konto-testowe");
        MatcherAssert.assertThat(
            "Wzięcie własnego zgłoszenia nie dało 403 albo zmieniło jego status",
            List.of(ReviewHttpIT.claim(context, id), ReviewHttpIT.status(context, id)),
            Matchers.contains(403, "SENT")
        );
    }

    @Test
    void claimsForeignSubmissionOnceAndAgain(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        MatcherAssert.assertThat(
            "Wzięcie cudzego zgłoszenia, także ponowne, nie dało 200 i statusu „w weryfikacji”",
            List.of(
                ReviewHttpIT.claim(context, id), ReviewHttpIT.status(context, id),
                ReviewHttpIT.claim(context, id)
            ),
            Matchers.contains(200, "IN_REVIEW", 200)
        );
    }

    @Test
    void conflictsOnSubmissionClaimedByAnotherReviewer(
        final WebApplicationContext context
    ) throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = 'IN_REVIEW', reviewer = 'inny' WHERE id = :id")
            .param("id", id).update();
        MatcherAssert.assertThat(
            "Wzięcie zgłoszenia wziętego przez innego Weryfikatora nie dało 409",
            ReviewHttpIT.claim(context, id), Matchers.equalTo(409)
        );
    }

    @Test
    void conflictsOnClaimWithoutPhoto(final WebApplicationContext context) throws Exception {
        final UUID id = ReviewHttpIT.withoutPhoto(context, "inne-konto");
        MatcherAssert.assertThat(
            "Wzięcie zgłoszenia bez zdjęcia w magazynie nie dało 409 albo zmieniło status",
            List.of(ReviewHttpIT.claim(context, id), ReviewHttpIT.status(context, id)),
            Matchers.contains(409, "SENT")
        );
    }

    @Test
    void conflictsOnDecisionWithoutPhoto(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        context.getBean(PhotoStore.class).delete(id, ReviewHttpIT.PHOTO);
        MatcherAssert.assertThat(
            "Decyzja o zgłoszeniu bez zdjęcia w magazynie nie dała 409 albo zmieniła status",
            List.of(ReviewHttpIT.review(context, id), ReviewHttpIT.status(context, id)),
            Matchers.contains(409, "IN_REVIEW")
        );
    }

    @Test
    void appliesAndArchivesAcceptedEntranceMarking(final WebApplicationContext context)
        throws Exception {
        final UUID id = ReviewHttpIT.entranceChange(context);
        final int claim = ReviewHttpIT.claim(context, id);
        final int review = ReviewHttpIT.review(context, id, "{\"decision\":\"ACCEPT\",\"version\":1}");
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        MatcherAssert.assertThat(
            "Przyjęcie zmiany stosuje oznaczenie wejścia i archiwizuje zgłoszenie",
            List.of(
                claim, review, ReviewHttpIT.status(context, id),
                jdbc.sql(
                    """
                    SELECT marking || '/' || marking_source FROM entrances
                    WHERE building_symbol = 'C-13' AND id = 'spec-review-entrance'
                    """
                ).query(String.class).single()
            ),
            Matchers.contains(200, 200, "ARCHIVED", "INACCESSIBLE/WALKTHROUGH")
        );
    }

    private static UUID entranceChange(final WebApplicationContext context) throws IOException {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, marking,
                marking_date, marking_source)
            VALUES ('spec-review-entrance', 'C-13', FALSE, ST_SetSRID(ST_Point(17, 51), 4326),
                'ACCESSIBLE', DATE '2026-11-10', 'IMPORT')
            """
        ).update();
        jdbc.sql(
            """
            UPDATE submissions SET content = content - 'barrierGroup' || '{
                "kind":"ENTRANCE_MARKING","buildingSymbol":"C-13",
                "entranceId":"spec-review-entrance","entranceMarking":"INACCESSIBLE"
            }'::jsonb WHERE id = :id
            """
        ).param("id", id).update();
        return id;
    }

    private static List<Object> refused(final WebApplicationContext context, final String body)
        throws Exception {
        final UUID id = ReviewHttpIT.sent(context, "inne-konto");
        ReviewHttpIT.claim(context, id);
        return List.of(
            ReviewHttpIT.review(context, id, body), ReviewHttpIT.status(context, id)
        );
    }

    private static JsonNode fetched(final WebApplicationContext context, final UUID id)
        throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.get("/submissions/{id}", id).with(new AsVerifier())
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private static UUID sent(final WebApplicationContext context, final String author)
        throws IOException {
        final UUID id = ReviewHttpIT.withoutPhoto(context, author);
        context.getBean(PhotoStore.class).put(id, ReviewHttpIT.PHOTO, JpegCleanerTest.photo());
        return id;
    }

    private static UUID withoutPhoto(final WebApplicationContext context, final String author) {
        final UUID id = new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(ReviewHttpIT.PHOTO));
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = :author WHERE id = :id")
            .param("author", author).param("id", id).update();
        return id;
    }

    private static String status(final WebApplicationContext context, final UUID id) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT status FROM submissions WHERE id = :id")
            .param("id", id).query(String.class).single();
    }

    private static String field(
        final WebApplicationContext context, final UUID id, final String name
    ) {
        return context.getBean(JdbcClient.class)
            .sql("SELECT content ->> :name FROM submissions WHERE id = :id")
            .param("name", name).param("id", id).query(String.class).single();
    }

    private static int claim(final WebApplicationContext context, final UUID id)
        throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions/{id}/claim", id).with(new AsVerifier())
        ).andReturn().getResponse().getStatus();
    }

    private static int review(final WebApplicationContext context, final UUID id)
        throws Exception {
        return ReviewHttpIT.review(
            context, id,
            "{\"decision\":\"ACCEPT\",\"version\":1,\"barrierType\":\"TB-3\",\"severity\":2}"
        );
    }

    private static int review(
        final WebApplicationContext context, final UUID id, final String body
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(new AsVerifier())
        ).andReturn().getResponse().getStatus();
    }
}
