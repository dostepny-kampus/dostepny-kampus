/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.storage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Magazyn, który przyjmuje połączenie i nie odpowiada, kończy wysyłkę zdjęcia błędem przed
 * upływem 15 sekund, zamiast trzymać żądanie bez końca.
 *
 * <p>Wymagania: NFR-50</p>
 *
 * @since 0.1.0
 */
final class PhotoStorageTest {

    @Test
    void failsUploadToSilentStorageInTime() throws IOException {
        final List<Socket> held = new ArrayList<>(4);
        try (ServerSocket silent = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            final Thread acceptor = new Thread(() -> PhotoStorageTest.hold(silent, held));
            acceptor.setDaemon(true);
            acceptor.start();
            final S3Client client = PhotoStorage.photoClient(
                new PhotoSettings(
                    URI.create(String.format("http://127.0.0.1:%d", silent.getLocalPort())),
                    "zdjecia", "klucz", "sekret"
                )
            );
            Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(15),
                () -> Assertions.assertThrows(
                    SdkClientException.class,
                    () -> client.putObject(
                        PutObjectRequest.builder().bucket("zdjecia").key("zdjecie").build(),
                        RequestBody.fromBytes(new byte[] {1, 2, 3})
                    ),
                    "Wysyłka do milczącego magazynu nie skończyła się błędem klienta"
                ),
                "Wysyłka do milczącego magazynu nie skończyła się błędem w 15 sekund"
            );
        }
    }

    private static void hold(final ServerSocket silent, final List<Socket> held) {
        try {
            while (!silent.isClosed()) {
                held.add(silent.accept());
            }
        } catch (final IOException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
