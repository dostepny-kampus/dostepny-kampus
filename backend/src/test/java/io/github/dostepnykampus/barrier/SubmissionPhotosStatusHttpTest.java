/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy I2b-1 i I2b-3: zdjęcia zmienia autor tylko
 * w statusie edytowalnym, czyta Gość tylko w publicznym; zdjęcie spoza
 * photoIds nie istnieje, a identyfikatory porównuje się jako UUID.
 *
 * <p>Wymagania: BR-3, BR-6, BR-18</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionPhotosStatusHttpTest {

    /** Zdjęcie z listy photoIds. */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /** Kody PUT i GET dla każdego statusu; nowy status wymaga wpisu. */
    private static final Map<SubmissionStatus, List<Integer>> CODES = Map.of(
        SubmissionStatus.SENT, List.of(204, 404),
        SubmissionStatus.IN_REVIEW, List.of(204, 404),
        SubmissionStatus.REJECTED, List.of(204, 404),
        SubmissionStatus.ACCEPTED, List.of(403, 200),
        SubmissionStatus.ARCHIVED, List.of(403, 404)
    );

    @ParameterizedTest
    @EnumSource(SubmissionStatus.class)
    void acTwoBOneAndThreeGateByStatus(
        final SubmissionStatus status, final WebApplicationContext context
    ) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final String photo = SubmissionPhotosStatusHttpTest.PHOTO.toString();
        final UUID id = requests.add(status.name(), photo);
        final int written = requests.put(
            id, SubmissionPhotosStatusHttpTest.PHOTO, JpegCleanerTest.photo()
        ).getStatus();
        context.getBean(PhotoStore.class).put(
            id, SubmissionPhotosStatusHttpTest.PHOTO, JpegCleanerTest.photo()
        );
        MatcherAssert.assertThat(
            "Zapis albo odczyt zdjęcia w statusie ".concat(status.name()).concat(" daje zły kod"),
            List.of(written, requests.get(id, photo).getStatus()),
            Matchers.equalTo(SubmissionPhotosStatusHttpTest.CODES.get(status))
        );
    }

    @Test
    void acTwoBThreeHidesPhotoOutsideList(final WebApplicationContext context)
        throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final UUID id = requests.add(
            "ACCEPTED", SubmissionPhotosStatusHttpTest.PHOTO.toString()
        );
        final UUID other = UUID.randomUUID();
        context.getBean(PhotoStore.class).put(id, other, JpegCleanerTest.photo());
        MatcherAssert.assertThat(
            "Zdjęcie spoza listy photoIds jest widoczne mimo zapisu w magazynie",
            requests.get(id, other.toString()).getStatus(),
            Matchers.equalTo(404)
        );
    }

    @Test
    void acTwoBThreeComparesPhotoIdsAsUuid(final WebApplicationContext context)
        throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final String lower = SubmissionPhotosStatusHttpTest.PHOTO.toString();
        final String upper = lower.toUpperCase(Locale.ROOT);
        final UUID id = requests.add("ACCEPTED", upper);
        context.getBean(PhotoStore.class).put(
            id, SubmissionPhotosStatusHttpTest.PHOTO, JpegCleanerTest.photo()
        );
        MatcherAssert.assertThat(
            "Wielkość liter w identyfikatorze zdjęcia zmienia wynik odczytu",
            List.of(
                requests.get(id, lower).getStatus(), requests.get(id, upper).getStatus()
            ),
            Matchers.equalTo(List.of(200, 200))
        );
    }
}
