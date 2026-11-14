/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.application;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.BarrierType;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import io.github.dostepnykampus.barrier.photo.PhotoSettings;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.barrier.photo.S3PhotoStore;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Po zapisanej edycji zdjęcia spoza nowej listy photoIds znikają
 * z magazynu RustFS, a błąd magazynu nie zmienia wyniku edycji.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SubmissionPhotosPruneTest {

    @Test
    void brEightRemovesPhotoDroppedByEdit(final ApplicationContext context) {
        final PhotoStore store = context.getBean(PhotoStore.class);
        final UUID submission = UUID.randomUUID();
        final UUID kept = UUID.randomUUID();
        store.put(submission, kept, new byte[] {1});
        store.put(submission, UUID.randomUUID(), new byte[] {2});
        context.getBean(SubmissionPhotos.class)
            .prune(SubmissionPhotosPruneTest.saved(submission, kept));
        MatcherAssert.assertThat(
            "Zdjęcie spoza nowej listy photoIds zostało w magazynie",
            store.stored(submission),
            Matchers.contains(kept)
        );
    }

    @Test
    void brEightRemovesPhotoAfterCommittedEdit(final ApplicationContext context) {
        final SubmissionService service = context.getBean(SubmissionService.class);
        final PhotoStore store = context.getBean(PhotoStore.class);
        final UUID submission = UUID.randomUUID();
        final UUID kept = UUID.randomUUID();
        final UUID dropped = UUID.randomUUID();
        service.create(
            new NewSubmission(
                submission, SubmissionPhotosPruneTest.content(kept, dropped), "konto"
            )
        );
        store.put(submission, kept, new byte[] {1});
        store.put(submission, dropped, new byte[] {2});
        service.update(
            new SubmissionEdit(submission, 1, SubmissionPhotosPruneTest.content(kept))
        );
        final List<UUID> left = store.stored(submission);
        context.getBean(JdbcClient.class)
            .sql("DELETE FROM submissions WHERE id = :id")
            .param("id", submission)
            .update();
        MatcherAssert.assertThat(
            "Zatwierdzona edycja nie usunęła zdjęcia spoza nowej listy photoIds",
            left,
            Matchers.contains(kept)
        );
    }

    @Test
    void brEightKeepsEditWhenStoreFails(final ApplicationContext context) {
        final SubmissionPhotos photos = new SubmissionPhotos(
            context.getBean(SubmissionFinder.class),
            new S3PhotoStore(
                new SubmissionPhotosPruneTest.NoBucket(),
                new PhotoSettings(URI.create("http://magazyn"), "brak-kubelka", "", "")
            ),
            context.getBean(SubmissionService.class)
        );
        final UUID submission = UUID.randomUUID();
        Assertions.assertDoesNotThrow(
            () -> photos.prune(SubmissionPhotosPruneTest.saved(submission, submission)),
            "Błąd magazynu przy usuwaniu zdjęć wyszedł poza zapisaną edycję"
        );
    }

    private static EditResult.Saved saved(final UUID submission, final UUID photo) {
        return new EditResult.Saved(
            new Submission(
                submission, 2, SubmissionStatus.SENT, Instant.EPOCH,
                SubmissionPhotosPruneTest.content(photo)
            )
        );
    }

    private static SubmissionContent content(final UUID... photos) {
        return new SubmissionContent(
            SubmissionKind.PRECISE,
            new SubmissionPlace(
                new SubmissionPoint(17.062_09, 51.107_85), 4.5, true, "A-1",
                new SubmissionFloor("p2", 2, false)
            ),
            new BarrierDetails(BarrierGroup.OTHER, new BarrierType(5), 2, "opis", false),
            null, null, null, List.of(photos)
        );
    }

    /**
     * Klient S3 bez kubełka: lista zdjęć kończy się błędem magazynu.
     *
     * @since 0.1.0
     */
    private static final class NoBucket implements S3Client {

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            // Klient bez połączenia nie ma czego zamykać.
        }

        @Override
        public ListObjectsV2Response listObjectsV2(final ListObjectsV2Request request) {
            throw NoSuchBucketException.builder().message(request.bucket()).build();
        }
    }
}
