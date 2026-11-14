/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Udostępnia warstwy planów PWr z ustawień serwera.
 *
 * <p>Wymagania: NFR-47</p>
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PlanSettings.class)
public final class PlanConfiguration {

    /**
     * Tworzy konfigurację bez stanu.
     */
    public PlanConfiguration() {
        // Bez stanu: warstwy czyta PlanSettings.
    }
}
