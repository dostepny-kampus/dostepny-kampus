/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.storage;

import io.github.dostepnykampus.barrier.photo.PhotoStorageException;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
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
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Zdjęcie leży w magazynie pod kluczem {@code <zgłoszenie>/<zdjęcie>.jpg}, a lista
 * zdjęć zgłoszenia odczytuje identyfikatory z tych kluczy. Zapis do nieistniejącego kubełka
 * kończy się błędem magazynu, a nie cichym utworzeniem kubełka w żądaniu (ADR-0009).
 *
 * <p>Wymagania: BR-7, BR-8, US2.2</p>
 *
 * @since 0.1.0
 */
final class S3PhotoStoreTest {

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
        final S3PhotoStoreTest.Bucket client = new S3PhotoStoreTest.Bucket();
        S3PhotoStoreTest.store(client).put(
            S3PhotoStoreTest.SUBMISSION, S3PhotoStoreTest.PHOTO, new byte[] {1}
        );
        Assertions.assertEquals(
            List.of(
                String.format("%s/%s.jpg", S3PhotoStoreTest.SUBMISSION, S3PhotoStoreTest.PHOTO)
            ),
            client.keys,
            "Zdjęcie trafia do kubełka pod kluczem zgłoszenia"
        );
    }

    @Test
    void listsPhotosFromKeys() {
        final S3PhotoStoreTest.Bucket client = new S3PhotoStoreTest.Bucket();
        client.keys.add(
            String.format("%s/%s.jpg", S3PhotoStoreTest.SUBMISSION, S3PhotoStoreTest.PHOTO)
        );
        Assertions.assertEquals(
            List.of(S3PhotoStoreTest.PHOTO),
            S3PhotoStoreTest.store(client).stored(S3PhotoStoreTest.SUBMISSION),
            "Lista zdjęć zgłoszenia wynika z kluczy w kubełku"
        );
    }

    @Test
    void doesNotCreateBucketOnPut() {
        final S3PhotoStoreTest.NoBucket client =
            new S3PhotoStoreTest.NoBucket();
        final PhotoStore store = new S3PhotoStore(
            client, new PhotoSettings(URI.create("http://magazyn"), "brak-kubelka", "", "")
        );
        final UUID photo = UUID.randomUUID();
        Assertions.assertAll(
            () -> Assertions.assertInstanceOf(
                NoSuchBucketException.class,
                Assertions.assertThrows(
                    PhotoStorageException.class,
                    () -> store.put(photo, photo, new byte[] {1}),
                    "Zapis do nieistniejącego kubełka nie rzuca błędu magazynu zdjęć"
                ).getCause(),
                "Błąd magazynu zdjęć nie wskazuje błędu S3 jako przyczyny"
            ),
            () -> Assertions.assertEquals(
                List.of(), client.created, "Zapis zdjęcia utworzył kubełek w żądaniu"
            )
        );
    }

    @Test
    void createsAndReadsBucketFromSettings() {
        final UUID photo = UUID.randomUUID();
        try (S3PhotoStoreTest.BucketProbe probe = new S3PhotoStoreTest.BucketProbe()) {
            final PhotoSettings settings = new PhotoSettings(
                URI.create("http://magazyn"), "zdjecia-testowe", "k", "k"
            );
            final PhotoStore store = new S3PhotoStore(probe, settings);
            new PhotoBucket(probe, settings).run(null);
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

    private static PhotoStore store(final S3Client client) {
        return new S3PhotoStore(
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
