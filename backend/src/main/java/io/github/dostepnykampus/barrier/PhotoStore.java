/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Zdjęcia zgłoszeń w magazynie S3: jeden obiekt na zdjęcie, pod kluczem
 * z identyfikatorem zgłoszenia i zdjęcia nadanymi w telefonie (BR-9).
 *
 * @since 0.1.0
 */
@Service
final class PhotoStore {

    /**
     * Klient magazynu zdjęć.
     */
    private final S3Client client;

    /**
     * Kubełek ze zdjęciami.
     */
    private final String bucket;

    PhotoStore(final S3Client client, @Value("${dk.photos.bucket}") final String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    /**
     * Zapisuje zdjęcie; ponowny zapis tego samego zdjęcia je zastępuje.
     * Kubełek powstaje przy pierwszym zapisie.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @param jpeg Plik JPEG
     */
    void put(final UUID submission, final UUID photo, final byte[] jpeg) {
        try {
            this.write(submission, photo, jpeg);
        } catch (final NoSuchBucketException ex) {
            this.client.createBucket(request -> request.bucket(this.bucket));
            this.write(submission, photo, jpeg);
        }
    }

    /**
     * Czyta zdjęcie.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @return Plik JPEG; brak, gdy magazyn nie ma zdjęcia
     */
    Optional<byte[]> get(final UUID submission, final UUID photo) {
        Optional<byte[]> found;
        try {
            found = Optional.of(
                this.client.getObjectAsBytes(
                    request -> request.bucket(this.bucket).key(PhotoStore.key(submission, photo))
                ).asByteArray()
            );
        } catch (final NoSuchKeyException | NoSuchBucketException ex) {
            found = Optional.empty();
        }
        return found;
    }

    private void write(final UUID submission, final UUID photo, final byte[] jpeg) {
        this.client.putObject(
            request -> request.bucket(this.bucket)
                .key(PhotoStore.key(submission, photo))
                .contentType("image/jpeg"),
            RequestBody.fromBytes(jpeg)
        );
    }

    private static String key(final UUID submission, final UUID photo) {
        return String.format("%s/%s.jpg", submission, photo);
    }
}
