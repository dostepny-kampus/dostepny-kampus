/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Plik JPEG z więcej niż 64 segmentami przed danymi obrazu jest odrzucany przed rozkładem
 * na segmenty, także poprawny obraz w czyszczeniu, a 64 segmenty przechodzą.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class JpegLayoutTest {

    @Test
    void acceptsSixtyFourSegments() {
        Assertions.assertDoesNotThrow(
            () -> new JpegLayout(JpegLayoutTest.comments(64)).verify(),
            "Plik z 64 segmentami został odrzucony"
        );
    }

    @Test
    void rejectsSixtyFiveSegments() {
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegLayout(JpegLayoutTest.comments(65)).verify(),
            "Plik z 65 segmentami przeszedł"
        );
    }

    @Test
    void rejectsImageWithSixtyFiveCommentsInCleaner() throws IOException {
        final ByteArrayOutputStream image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpg", image);
        final byte[] plain = image.toByteArray();
        final ByteArrayOutputStream marked = new ByteArrayOutputStream();
        marked.write(plain, 0, 2);
        for (int idx = 0; idx < 65; idx += 1) {
            marked.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 2});
        }
        marked.write(plain, 2, plain.length - 2);
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(marked.toByteArray()),
            "Poprawny obraz z 65 komentarzami przeszedł przez czyszczenie"
        );
    }

    private static byte[] comments(final int count) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(count * 4);
        bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xD8});
        for (int idx = 0; idx < count; idx += 1) {
            bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 2});
        }
        bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xDA, 0, 2, (byte) 0xFF, (byte) 0xD9});
        return bytes.toByteArray();
    }
}
