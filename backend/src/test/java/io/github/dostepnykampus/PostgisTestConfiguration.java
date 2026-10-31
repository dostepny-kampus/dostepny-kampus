/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Baza PostGIS dla testów, uruchamiana w kontenerze Docker.
 *
 * <p>Obraz jest czytany z {@code infra/docker-compose.yml}, więc test i compose
 * mają jedną wersję, którą podbija Dependabot. Spring łączy źródło danych
 * z kontenerem przez {@link ServiceConnection}, a Flyway wykonuje migracje
 * przy starcie kontekstu.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class PostgisTestConfiguration {

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private PostgisTestConfiguration() {
    }

    /**
     * Tworzy kontener PostGIS; Spring uruchamia go i zatrzymuje z kontekstem.
     *
     * @return Kontener PostgreSQL z rozszerzeniem PostGIS
     * @throws IOException Gdy nie da się przeczytać pliku compose
     */
    @Bean
    @ServiceConnection
    static PostgreSQLContainer postgis() throws IOException {
        return new PostgreSQLContainer(
            DockerImageName.parse(
                Pattern.compile("image: (postgis/postgis:\\S+)")
                    .matcher(Files.readString(Path.of("../infra/docker-compose.yml")))
                    .results().findFirst().orElseThrow().group(1)
            ).asCompatibleSubstituteFor("postgres")
        );
    }
}
