/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Połączenie z magazynem zdjęć zgodnym z S3, lokalnie RustFS (ADR-0009).
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public final class PhotoStorage {

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private PhotoStorage() {
    }

    /**
     * Tworzy klienta S3 z adresem i kluczami z ustawień {@code dk.photos}.
     * Klucze są czytane przy pierwszym żądaniu, więc serwer startuje także
     * bez skonfigurowanego magazynu.
     *
     * @param endpoint Adres S3 magazynu, np. http://127.0.0.1:9000
     * @param access Klucz dostępu
     * @param secret Sekret
     * @return Klient S3 z adresami w ścieżce, bez nazw kubełków w domenie
     */
    @Bean
    static S3Client photoClient(
        @Value("${dk.photos.endpoint}") final String endpoint,
        @Value("${dk.photos.access-key}") final String access,
        @Value("${dk.photos.secret-key}") final String secret
    ) {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(() -> AwsBasicCredentials.create(access, secret))
            .build();
    }
}
