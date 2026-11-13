/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.ForbiddenChange;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Zapis zdjęcia i przyjęcie zgłoszenia nie mogą zmienić tego samego zgłoszenia równocześnie.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SubmissionPhotoRaceTest {

    @Test
    void acceptedDuringBodyReadKeepsPhoto(final ApplicationContext context) throws Exception {
        final UUID photo = UUID.randomUUID();
        final UUID id = new SampleSubmissions(context).add("SENT", SampleSubmissions.quick(photo));
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final PhotoStore store = context.getBean(PhotoStore.class);
        final byte[] original = {1, 2, 3};
        store.put(id, photo, original);
        try (
            ByteArrayInputStream input = new SubmissionPhotoRaceTest.AcceptedBody(
                jdbc, id, JpegCleanerTest.photo()
            )
        ) {
            Assertions.assertThrows(
                ForbiddenChange.class,
                () -> context.getBean(SubmissionPhotos.class).put(
                    id, photo, new PhotoUpload(input, -1)
                ),
                "Przyjęcie w trakcie odczytu ciała musi zabronić zastąpienia zdjęcia"
            );
            Assertions.assertArrayEquals(
                original, store.get(id, photo).orElseThrow(),
                "Zdjęcie przyjętego zgłoszenia zmieniło się"
            );
        } finally {
            SubmissionPhotoRaceTest.clean(context, id, photo);
        }
    }

    @Test
    void acceptanceWaitsForObjectWrite(final ApplicationContext context) throws Exception {
        final UUID photo = UUID.randomUUID();
        final UUID id = new SampleSubmissions(context).add("SENT", SampleSubmissions.quick(photo));
        final byte[] jpeg = JpegCleanerTest.photo();
        try (
            S3Client client = new SubmissionPhotoRaceTest.AcceptanceAttempt(
                PhotoStorage.photoClient(context.getBean(PhotoSettings.class)),
                context.getBean(DataSource.class), id
            );
            ByteArrayInputStream input = new ByteArrayInputStream(jpeg)
        ) {
            final SubmissionPhotos service = new SubmissionPhotos(
                context.getBean(SubmissionFinder.class),
                new PhotoStore(client, context.getBean(PhotoSettings.class)),
                context.getBean(SubmissionService.class)
            );
            service.put(id, photo, new PhotoUpload(input, jpeg.length));
            SubmissionPhotoRaceTest.accept(context.getBean(JdbcClient.class), id);
            Assertions.assertArrayEquals(
                JpegCleaner.strip(jpeg),
                context.getBean(PhotoStore.class).get(id, photo).orElseThrow(),
                "Przyjęte zgłoszenie nie zachowało zdjęcia zapisanego przed przyjęciem"
            );
        } finally {
            SubmissionPhotoRaceTest.clean(context, id, photo);
        }
    }

    private static void accept(final JdbcClient jdbc, final UUID id) {
        jdbc.sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", id).update();
    }

    private static void clean(final ApplicationContext context, final UUID id, final UUID photo) {
        context.getBean(PhotoStore.class).delete(id, photo);
        context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id = :id")
            .param("id", id).update();
    }

    /** Odczyt ciała przyjmuje zgłoszenie przed zwróceniem bajtów. */
    private static final class AcceptedBody extends ByteArrayInputStream {

        /** Dostęp do statusu zgłoszenia. */
        private final JdbcClient jdbc;

        /** Identyfikator zgłoszenia. */
        private final UUID id;

        AcceptedBody(final JdbcClient jdbc, final UUID id, final byte[] body) {
            super(body);
            this.jdbc = jdbc;
            this.id = id;
        }

        @Override
        public byte[] readNBytes(final int length) throws IOException {
            SubmissionPhotoRaceTest.accept(this.jdbc, this.id);
            return super.readNBytes(length);
        }
    }

    /**
     * Próbuje przyjąć zgłoszenie na osobnym połączeniu tuż przed zapisem obiektu do S3.
     *
     * @param client Własny klient magazynu zamykany razem z adapterem
     * @param source Dostęp do osobnego połączenia z bazą
     * @param id Identyfikator zgłoszenia
     * @since 0.1.0
     */
    private record AcceptanceAttempt(
        S3Client client, DataSource source, UUID id
    ) implements S3Client {
        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.client.close();
        }

        @Override
        public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody body) {
            try (
                Connection connection = this.source.getConnection();
                Statement statement = connection.createStatement()
            ) {
                connection.setAutoCommit(false);
                statement.execute("SET LOCAL lock_timeout = '100ms'");
                Assertions.assertEquals(
                    "55P03",
                    Assertions.assertThrows(
                        SQLException.class,
                        () -> statement.executeUpdate(
                            "UPDATE submissions SET status = 'ACCEPTED' WHERE id = '%s'"
                                .formatted(this.id)
                        ),
                        "Przyjęcie zgłoszenia wyprzedziło zapis zdjęcia do S3"
                    ).getSQLState(),
                    "Odmowa nie wynikała z blokady wiersza"
                );
                connection.rollback();
                return this.client.putObject(request, body);
            } catch (final SQLException ex) {
                throw new IllegalStateException("Nie udało się sprawdzić blokady zgłoszenia", ex);
            }
        }
    }
}
