/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Magazyn zdjęć w S3: jeden obiekt na zdjęcie, pod kluczem z identyfikatorem zgłoszenia
 * i zdjęcia; błędy klienta S3 zamienia na {@link PhotoStorageException}.
 *
 * <p>Wymagania: BR-9</p>
 *
 * @since 0.1.0
 */
@Service
public final class S3PhotoStore implements PhotoStore {

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

    /**
     * Tworzy magazyn zdjęć.
     *
     * @param client Klient S3
     * @param settings Ustawienia magazynu z kubełkiem ze zdjęciami
     */
    public S3PhotoStore(final S3Client client, final PhotoSettings settings) {
        this.client = client;
        this.settings = settings;
    }

    @Override
    public void put(final UUID submission, final UUID photo, final byte[] jpeg) {
        try {
            this.client.putObject(
                request -> request.bucket(this.settings.bucket())
                    .key(S3PhotoStore.key(submission, photo))
                    .contentType("image/jpeg"),
                RequestBody.fromBytes(jpeg)
            );
        } catch (final SdkException ex) {
            throw new PhotoStorageException("Nie zapisano zdjęcia", ex);
        }
    }

    @Override
    public Optional<byte[]> get(final UUID submission, final UUID photo) {
        Optional<byte[]> found;
        try {
            found = Optional.of(
                this.client.getObjectAsBytes(
                    request -> request.bucket(this.settings.bucket())
                        .key(S3PhotoStore.key(submission, photo))
                ).asByteArray()
            );
        } catch (final NoSuchKeyException ex) {
            found = Optional.empty();
        } catch (final SdkException ex) {
            throw new PhotoStorageException("Nie odczytano zdjęcia", ex);
        }
        return found;
    }

    @Override
    public List<UUID> stored(final UUID submission) {
        try {
            return this.client.listObjectsV2Paginator(
                request -> request.bucket(this.settings.bucket())
                    .prefix(String.format("%s/", submission))
            ).contents().stream()
                .map(S3Object::key)
                .map(S3PhotoStore::photo)
                .toList();
        } catch (final SdkException ex) {
            throw new PhotoStorageException("Nie odczytano listy zdjęć", ex);
        }
    }

    @Override
    public void delete(final UUID submission, final UUID photo) {
        try {
            this.client.deleteObject(
                request -> request.bucket(this.settings.bucket())
                    .key(S3PhotoStore.key(submission, photo))
            );
        } catch (final SdkException ex) {
            throw new PhotoStorageException("Nie usunięto zdjęcia", ex);
        }
    }

    private static String key(final UUID submission, final UUID photo) {
        return String.format("%s/%s%s", submission, photo, S3PhotoStore.JPEG);
    }

    private static UUID photo(final String key) {
        return UUID.fromString(
            key.substring(key.indexOf('/') + 1, key.length() - S3PhotoStore.JPEG.length())
        );
    }
}
