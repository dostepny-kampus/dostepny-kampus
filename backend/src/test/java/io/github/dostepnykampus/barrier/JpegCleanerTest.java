/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.photo.JpegCleaner;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Zdjęcie po zapisie nie ma EXIF z miejscem, komentarza ani innego prywatnego segmentu,
 * a skompresowany obraz i profil kolorów ICC zostają bez zmian.
 *
 * <p>Wymagania: NFR-26</p>
 *
 * @since 0.1.0
 */
final class JpegCleanerTest {

    @Test
    void removesExifLocation() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu ma EXIF z miejscem",
            JpegCleanerTest.text(new JpegCleaner().strip(JpegCleanerTest.photo())),
            Matchers.not(Matchers.containsString("GPS 51.1"))
        );
    }

    @Test
    void removesComment() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu ma komentarz",
            JpegCleanerTest.text(new JpegCleaner().strip(JpegCleanerTest.photo())),
            Matchers.not(Matchers.containsString("Pixel 9"))
        );
    }

    @Test
    void keepsColorProfile() throws IOException {
        MatcherAssert.assertThat(
            "Zdjęcie po czyszczeniu nie ma profilu kolorów ICC",
            JpegCleanerTest.text(new JpegCleaner().strip(JpegCleanerTest.photo())),
            Matchers.containsString("ICC_PROFILE")
        );
    }

    @Test
    void keepsImage() throws IOException {
        MatcherAssert.assertThat(
            "Obraz po czyszczeniu nie ma szerokości 8 px",
            ImageIO.read(
                new ByteArrayInputStream(new JpegCleaner().strip(JpegCleanerTest.photo()))
            ).getWidth(),
            Matchers.equalTo(8)
        );
    }

    @Test
    void rejectsOtherFormat() {
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip("GIF89a".getBytes(StandardCharsets.US_ASCII)),
            "Plik GIF przeszedł jako JPEG"
        );
    }

    @Test
    void rejectsCutFile() throws IOException {
        final byte[] photo = JpegCleanerTest.photo();
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(Arrays.copyOf(photo, 30)),
            "Ucięty plik JPEG przeszedł"
        );
    }

    @ParameterizedTest
    @ValueSource(
        ints = {
            0xE1, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9,
            0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF, 0xFE
        }
    )
    void removesEveryPrivateSegment(final int marker) throws Exception {
        final byte[] plain = JpegCleanerTest.image(false);
        Assertions.assertArrayEquals(
            plain,
            new JpegCleaner().strip(
                JpegCleanerTest.insert(
                    plain, marker, "private location".getBytes(StandardCharsets.US_ASCII)
                )
            ),
            "Pozostał prywatny segment JPEG"
        );
    }

    @ParameterizedTest
    @CsvSource({"E0, JFXX", "E2, MPF", "E2, FPXR"})
    void removesThumbnailAndOtherAppTwo(final String marker, final String name)
        throws Exception {
        final byte[] plain = JpegCleanerTest.image(false);
        Assertions.assertArrayEquals(
            plain,
            new JpegCleaner().strip(
                JpegCleanerTest.insert(
                    plain, Integer.parseInt(marker, 16),
                    name.concat("\0miniatura").getBytes(StandardCharsets.US_ASCII)
                )
            ),
            String.format("Pozostał segment %s %s", marker, name)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsExactImageAndIcc(final boolean progressive) throws Exception {
        final ByteArrayOutputStream profile = new ByteArrayOutputStream();
        profile.write("ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII));
        profile.write(1);
        profile.write(1);
        profile.write(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());
        final byte[] expected = JpegCleanerTest.insert(
            JpegCleanerTest.image(progressive), 0xE2, profile.toByteArray()
        );
        byte[] marked = expected;
        for (final String metadata : new String[] {
            "Exif\0\0GPS 51.1 17.0", "http://ns.adobe.com/xap/1.0/\0private XMP",
        }) {
            marked = JpegCleanerTest.insert(
                marked, 0xE1, metadata.getBytes(StandardCharsets.US_ASCII)
            );
        }
        marked = JpegCleanerTest.insert(
            marked, 0xED, "Photoshop 3.0\0private IPTC".getBytes(StandardCharsets.US_ASCII)
        );
        marked = JpegCleanerTest.insert(
            marked, 0xFE, "private comment".getBytes(StandardCharsets.US_ASCII)
        );
        Assertions.assertArrayEquals(
            expected, new JpegCleaner().strip(marked), "Zmienił się obraz, JFIF lub profil ICC"
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsCleanJpeg(final boolean progressive) throws IOException {
        final byte[] plain = JpegCleanerTest.image(progressive);
        Assertions.assertArrayEquals(
            plain, new JpegCleaner().strip(plain), "Czysty JPEG został zmieniony"
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 20})
    void rejectsJunkBetweenSegments(final int offset) throws IOException {
        final byte[] plain = JpegCleanerTest.image(false);
        final ByteArrayOutputStream corrupt = new ByteArrayOutputStream();
        corrupt.write(plain, 0, offset);
        corrupt.write(0);
        corrupt.write(plain, offset, plain.length - offset);
        Assertions.assertThrows(
            IOException.class, () -> new JpegCleaner().strip(corrupt.toByteArray()),
            "Pominięto obcy bajt pomiędzy segmentami JPEG"
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

    private static byte[] image(final boolean progressive) throws IOException {
        final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 7, 0x12AB34);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            final ImageWriteParam params = writer.getDefaultWriteParam();
            params.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
            if (progressive) {
                params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            }
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static byte[] insert(final byte[] jpeg, final int marker, final byte[] data)
        throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.write(jpeg, 0, 2);
            output.writeByte(0xFF);
            output.writeByte(marker);
            output.writeShort(data.length + 2);
            output.write(data);
            output.write(jpeg, 2, jpeg.length - 2);
        }
        return bytes.toByteArray();
    }
}
