/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.photo.JpegCleaner;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Zdjęcie po zapisie nie ma EXIF z miejscem ani komentarza,
 * a obraz i profil kolorów zostają.
 *
 * <p>Wymagania: NFR-26</p>
 *
 * @since 0.1.0
 */
final class JpegCleanerTest {

    @Test
    void nfrTwentySixRemovesExifLocation() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu ma EXIF z miejscem",
            JpegCleanerTest.text(JpegCleaner.strip(JpegCleanerTest.photo())),
            Matchers.not(Matchers.containsString("GPS 51.1"))
        );
    }

    @Test
    void nfrTwentySixRemovesComment() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu ma komentarz",
            JpegCleanerTest.text(JpegCleaner.strip(JpegCleanerTest.photo())),
            Matchers.not(Matchers.containsString("Pixel 9"))
        );
    }

    @Test
    void nfrTwentySixKeepsColorProfile() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu nie ma profilu kolorów ICC",
            JpegCleanerTest.text(JpegCleaner.strip(JpegCleanerTest.photo())),
            Matchers.containsString("ICC_PROFILE")
        );
    }

    @Test
    void nfrTwentySixKeepsImage() throws IOException {
        MatcherAssert.assertThat(
            "Obraz po czyszczeniu nie ma szerokości 8 px",
            ImageIO.read(
                new ByteArrayInputStream(JpegCleaner.strip(JpegCleanerTest.photo()))
            ).getWidth(),
            Matchers.equalTo(8)
        );
    }

    @Test
    void nfrTwentySixRejectsOtherFormat() {
        Assertions.assertThrows(
            IOException.class,
            () -> JpegCleaner.strip("GIF89a".getBytes(StandardCharsets.US_ASCII)),
            "Plik GIF przeszedł jako JPEG"
        );
    }

    @Test
    void nfrTwentySixRejectsCutFile() throws IOException {
        final byte[] photo = JpegCleanerTest.photo();
        Assertions.assertThrows(
            IOException.class,
            () -> JpegCleaner.strip(Arrays.copyOf(photo, 30)),
            "Ucięty plik JPEG przeszedł"
        );
    }

    /**
     * Tworzy zdjęcie JPEG 8 × 8 px z segmentami EXIF, ICC i komentarzem.
     *
     * @return Plik JPEG z metadanymi
     * @throws IOException Gdy ImageIO nie zapisze obrazu
     */
    static byte[] photo() throws IOException {
        final ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", image);
        final byte[] plain = image.toByteArray();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(plain, 0, 2);
        out.write(JpegCleanerTest.segment(0xE1, "Exif\0\0GPS 51.1 17.0"));
        out.write(JpegCleanerTest.segment(0xE2, "ICC_PROFILE\0"));
        out.write(JpegCleanerTest.segment(0xFE, "Pixel 9"));
        out.write(plain, 2, plain.length - 2);
        return out.toByteArray();
    }

    private static byte[] segment(final int marker, final String body) {
        final byte[] data = body.getBytes(StandardCharsets.US_ASCII);
        final int size = data.length + 2;
        final byte[] segment = new byte[data.length + 4];
        segment[0] = (byte) 0xFF;
        segment[1] = (byte) marker;
        segment[2] = (byte) (size >> 8);
        segment[3] = (byte) size;
        System.arraycopy(data, 0, segment, 4, data.length);
        return segment;
    }

    private static String text(final byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
