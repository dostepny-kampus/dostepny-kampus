/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Zdjęcie leży w magazynie pod kluczem {@code <zgłoszenie>/<zdjęcie>.jpg}, a lista
 * zdjęć zgłoszenia odczytuje identyfikatory z tych kluczy.
 *
 * <p>Wymagania: BR-7, BR-8, US2.2</p>
 *
 * @since 0.1.0
 */
final class PhotoStoreKeysTest {

    /**
     * Zgłoszenie ze zdjęciami.
     */
    private static final UUID SUBMISSION =
        UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Zdjęcie zgłoszenia.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void putsPhotoUnderSubmissionKey() {
        final PhotoStoreKeysTest.Bucket client = new PhotoStoreKeysTest.Bucket();
        PhotoStoreKeysTest.store(client).put(
            PhotoStoreKeysTest.SUBMISSION, PhotoStoreKeysTest.PHOTO, new byte[] {1}
        );
        Assertions.assertEquals(
            List.of(
                String.format("%s/%s.jpg", PhotoStoreKeysTest.SUBMISSION, PhotoStoreKeysTest.PHOTO)
            ),
            client.keys
        );
    }

    @Test
    void listsPhotosFromKeys() {
        final PhotoStoreKeysTest.Bucket client = new PhotoStoreKeysTest.Bucket();
        client.keys.add(
            String.format("%s/%s.jpg", PhotoStoreKeysTest.SUBMISSION, PhotoStoreKeysTest.PHOTO)
        );
        Assertions.assertEquals(
            List.of(PhotoStoreKeysTest.PHOTO),
            PhotoStoreKeysTest.store(client).stored(PhotoStoreKeysTest.SUBMISSION)
        );
    }

    private static PhotoStore store(final S3Client client) {
        return new PhotoStore(
            client, new PhotoSettings(URI.create("http://magazyn"), "zdjecia", "", "")
        );
    }

    /**
     * Klient S3 z jednym kubełkiem: zapamiętuje klucze zapisów i zwraca je w liście.
     *
     * @since 0.1.0
     */
    private static final class Bucket implements S3Client {

        /**
         * Klucze obiektów w kubełku.
         */
        private final List<String> keys = new ArrayList<>(1);

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.keys.clear();
        }

        @Override
        public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody body) {
            this.keys.add(request.key());
            return PutObjectResponse.builder().build();
        }

        @Override
        public ListObjectsV2Response listObjectsV2(final ListObjectsV2Request request) {
            return ListObjectsV2Response.builder()
                .contents(this.objects(request.prefix())).isTruncated(false).build();
        }

        private List<S3Object> objects(final String prefix) {
            return this.keys.stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> S3Object.builder().key(key).build())
                .toList();
        }
    }
}
