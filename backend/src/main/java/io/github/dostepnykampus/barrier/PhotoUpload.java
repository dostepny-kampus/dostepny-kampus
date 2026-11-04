/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.io.IOException;
import java.io.InputStream;

/**
 * Ograniczony odczyt zdjęcia przed czyszczeniem: NFR-34, 5 MB = 5 000 000 bajtów.
 * Nie zamyka strumienia należącego do kontenera HTTP.
 *
 * @param source Ciało żądania
 * @param declared Zadeklarowany rozmiar albo -1, gdy nieznany
 * @since 0.1.0
 */
record PhotoUpload(InputStream source, long declared) {

    /**
     * Maksymalny rozmiar wejściowego JPEG z NFR-34.
     */
    private static final int MAX_BYTES = 5_000_000;

    /**
     * Czyta najwyżej limit bajtów do bufora; dodatkowy bajt wykrywa przekroczenie.
     *
     * @return Ciało mieszczące się w limicie
     * @throws IOException Gdy odczyt się nie uda lub rozmiar przekracza limit
     *  (PhotoTooLargeException)
     */
    byte[] read() throws IOException {
        if (this.declared > PhotoUpload.MAX_BYTES) {
            throw new PhotoTooLargeException();
        }
        final byte[] jpeg = this.source.readNBytes(PhotoUpload.MAX_BYTES);
        if (this.source.read() != -1) {
            throw new PhotoTooLargeException();
        }
        return jpeg;
    }
}
