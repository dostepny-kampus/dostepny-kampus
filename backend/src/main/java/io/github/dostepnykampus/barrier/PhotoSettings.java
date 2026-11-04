/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ustawienia magazynu zdjęć {@code dk.photos} (ADR-0009).
 *
 * @param endpoint Adres S3 magazynu, np. http://127.0.0.1:9000
 * @param bucket Kubełek ze zdjęciami
 * @param accessKey Klucz dostępu
 * @param secretKey Sekret
 * @since 0.1.0
 */
@ConfigurationProperties("dk.photos")
record PhotoSettings(URI endpoint, String bucket, String accessKey, String secretKey) {
}
