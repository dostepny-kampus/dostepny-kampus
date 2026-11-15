/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.IOException;

/**
 * Układ segmentów pliku JPEG przed pierwszym SOS, czytany bez tworzenia obiektów segmentów.
 * Plik z więcej niż 64 segmentami przed danymi obrazu jest odrzucany, zanim parser rozłoży
 * go na segmenty; inne błędy układu zostawia parserowi.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class JpegLayout {

    /**
     * Bajty pliku.
     */
    private final byte[] jpeg;

    JpegLayout(final byte[] jpeg) {
        this.jpeg = jpeg.clone();
    }

    /**
     * Liczy segmenty od znacznika SOI do pierwszego SOS albo do pierwszego bajtu, który
     * nie jest znacznikiem.
     *
     * @throws IOException Gdy segmentów jest więcej niż 64
     */
    void verify() throws IOException {
        int position = 2;
        int count = 0;
        while (this.segment(position)) {
            count += 1;
            if (count > 64) {
                throw new IOException("Ponad 64 segmenty przed danymi obrazu JPEG");
            }
            position += 2 + ((this.jpeg[position + 2] & 0xFF) << 8)
                + (this.jpeg[position + 3] & 0xFF);
        }
    }

    private boolean segment(final int position) {
        return position + 3 < this.jpeg.length
            && (this.jpeg[position] & 0xFF) == 0xFF
            && (this.jpeg[position + 1] & 0xFF) != 0xDA;
    }
}
