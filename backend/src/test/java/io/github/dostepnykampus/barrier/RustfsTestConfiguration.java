/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Magazyn zdjęć RustFS dla testów, uruchamiany w kontenerze Docker.
 *
 * <p>Obraz jest czytany z {@code infra/docker-compose.yml}, tak jak obraz
 * bazy w {@code PostgisTestConfiguration}. Adres i klucze trafiają do
 * ustawień {@code dk.photos}.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
final class RustfsTestConfiguration {

    /**
     * Klucz dostępu i sekret magazynu w teście.
     */
    private static final String KEY = "dostepny-kampus";

    /**
     * Port S3 w kontenerze.
     */
    private static final int PORT = 9000;

    private RustfsTestConfiguration() {
    }

    /**
     * Tworzy kontener RustFS; Spring uruchamia go i zatrzymuje z kontekstem.
     *
     * @return Kontener magazynu zdjęć
     * @throws IOException Gdy nie da się przeczytać pliku compose
     */
    @Bean
    static GenericContainer<?> rustfs() throws IOException {
        return new GenericContainer<>(
            DockerImageName.parse(
                Pattern.compile("image: (rustfs/rustfs:\\S+)")
                    .matcher(Files.readString(Path.of("../infra/docker-compose.yml")))
                    .results().findFirst().orElseThrow().group(1)
            )
        ).withEnv("RUSTFS_ACCESS_KEY", RustfsTestConfiguration.KEY)
            .withEnv("RUSTFS_SECRET_KEY", RustfsTestConfiguration.KEY)
            .withExposedPorts(RustfsTestConfiguration.PORT)
            .waitingFor(Wait.forHttp("/health").forPort(RustfsTestConfiguration.PORT));
    }

    /**
     * Wskazuje serwerowi magazyn z kontenera.
     *
     * @param rustfs Kontener magazynu zdjęć
     * @return Ustawienia dk.photos z adresem i kluczami kontenera
     */
    @Bean
    static DynamicPropertyRegistrar photos(final GenericContainer<?> rustfs) {
        return registry -> {
            registry.add(
                "dk.photos.endpoint",
                () -> String.format(
                    "http://%s:%d",
                    rustfs.getHost(),
                    rustfs.getMappedPort(RustfsTestConfiguration.PORT)
                )
            );
            registry.add("dk.photos.access-key", () -> RustfsTestConfiguration.KEY);
            registry.add("dk.photos.secret-key", () -> RustfsTestConfiguration.KEY);
        };
    }
}
