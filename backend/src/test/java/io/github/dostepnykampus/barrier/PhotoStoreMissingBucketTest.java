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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Test I2b-4 (ADR-0009): zapis do nieistniejącego kubełka kończy się błędem
 * magazynu, a nie cichym utworzeniem kubełka w żądaniu.
 *
 * @since 0.1.0
 */
final class PhotoStoreMissingBucketTest {

    @Test
    void iTwoBFourDoesNotCreateBucketOnPut() {
        final PhotoStoreMissingBucketTest.NoBucket client =
            new PhotoStoreMissingBucketTest.NoBucket();
        final PhotoStore store = new PhotoStore(
            client, new PhotoSettings(URI.create("http://magazyn"), "brak-kubelka", "", "")
        );
        final UUID photo = UUID.randomUUID();
        Assertions.assertAll(
            () -> Assertions.assertThrows(
                NoSuchBucketException.class,
                () -> store.put(photo, photo, new byte[] {1}),
                "Zapis do nieistniejącego kubełka nie rzuca błędu magazynu"
            ),
            () -> Assertions.assertEquals(
                List.of(), client.created, "Zapis zdjęcia utworzył kubełek w żądaniu"
            )
        );
    }

    /**
     * Klient S3 bez kubełka; zapamiętuje próby jego utworzenia.
     *
     * @since 0.1.0
     */
    private static final class NoBucket implements S3Client {

        /**
         * Kubełki kolejnych utworzeń.
         */
        private final List<String> created = new ArrayList<>(1);

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.created.clear();
        }

        @Override
        public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody body) {
            throw NoSuchBucketException.builder().message(request.bucket()).build();
        }

        @Override
        public CreateBucketResponse createBucket(final CreateBucketRequest request) {
            this.created.add(request.bucket());
            return CreateBucketResponse.builder().build();
        }
    }
}
