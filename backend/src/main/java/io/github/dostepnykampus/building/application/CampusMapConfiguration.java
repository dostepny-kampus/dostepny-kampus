/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Udostępnia usługę mapy kampusu PWr z ustawień serwera.
 *
 * <p>Wymagania: NFR-47</p>
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CampusMapSettings.class)
public final class CampusMapConfiguration {

    /**
     * Tworzy konfigurację bez stanu.
     */
    public CampusMapConfiguration() {
        // Bez stanu: usługę czyta CampusMapSettings.
    }
}
