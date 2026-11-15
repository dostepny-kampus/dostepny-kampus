/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.photo.JpegCleaner;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Czyszczenie przyjmuje tylko plik, który da się zdekodować jako obraz najwyżej
 * 4096 × 4096 pikseli: dane obrazu z obcą treścią, brak znacznika końca i wymiary ponad
 * granicę kończą się błędem.
 *
 * <p>Wymagania: NFR-26, NFR-34</p>
 *
 * @since 0.1.0
 */
final class JpegImageTest {

    @Test
    void keepsDecodableImage() throws IOException {
        Assertions.assertTrue(
            new JpegCleaner().strip(JpegImageTest.image()).length > 0,
            "Poprawne zdjęcie zostało odrzucone"
        );
    }

    @Test
    void rejectsForeignImageData() throws IOException {
        final byte[] jpeg = JpegImageTest.image();
        final int start = JpegImageTest.scan(jpeg);
        Arrays.fill(jpeg, start, jpeg.length - 2, (byte) '<');
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(jpeg),
            "Plik z obcą treścią w miejscu danych obrazu przeszedł"
        );
    }

    @Test
    void rejectsMissingEndMarker() throws IOException {
        final byte[] jpeg = JpegImageTest.image();
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(Arrays.copyOf(jpeg, jpeg.length - 2)),
            "Plik bez znacznika końca przeszedł"
        );
    }

    @Test
    void rejectsDimensionsOverLimit() throws IOException {
        final byte[] jpeg = JpegImageTest.image();
        final int frame = JpegImageTest.marker(jpeg, 0xC0);
        jpeg[frame + 5] = (byte) 0x10;
        jpeg[frame + 6] = (byte) 0x01;
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(jpeg),
            "Plik o wysokości 4097 pikseli przeszedł"
        );
    }

    private static byte[] image() throws IOException {
        final BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 7, 0x12AB34);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", bytes);
        return bytes.toByteArray();
    }

    private static int scan(final byte[] jpeg) {
        final int sos = JpegImageTest.marker(jpeg, 0xDA);
        return sos + 2 + (((jpeg[sos + 2] & 0xFF) << 8) | (jpeg[sos + 3] & 0xFF));
    }

    private static int marker(final byte[] jpeg, final int code) {
        int found = -1;
        for (int idx = 0; idx < jpeg.length - 1 && found < 0; idx += 1) {
            if ((jpeg[idx] & 0xFF) == 0xFF && (jpeg[idx + 1] & 0xFF) == code) {
                found = idx;
            }
        }
        return found;
    }
}
