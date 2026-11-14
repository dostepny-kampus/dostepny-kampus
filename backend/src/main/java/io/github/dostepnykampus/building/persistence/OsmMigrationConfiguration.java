/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejestruje import OSM jako migrację historii głównej Flyway.
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
class OsmMigrationConfiguration {

    @Bean
    OsmBuildings osmBuildings(
        final ObjectMapper mapper,
        @Value("classpath:data/pwr-buildings.geojson") final Resource file
    ) {
        return new OsmBuildings(mapper, file);
    }

    @Bean
    FlywayConfigurationCustomizer osmMigration(final OsmBuildings migration) {
        return new OsmMigrationResolver(migration);
    }
}
