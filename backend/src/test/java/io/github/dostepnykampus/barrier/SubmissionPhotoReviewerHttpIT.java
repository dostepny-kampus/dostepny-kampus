/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Weryfikator, który wziął zgłoszenie, zastępuje zapisane zdjęcie wersją z zasłoniętą twarzą
 * bez nowej wersji zgłoszenia; inny Weryfikator dostaje 403.
 *
 * <p>Wymagania: NFR-25, BR-15</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionPhotoReviewerHttpIT {

    /** Zdjęcie z listy photoIds. */
    private static final UUID PHOTO = UUID.fromString("7f8e9d0c-1b2a-4c3d-8e4f-5a6b7c8d9e0f");

    @Test
    void replacesPhotoForClaimingVerifier(final WebApplicationContext context)
        throws Exception {
        final UUID id = SubmissionPhotoReviewerHttpIT.claimed(context);
        MatcherAssert.assertThat(
            "Weryfikator, który wziął zgłoszenie, nie zastąpił zdjęcia albo zmienił wersję",
            List.of(
                SubmissionPhotoReviewerHttpIT.put(context, id, "weryfikator-zdjec"),
                context.getBean(JdbcClient.class)
                    .sql("SELECT version FROM submissions WHERE id = :id")
                    .param("id", id).query(Integer.class).single()
            ),
            Matchers.contains(204, 1)
        );
    }

    @Test
    void refusesOtherVerifier(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator, który nie wziął zgłoszenia, zastąpił jego zdjęcie",
            SubmissionPhotoReviewerHttpIT.put(
                context, SubmissionPhotoReviewerHttpIT.claimed(context), "inny-weryfikator"
            ),
            Matchers.equalTo(403)
        );
    }

    private static UUID claimed(final WebApplicationContext context) throws Exception {
        final UUID id = new PhotoRequests(context)
            .add("IN_REVIEW", SubmissionPhotoReviewerHttpIT.PHOTO.toString());
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET reviewer = 'weryfikator-zdjec' WHERE id = :id")
            .param("id", id).update();
        context.getBean(PhotoStore.class)
            .put(id, SubmissionPhotoReviewerHttpIT.PHOTO, JpegCleanerTest.photo());
        return id;
    }

    private static int put(
        final WebApplicationContext context, final UUID id, final String account
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.put(
                "/submissions/{id}/photos/{photo}", id, SubmissionPhotoReviewerHttpIT.PHOTO
            ).contentType(MediaType.IMAGE_JPEG).content(JpegCleanerTest.photo())
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer VERIFIER.%s", account))
        ).andReturn().getResponse().getStatus();
    }
}
