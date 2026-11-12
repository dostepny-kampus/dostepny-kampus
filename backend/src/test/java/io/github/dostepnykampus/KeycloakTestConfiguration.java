/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Usługa logowania Keycloak w kontenerze, z realmem deweloperskim.
 *
 * <p>Obraz jest czytany z {@code infra/docker-compose.yml}, a realm z
 * {@code infra/keycloak/realm.json}, więc test sprawdza te same wersje i te
 * same konta co środowisko deweloperskie. Serwer dostaje adres realmu
 * z kontenera i sprawdza tokeny domyślnym dekoderem.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class KeycloakTestConfiguration {

    /**
     * Nazwa i hasło administratora Keycloaka w teście.
     */
    static final String ADMIN = "administrator-testu";

    /**
     * Port HTTP Keycloaka w kontenerze.
     */
    private static final int PORT = 8180;

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private KeycloakTestConfiguration() {
    }

    /**
     * Tworzy kontener Keycloaka; Spring uruchamia go i zatrzymuje z kontekstem.
     *
     * @return Kontener usługi logowania z zaimportowanym realmem
     * @throws IOException Gdy nie da się przeczytać pliku compose
     */
    @Bean
    static GenericContainer<?> keycloak() throws IOException {
        final GenericContainer<?> container = new GenericContainer<>(
            DockerImageName.parse(
                Pattern.compile("image: (quay\\.io/keycloak/keycloak:\\S+)")
                    .matcher(Files.readString(Path.of("../infra/docker-compose.yml")))
                    .results().findFirst().orElseThrow().group(1)
            )
        );
        container.withCommand(
            "start-dev", "--import-realm", "--http-port=%d".formatted(KeycloakTestConfiguration.PORT)
        );
        container.withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", KeycloakTestConfiguration.ADMIN);
        container.withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", KeycloakTestConfiguration.ADMIN);
        container.withCopyFileToContainer(
            MountableFile.forHostPath("../infra/keycloak/realm.json"),
            "/opt/keycloak/data/import/realm.json"
        );
        container.withExposedPorts(KeycloakTestConfiguration.PORT);
        container.waitingFor(
            Wait.forHttp("/realms/dostepny-kampus").forPort(KeycloakTestConfiguration.PORT)
                .withStartupTimeout(Duration.ofMinutes(3))
        );
        return container;
    }

    /**
     * Wskazuje serwerowi realm z kontenera jako wystawcę tokenów.
     *
     * @param keycloak Kontener usługi logowania
     * @return Ustawienie adresu wystawcy tokenów
     */
    @Bean
    static DynamicPropertyRegistrar issuer(final GenericContainer<?> keycloak) {
        return registry -> registry.add(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> "http://%s:%d/realms/dostepny-kampus".formatted(
                keycloak.getHost(), keycloak.getMappedPort(KeycloakTestConfiguration.PORT)
            )
        );
    }
}
