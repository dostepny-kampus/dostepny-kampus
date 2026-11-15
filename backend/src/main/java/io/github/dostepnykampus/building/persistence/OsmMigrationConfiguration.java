/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejestruje import OSM jako migrację historii głównej Flyway, a import granic
 * terenów jako migrację historii modułu budynków.
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public final class OsmMigrationConfiguration {

    private OsmMigrationConfiguration() {
    }

    /**
     * Import konturów i wejść z pliku OSM dostarczonego z serwerem.
     *
     * @param mapper Odczyt GeoJSON
     * @param file Plik konturów i wejść
     * @return Powtarzalna migracja
     */
    @Bean
    static FileMigration osmBuildings(
        final ObjectMapper mapper,
        @Value("classpath:data/pwr-buildings.geojson") final Resource file
    ) {
        return OsmMigrationConfiguration.buildings(mapper, file);
    }

    /**
     * Przypisuje import OSM do głównej historii migracji.
     *
     * @param migration Import budynków i wejść
     * @return Konfiguracja rejestracji migracji
     */
    @Bean
    static FlywayConfigurationCustomizer osmMigration(
        @Qualifier("osmBuildings") final FileMigration migration
    ) {
        return new OsmMigrationResolver(migration, "db/migration/__root");
    }

    /**
     * Import granic terenów z pliku geometrii dostarczonego z serwerem.
     *
     * @param mapper Odczyt GeoJSON
     * @param file Plik granic terenów
     * @return Powtarzalna migracja
     */
    @Bean
    static FileMigration osmAreas(
        final ObjectMapper mapper,
        @Value("classpath:data/pwr-areas.geojson") final Resource file
    ) {
        return new FileMigration("tereny PWr", file, OsmAreas.FORMAT, new OsmAreas(mapper));
    }

    /**
     * Przypisuje import granic terenów do historii migracji modułu budynków.
     *
     * @param migration Import granic terenów
     * @return Konfiguracja rejestracji migracji
     */
    @Bean
    static FlywayConfigurationCustomizer areasMigration(
        @Qualifier("osmAreas") final FileMigration migration
    ) {
        return new OsmMigrationResolver(migration, "db/migration/building");
    }

    /**
     * Import konturów i wejść z pliku OSM.
     *
     * @param mapper Odczyt GeoJSON
     * @param file Plik konturów i wejść
     * @return Powtarzalna migracja
     */
    static FileMigration buildings(final ObjectMapper mapper, final Resource file) {
        return new FileMigration(
            "budynki z OpenStreetMap", file, OsmBuildings.FORMAT, new OsmBuildings(mapper)
        );
    }
}
