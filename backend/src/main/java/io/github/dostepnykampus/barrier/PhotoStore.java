/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.Optional;
import java.util.UUID;
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
     * Ustawienia magazynu z kubełkiem ze zdjęciami.
     */
    private final PhotoSettings settings;

    PhotoStore(final S3Client client, final PhotoSettings settings) {
        this.client = client;
        this.settings = settings;
    }

    /**
     * Zapisuje zdjęcie; ponowny zapis tego samego zdjęcia je zastępuje.
     * Kubełek tworzy {@code PhotoBucket} przy starcie; brak kubełka to błąd magazynu.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     * @param jpeg Plik JPEG
     */
    void put(final UUID submission, final UUID photo, final byte[] jpeg) {
        this.client.putObject(
            request -> request.bucket(this.settings.bucket())
                .key(PhotoStore.key(submission, photo))
                .contentType("image/jpeg"),
            RequestBody.fromBytes(jpeg)
        );
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
                    request -> request.bucket(this.settings.bucket())
                        .key(PhotoStore.key(submission, photo))
                ).asByteArray()
            );
        } catch (final NoSuchKeyException | NoSuchBucketException ex) {
            found = Optional.empty();
        }
        return found;
    }

    private static String key(final UUID submission, final UUID photo) {
        return String.format("%s/%s.jpg", submission, photo);
    }
}
