/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Zdjęcia zgłoszeń w magazynie S3: jeden obiekt na zdjęcie, pod kluczem
 * z identyfikatorem zgłoszenia i zdjęcia nadanymi w telefonie (BR-9).
 *
 * @since 0.1.0
 */
@Service
final class PhotoStore {

    /**
     * Rozszerzenie klucza zdjęcia w magazynie.
     */
    private static final String JPEG = ".jpg";

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
        } catch (final NoSuchKeyException ex) {
            found = Optional.empty();
        }
        return found;
    }

    /**
     * Zdjęcia zgłoszenia zapisane w magazynie, także spoza bieżącej listy
     * photoIds (#839).
     *
     * @param submission Identyfikator zgłoszenia
     * @return Identyfikatory zdjęć pod kluczami zgłoszenia
     */
    List<UUID> stored(final UUID submission) {
        return this.client.listObjectsV2Paginator(
            request -> request.bucket(this.settings.bucket())
                .prefix(String.format("%s/", submission))
        ).contents().stream()
            .map(S3Object::key)
            .map(PhotoStore::photo)
            .toList();
    }

    /**
     * Usuwa zdjęcie; brak zdjęcia nie jest błędem.
     *
     * @param submission Identyfikator zgłoszenia
     * @param photo Identyfikator zdjęcia
     */
    void delete(final UUID submission, final UUID photo) {
        this.client.deleteObject(
            request -> request.bucket(this.settings.bucket())
                .key(PhotoStore.key(submission, photo))
        );
    }

    private static String key(final UUID submission, final UUID photo) {
        return String.format("%s/%s%s", submission, photo, PhotoStore.JPEG);
    }

    private static UUID photo(final String key) {
        return UUID.fromString(
            key.substring(key.indexOf('/') + 1, key.length() - PhotoStore.JPEG.length())
        );
    }
}
