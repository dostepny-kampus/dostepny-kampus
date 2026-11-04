/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Tworzy kubełek zdjęć przy starcie (ADR-0009); gdy się nie uda albo kubełek
 * już jest, serwer startuje, a powód trafia do dziennika.
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
        } catch (final SdkException ex) {
            PhotoBucket.LOG.warn("Nie utworzono kubełka zdjęć: {}", ex.getMessage());
        }
    }
}
