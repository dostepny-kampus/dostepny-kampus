/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Połączenie z magazynem zdjęć zgodnym z S3, lokalnie RustFS (ADR-0009).
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PhotoSettings.class)
public final class PhotoStorage {

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private PhotoStorage() {
    }

    /**
     * Tworzy klienta S3 z adresem i kluczami z ustawień {@code dk.photos}.
     * Klucze są czytane przy pierwszym żądaniu, więc serwer startuje także
     * bez skonfigurowanego magazynu. Próba wywołania trwa najwyżej 4 s, całe wywołanie
     * z ponowieniami 10 s, więc zapis zdjęcia nie trzyma połączenia z bazą bez końca.
     *
     * @param settings Ustawienia magazynu zdjęć
     * @return Klient S3 z adresami w ścieżce, bez nazw kubełków w domenie, z limitami czasu
     */
    @Bean
    static S3Client photoClient(final PhotoSettings settings) {
        return S3Client.builder()
            .endpointOverride(settings.endpoint())
            .region(Region.US_EAST_1).overrideConfiguration(
                ClientOverrideConfiguration.builder().apiCallAttemptTimeout(Duration.ofSeconds(4))
                    .apiCallTimeout(Duration.ofSeconds(10)).build()
            )
            .forcePathStyle(true).credentialsProvider(
                () -> AwsBasicCredentials.create(settings.accessKey(), settings.secretKey())
            )
            .build();
    }
}
