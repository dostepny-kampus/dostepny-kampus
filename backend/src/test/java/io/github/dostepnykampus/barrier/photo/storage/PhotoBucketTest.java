/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Test I2b-4 (ADR-0009): start serwera tworzy kubełek zdjęć; istniejący kubełek
 * to sukces, awaria magazynu przy skonfigurowanych kluczach zatrzymuje start,
 * a brak kluczy tylko ostrzega.
 *
 * @since 0.1.0
 */
final class PhotoBucketTest {

    @Test
    void skipsCreationWhenBucketExists() {
        final PhotoBucketTest.Storage client = new PhotoBucketTest.Storage(true, null);
        new PhotoBucket(client, PhotoBucketTest.settings("klucz")).run(null);
        Assertions.assertEquals(
            List.of(), client.created,
            "Serwer tworzył istniejący kubełek, do czego klucz serwera nie ma prawa"
        );
    }

    @Test
    void treatsBucketCreatedMeanwhileAsSuccess() {
        final PhotoBucketTest.Storage client = new PhotoBucketTest.Storage(
            false, BucketAlreadyOwnedByYouException.builder().message("jest").build()
        );
        Assertions.assertDoesNotThrow(
            () -> new PhotoBucket(client, PhotoBucketTest.settings("klucz")).run(null),
            "Istniejący kubełek zatrzymał start serwera"
        );
    }

    @Test
    void stopsStartupWhenStorageFailsWithKeys() {
        MatcherAssert.assertThat(
            "Komunikat nie wskazuje ustawienia dk.photos i kubełka",
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> new PhotoBucket(
                    new PhotoBucketTest.Storage(
                        false, SdkClientException.create("brak połączenia")
                    ),
                    PhotoBucketTest.settings("klucz")
                ).run(null),
                "Awaria magazynu przy skonfigurowanych kluczach nie zatrzymała startu"
            ).getMessage(), Matchers.allOf(
                Matchers.containsString("dk.photos"), Matchers.containsString("zdjecia-start")
            )
        );
    }

    @Test
    void startsWithoutKeysAndCreatesNothing() {
        final PhotoBucketTest.Storage client = new PhotoBucketTest.Storage(false, null);
        new PhotoBucket(client, PhotoBucketTest.settings("")).run(null);
        Assertions.assertEquals(
            List.of(), client.created, "Bez kluczy serwer próbował utworzyć kubełek"
        );
    }

    private static PhotoSettings settings(final String key) {
        return new PhotoSettings(URI.create("http://magazyn"), "zdjecia-start", key, key);
    }

    /**
     * Klient S3 z kubełkiem albo bez; zapamiętuje utworzone kubełki i rzuca zadany błąd.
     *
     * @since 0.1.0
     */
    private static final class Storage implements S3Client {

        /**
         * Kubełki kolejnych utworzeń.
         */
        private final List<String> created;

        /**
         * Czy kubełek już istnieje.
         */
        private final boolean exists;

        /**
         * Błąd rzucany przy tworzeniu albo null.
         */
        private final @Nullable RuntimeException failure;

        Storage(final boolean exists, final @Nullable RuntimeException failure) {
            this.created = new ArrayList<>(1);
            this.exists = exists;
            this.failure = failure;
        }

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.created.clear();
        }

        @Override
        public HeadBucketResponse headBucket(final HeadBucketRequest request) {
            if (!this.exists) {
                throw NoSuchBucketException.builder().message(request.bucket()).build();
            }
            return HeadBucketResponse.builder().build();
        }

        @Override
        public CreateBucketResponse createBucket(final CreateBucketRequest request) {
            this.created.add(request.bucket());
            if (this.failure != null) {
                throw this.failure;
            }
            return CreateBucketResponse.builder().build();
        }
    }
}
