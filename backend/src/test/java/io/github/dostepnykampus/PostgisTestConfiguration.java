/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Baza PostGIS dla testów, uruchamiana w kontenerze Docker.
 *
 * <p>Obraz jest ten sam co w {@code infra/docker-compose.yml}. Spring łączy
 * źródło danych z kontenerem przez {@link ServiceConnection}, a Flyway
 * wykonuje migracje przy starcie kontekstu.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class PostgisTestConfiguration {

    /**
     * Tworzy klasę konfiguracji.
     *
     * <p>Wywołuje ją tylko Spring przez refleksję, gdy rejestruje tę klasę
     * jako komponent konfiguracji.</p>
     */
    private PostgisTestConfiguration() {
    }

    /**
     * Tworzy kontener PostGIS; Spring uruchamia go i zatrzymuje z kontekstem.
     *
     * @return Kontener PostgreSQL z rozszerzeniem PostGIS
     */
    @Bean
    @ServiceConnection
    static PostgreSQLContainer postgis() {
        return new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:18-3.6")
                .asCompatibleSubstituteFor("postgres")
        );
    }
}
