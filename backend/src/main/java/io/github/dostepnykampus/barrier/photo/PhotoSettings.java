/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ustawienia magazynu zdjęć {@code dk.photos} (ADR-0009).
 *
 * <p>Puste klucze są dozwolone; tekst ustawień ich nie zawiera.</p>
 *
 * <p>Wymagania: NFR-35</p>
 *
 * @param endpoint Adres S3 magazynu, np. http://127.0.0.1:9000
 * @param bucket Kubełek ze zdjęciami
 * @param accessKey Klucz dostępu
 * @param secretKey Sekret
 * @since 0.1.0
 */
@ConfigurationProperties("dk.photos")
public record PhotoSettings(URI endpoint, String bucket, String accessKey, String secretKey) {

    /**
     * Sprawdza adres i kubełek magazynu.
     *
     * @throws IllegalArgumentException Gdy adres nie ma schematu albo hosta
     *  lub kubełek jest pusty; komunikat zaczyna się od nazwy ustawienia
     */
    public PhotoSettings {
        if (endpoint == null || endpoint.getScheme() == null || endpoint.getHost() == null) {
            throw new IllegalArgumentException(
                String.format("dk.photos.endpoint: adres bez schematu lub hosta: %s", endpoint)
            );
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("dk.photos.bucket: pusty kubełek");
        }
    }

    @Override
    public String toString() {
        return String.format("PhotoSettings[endpoint=%s, bucket=%s]", this.endpoint, this.bucket);
    }
}
