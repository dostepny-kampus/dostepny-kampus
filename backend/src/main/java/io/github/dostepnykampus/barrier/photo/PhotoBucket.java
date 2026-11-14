/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

/**
 * Tworzy kubełek zdjęć przy starcie serwera (ADR-0009), więc zapis zdjęcia
 * nie tworzy go w żądaniu.
 *
 * <p>Istniejący kubełek to sukces. Przy skonfigurowanych kluczach każda inna
 * awaria magazynu zatrzymuje start. Bez kluczy (np. w testach bez magazynu)
 * serwer tylko ostrzega i startuje.</p>
 *
 * @since 0.1.0
 */
@Component
final class PhotoBucket implements ApplicationRunner {

    /** Dziennik serwera. */
    private static final Logger LOG = LoggerFactory.getLogger(PhotoBucket.class);

    /** Klient magazynu zdjęć. */
    private final S3Client client;

    /** Ustawienia magazynu zdjęć. */
    private final PhotoSettings settings;

    PhotoBucket(final S3Client client, final PhotoSettings settings) {
        this.client = client;
        this.settings = settings;
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (this.settings.accessKey().isBlank()) {
            PhotoBucket.LOG.warn("Brak dk.photos.access-key, kubełek zdjęć nie został utworzony");
            return;
        }
        try {
            this.client.createBucket(request -> request.bucket(this.settings.bucket()));
        } catch (final BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ex) {
            PhotoBucket.LOG.debug("Kubełek zdjęć już istnieje: {}", this.settings.bucket());
        } catch (final SdkException ex) {
            throw new IllegalStateException(
                String.format(
                    "dk.photos: nie utworzono kubełka %s w %s",
                    this.settings.bucket(), this.settings.endpoint()
                ),
                ex
            );
        }
    }
}
