/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Kolejka Weryfikatora GET /review/queue: kolejność od najdawniej wysłanego,
 * własne wzięte zostają, własne zgłoszenia i zmiany „bariera usunięta”,
 * cudze wzięte i zgłoszenia bez kompletu zdjęć nie trafiają do kolejki.
 *
 * <p>Wymagania: US10.4, US10.5, US10.7, BR-4, BR-7, BR-11</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ReviewQueueHttpIT {

    /**
     * Adres kolejki weryfikacji.
     */
    private static final String QUEUE = "/review/queue";

    /**
     * Pierwsza chwila wysłania używana w testach kolejności.
     */
    private static final OffsetDateTime FIRST = OffsetDateTime.parse("2026-10-26T10:00:00Z");

    @Test
    void ordersQueueFromOldestAndKeepsOwnClaimed(final WebApplicationContext context)
        throws Exception {
        final UUID older = ReviewQueueHttpIT.complete(
            context, "SENT", "inne-konto", ReviewQueueHttpIT.FIRST
        );
        final UUID mine = ReviewQueueHttpIT.complete(
            context, "IN_REVIEW", "inne-konto", ReviewQueueHttpIT.FIRST.plusHours(2)
        );
        final UUID newer = ReviewQueueHttpIT.complete(
            context, "SENT", "inne-konto", ReviewQueueHttpIT.FIRST.plusDays(1)
        );
        final UUID theirs = ReviewQueueHttpIT.complete(
            context, "IN_REVIEW", "inne-konto", ReviewQueueHttpIT.FIRST.minusDays(1)
        );
        final UUID accepted = ReviewQueueHttpIT.complete(
            context, "ACCEPTED", "inne-konto", ReviewQueueHttpIT.FIRST.minusHours(5)
        );
        ReviewQueueHttpIT.reviewer(context, mine, "konto-testowe");
        ReviewQueueHttpIT.reviewer(context, theirs, "inny-weryfikator");
        MatcherAssert.assertThat(
            "Kolejka nie jest od najstarszego, gubi własne wzięte albo ma cudze wzięte",
            ReviewQueueHttpIT.queueIds(
                context, new AsVerifier(), Set.of(older, mine, newer, theirs, accepted)
            ),
            Matchers.contains(older, mine, newer)
        );
    }

    @Test
    void excludesOwnSubmissionsIncludingBarrierRemoved(final WebApplicationContext context)
        throws Exception {
        final UUID own = ReviewQueueHttpIT.complete(
            context, "SENT", "konto-testowe", ReviewQueueHttpIT.FIRST
        );
        final UUID accepted = ReviewQueueHttpIT.complete(
            context, "ACCEPTED", "inne-konto", ReviewQueueHttpIT.FIRST.plusMinutes(5)
        );
        final UUID deletion = ReviewQueueHttpIT.removal(context, accepted);
        final UUID foreign = ReviewQueueHttpIT.complete(
            context, "SENT", "inne-konto", ReviewQueueHttpIT.FIRST.plusMinutes(20)
        );
        final Set<UUID> seeded = Set.of(own, accepted, deletion, foreign);
        MatcherAssert.assertThat(
            "Kolejka pokazuje własne zgłoszenia Weryfikatora albo ukrywa je przed innym",
            List.of(
                ReviewQueueHttpIT.queueIds(context, new AsVerifier(), seeded),
                ReviewQueueHttpIT.queueIds(
                    context, ReviewQueueHttpIT.bearer("VERIFIER.drugi-weryfikator"), seeded
                )
            ),
            Matchers.contains(List.of(foreign), List.of(own, deletion, foreign))
        );
    }

    @Test
    void excludesSubmissionsWithoutAllPhotos(final WebApplicationContext context)
        throws Exception {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final UUID complete = ReviewQueueHttpIT.store(
            context, "SENT", "inne-konto",
            ReviewQueueHttpIT.twoPhotos(first, second),
            ReviewQueueHttpIT.FIRST
        );
        ReviewQueueHttpIT.putPhoto(context, complete, first);
        ReviewQueueHttpIT.putPhoto(context, complete, second);
        final UUID third = UUID.randomUUID();
        final UUID partial = ReviewQueueHttpIT.store(
            context, "SENT", "inne-konto",
            ReviewQueueHttpIT.twoPhotos(third, UUID.randomUUID()),
            ReviewQueueHttpIT.FIRST.plusMinutes(1)
        );
        ReviewQueueHttpIT.putPhoto(context, partial, third);
        MatcherAssert.assertThat(
            "Zgłoszenie bez wszystkich zdjęć z photoIds trafiło do kolejki",
            ReviewQueueHttpIT.queueIds(
                context, new AsVerifier(),
                Set.of(
                    complete, partial,
                    ReviewQueueHttpIT.store(
                        context, "SENT", "inne-konto",
                        SampleSubmissions.quick(UUID.randomUUID()),
                        ReviewQueueHttpIT.FIRST.plusMinutes(2)
                    )
                )
            ),
            Matchers.contains(complete)
        );
    }

    @Test
    void refusesUnauthorizedAndNonVerifierRoles(final WebApplicationContext context) {
        Assertions.assertAll(
            Stream.concat(
                Stream.of(
                    () -> Assertions.assertEquals(
                        401,
                        ReviewQueueHttpIT.status(context, request -> request),
                        "Kolejka weryfikacji bez tokenu nie daje 401"
                    )
                ),
                Stream.of("VISITOR", "COLLECTOR", "COORDINATOR").map(
                    role -> () -> Assertions.assertEquals(
                        403,
                        ReviewQueueHttpIT.status(context, ReviewQueueHttpIT.bearer(role)),
                        String.format("Kolejka weryfikacji dla roli %s nie daje 403", role)
                    )
                )
            )
        );
    }

    private static UUID complete(
        final WebApplicationContext context, final String status, final String author,
        final OffsetDateTime created
    ) throws IOException {
        final UUID photo = UUID.randomUUID();
        final UUID id = ReviewQueueHttpIT.store(
            context, status, author, SampleSubmissions.quick(photo), created
        );
        ReviewQueueHttpIT.putPhoto(context, id, photo);
        return id;
    }

    private static UUID removal(final WebApplicationContext context, final UUID replaced)
        throws IOException {
        final UUID photo = UUID.randomUUID();
        final UUID id = ReviewQueueHttpIT.store(
            context, "SENT", "konto-testowe",
            new SubmissionContent(
                SubmissionKind.PRECISE,
                new SubmissionPlace(
                    new SubmissionPoint(17.05, 51.108), null, false, "A-1",
                    new SubmissionFloor("A-1-0", 0, false)
                ),
                new BarrierDetails(null, new BarrierType(9), null, null, true),
                null, null, replaced, List.of(photo)
            ),
            ReviewQueueHttpIT.FIRST.plusMinutes(10)
        );
        ReviewQueueHttpIT.putPhoto(context, id, photo);
        return id;
    }

    private static SubmissionContent twoPhotos(final UUID first, final UUID second) {
        final SubmissionContent base = SampleSubmissions.quick(first);
        return new SubmissionContent(
            base.kind(), base.place(), base.barrier(), base.entranceId(),
            base.entranceMarking(), base.replaces(), List.of(first, second)
        );
    }

    private static UUID store(
        final WebApplicationContext context, final String status, final String author,
        final SubmissionContent content, final OffsetDateTime created
    ) {
        final UUID id = new SampleSubmissions(context).add(status, content);
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = :author, created_at = :at WHERE id = :id")
            .param("author", author).param("at", created).param("id", id).update();
        return id;
    }

    private static void putPhoto(
        final WebApplicationContext context, final UUID submission, final UUID photo
    ) throws IOException {
        context.getBean(PhotoStore.class).put(submission, photo, JpegCleanerTest.photo());
    }

    private static void reviewer(
        final WebApplicationContext context, final UUID id, final String account
    ) {
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET reviewer = :reviewer WHERE id = :id")
            .param("reviewer", account).param("id", id).update();
    }

    private static List<UUID> queueIds(
        final WebApplicationContext context, final RequestPostProcessor identity,
        final Set<UUID> seeded
    ) throws Exception {
        final MockHttpServletResponse response = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get(ReviewQueueHttpIT.QUEUE).with(identity)
        ).andReturn().getResponse();
        MatcherAssert.assertThat(
            "Kolejka weryfikacji nie daje 200", response.getStatus(), Matchers.equalTo(200)
        );
        return StreamSupport.stream(
            context.getBean(ObjectMapper.class).readTree(
                response.getContentAsString(StandardCharsets.UTF_8)
            ).spliterator(), false
        )
            .map(item -> UUID.fromString(item.path("id").asString()))
            .filter(seeded::contains)
            .toList();
    }

    private static int status(
        final WebApplicationContext context, final RequestPostProcessor identity
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get(ReviewQueueHttpIT.QUEUE).with(identity)
        )
            .andReturn().getResponse().getStatus();
    }

    private static RequestPostProcessor bearer(final String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", token));
            return request;
        };
    }
}
