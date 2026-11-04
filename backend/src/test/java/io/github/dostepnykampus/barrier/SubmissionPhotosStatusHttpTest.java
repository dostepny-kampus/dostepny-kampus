/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy I2b-1 i I2b-3 (BR-3, BR-6, BR-18): zdjęcia zmienia autor tylko
 * w statusie edytowalnym, a czyta Gość tylko w statusie publicznym; zdjęcie
 * spoza listy photoIds nie istnieje, a identyfikatory porównuje się jako UUID.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Import(RustfsTestConfiguration.class)
@Transactional
final class SubmissionPhotosStatusHttpTest {

    /**
     * Zdjęcie z listy photoIds zgłoszeń testowych.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Oczekiwany kod PUT dla każdego statusu; nowy status wymaga wpisu.
     */
    private static final Map<SubmissionStatus, Integer> WRITE = Map.of(
        SubmissionStatus.SENT, 204, SubmissionStatus.IN_REVIEW, 204,
        SubmissionStatus.REJECTED, 204, SubmissionStatus.ACCEPTED, 403,
        SubmissionStatus.ARCHIVED, 403
    );

    /**
     * Oczekiwany kod GET dla każdego statusu; nowy status wymaga wpisu.
     */
    private static final Map<SubmissionStatus, Integer> READ = Map.of(
        SubmissionStatus.SENT, 404, SubmissionStatus.IN_REVIEW, 404,
        SubmissionStatus.REJECTED, 404, SubmissionStatus.ACCEPTED, 200,
        SubmissionStatus.ARCHIVED, 404
    );

    @ParameterizedTest
    @EnumSource(SubmissionStatus.class)
    void iTwoBOneWritesPhotoOnlyInEditableStatus(
        final SubmissionStatus status, final WebApplicationContext context
    ) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        MatcherAssert.assertThat(
            "Zapis zdjęcia w statusie ".concat(status.name()).concat(" daje zły kod"),
            requests.put(
                requests.add(status.name(), SubmissionPhotosStatusHttpTest.PHOTO.toString()),
                SubmissionPhotosStatusHttpTest.PHOTO,
                JpegCleanerTest.photo()
            ).getStatus(),
            Matchers.equalTo(SubmissionPhotosStatusHttpTest.WRITE.get(status))
        );
    }

    @ParameterizedTest
    @EnumSource(SubmissionStatus.class)
    void iTwoBThreeReadsPhotoOnlyInPublicStatus(
        final SubmissionStatus status, final WebApplicationContext context
    ) throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final UUID id = requests.add("SENT", SubmissionPhotosStatusHttpTest.PHOTO.toString());
        requests.put(id, SubmissionPhotosStatusHttpTest.PHOTO, JpegCleanerTest.photo());
        requests.status(id, status.name());
        MatcherAssert.assertThat(
            "Odczyt zdjęcia w statusie ".concat(status.name()).concat(" daje zły kod"),
            requests.get(id, SubmissionPhotosStatusHttpTest.PHOTO.toString()).getStatus(),
            Matchers.equalTo(SubmissionPhotosStatusHttpTest.READ.get(status))
        );
    }

    @Test
    void iTwoBThreeHidesPhotoOutsideList(final WebApplicationContext context)
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
    void iTwoBThreeComparesPhotoIdsAsUuid(final WebApplicationContext context)
        throws Exception {
        final PhotoRequests requests = new PhotoRequests(context);
        final UUID id = requests.add(
            "SENT", SubmissionPhotosStatusHttpTest.PHOTO.toString().toUpperCase(Locale.ROOT)
        );
        final int saved = requests.put(
            id, SubmissionPhotosStatusHttpTest.PHOTO, JpegCleanerTest.photo()
        ).getStatus();
        requests.status(id, "ACCEPTED");
        MatcherAssert.assertThat(
            "Wielkość liter w identyfikatorze zdjęcia zmienia wynik zapisu albo odczytu",
            List.of(
                saved,
                requests.get(id, SubmissionPhotosStatusHttpTest.PHOTO.toString()).getStatus(),
                requests.get(
                    id,
                    SubmissionPhotosStatusHttpTest.PHOTO.toString().toUpperCase(Locale.ROOT)
                ).getStatus()
            ),
            Matchers.equalTo(List.of(204, 200, 200))
        );
    }
}
