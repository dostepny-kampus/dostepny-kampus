/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Magazyn zdjęć tworzy i czyta kubełek z ustawień {@code dk.photos}
 * (ADR-0009).
 *
 * @since 0.1.0
 */
final class PhotoStoreBucketTest {

    @Test
    void cfgTwoCreatesAndReadsBucketFromSettings() {
        final UUID photo = UUID.randomUUID();
        try (PhotoStoreBucketTest.BucketProbe probe = new PhotoStoreBucketTest.BucketProbe()) {
            final PhotoStore store = new PhotoStore(
                probe, new PhotoSettings(URI.create("http://magazyn"), "zdjecia-testowe", "", "")
            );
            store.put(photo, photo, new byte[] {1});
            store.get(photo, photo);
            Assertions.assertAll(
                () -> Assertions.assertEquals(
                    List.of("zdjecia-testowe"), probe.created,
                    "Magazyn utworzył kubełek inny niż z DK_PHOTOS_BUCKET"
                ),
                () -> Assertions.assertEquals(
                    List.of("zdjecia-testowe"), probe.read,
                    "Magazyn czytał zdjęcie z kubełka innego niż z DK_PHOTOS_BUCKET"
                )
            );
        }
    }

    /**
     * Klient S3 bez kubełka, dopóki go nie utworzono; zapamiętuje kubełki
     * tworzenia i odczytu zamiast je wykonywać.
     *
     * @since 0.1.0
     */
    private static final class BucketProbe implements S3Client {

        /**
         * Kubełki kolejnych utworzeń.
         */
        private final List<String> created = new ArrayList<>(1);

        /**
         * Kubełki kolejnych odczytów.
         */
        private final List<String> read = new ArrayList<>(1);

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.created.clear();
            this.read.clear();
        }

        @Override
        public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody body) {
            if (this.created.isEmpty()) {
                throw NoSuchBucketException.builder().message(request.bucket()).build();
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public CreateBucketResponse createBucket(final CreateBucketRequest request) {
            this.created.add(request.bucket());
            return CreateBucketResponse.builder().build();
        }

        @Override
        public ResponseBytes<GetObjectResponse> getObjectAsBytes(final GetObjectRequest request) {
            this.read.add(request.bucket());
            return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {1});
        }
    }
}
