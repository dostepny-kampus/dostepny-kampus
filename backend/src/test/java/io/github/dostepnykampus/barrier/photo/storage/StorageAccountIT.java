/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.storage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Klucz serwera założony skryptem {@code infra/rustfs/konto-serwera.sh} zapisuje i czyta
 * zdjęcia w kubełku zdjęć, ale nie tworzy innego kubełka i nie czyta innego kubełka.
 *
 * <p>Wymagania: NFR-35</p>
 *
 * @since 0.1.0
 */
@Tag("kontekst-springa")
final class StorageAccountIT {

    /**
     * Klucz główny magazynu w teście.
     */
    private static final String ROOT = "klucz-glowny";

    /**
     * Port S3 w kontenerze.
     */
    private static final int PORT = 9000;

    @Test
    void limitsServerKeyToPhotoBucket() throws IOException {
        try (
            Network network = Network.newNetwork();
            GenericContainer<?> rustfs = StorageAccountIT.rustfs(network);
            GenericContainer<?> account = StorageAccountIT.account(network)
        ) {
            rustfs.start();
            account.start();
            final URI endpoint = URI.create(
                String.format(
                    "http://%s:%d", rustfs.getHost(), rustfs.getMappedPort(StorageAccountIT.PORT)
                )
            );
            try (
                S3Client root = PhotoStorage.photoClient(
                    new PhotoSettings(
                        endpoint, "inny", StorageAccountIT.ROOT, StorageAccountIT.ROOT
                    )
                );
                S3Client server = PhotoStorage.photoClient(
                    new PhotoSettings(endpoint, "zdjecia", "klucz-serwera", "sekret-serwera")
                )
            ) {
                root.createBucket(request -> request.bucket("inny"));
                root.putObject(request -> request.bucket("inny").key("a.jpg"), RequestBody.empty());
                new PhotoBucket(
                    server, new PhotoSettings(endpoint, "zdjecia", "klucz-serwera", "s")
                ).run(null);
                server.putObject(
                    request -> request.bucket("zdjecia").key("b.jpg"), RequestBody.empty()
                );
                Assertions.assertAll(
                    () -> Assertions.assertThrows(
                        SdkException.class,
                        () -> server.createBucket(request -> request.bucket("nowy")),
                        "Klucz serwera utworzył inny kubełek"
                    ),
                    () -> Assertions.assertThrows(
                        SdkException.class,
                        () -> server.getObjectAsBytes(
                            request -> request.bucket("inny").key("a.jpg")
                        ),
                        "Klucz serwera przeczytał obiekt innego kubełka"
                    )
                );
            }
        }
    }

    private static GenericContainer<?> rustfs(final Network network) throws IOException {
        return new GenericContainer<>(StorageAccountIT.image("rustfs/rustfs"))
            .withNetwork(network).withNetworkAliases("zdjecia")
            .withEnv("RUSTFS_ACCESS_KEY", StorageAccountIT.ROOT)
            .withEnv("RUSTFS_SECRET_KEY", StorageAccountIT.ROOT)
            .withExposedPorts(StorageAccountIT.PORT)
            .waitingFor(Wait.forHttp("/health").forPort(StorageAccountIT.PORT));
    }

    private static GenericContainer<?> account(final Network network) throws IOException {
        return new GenericContainer<>(StorageAccountIT.image("rustfs/rc"))
            .withNetwork(network)
            .withEnv("DK_PHOTOS_ENDPOINT", "http://zdjecia:9000")
            .withEnv("DK_PHOTOS_ROOT_ACCESS_KEY", StorageAccountIT.ROOT)
            .withEnv("DK_PHOTOS_ROOT_SECRET_KEY", StorageAccountIT.ROOT)
            .withEnv("DK_PHOTOS_BUCKET", "zdjecia")
            .withEnv("DK_PHOTOS_ACCESS_KEY", "klucz-serwera")
            .withEnv("DK_PHOTOS_SECRET_KEY", "sekret-serwera").withCopyFileToContainer(
                MountableFile.forHostPath(Path.of("../infra/rustfs/konto-serwera.sh")),
                "/konto-serwera.sh"
            ).withCreateContainerCmdModifier(command -> command.withEntrypoint("sh"))
            .withCommand("/konto-serwera.sh").withStartupCheckStrategy(
                new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(60))
            );
    }

    private static DockerImageName image(final String name) throws IOException {
        return DockerImageName.parse(
            Pattern.compile(String.format("image: (%s:\\S+)", Pattern.quote(name)))
                .matcher(Files.readString(Path.of("../infra/docker-compose.yml")))
                .results().findFirst().orElseThrow().group(1)
        );
    }
}
