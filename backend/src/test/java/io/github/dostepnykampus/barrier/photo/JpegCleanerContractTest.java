/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Usuwanie metadanych nie zmienia skompresowanego obrazu ani profilu ICC.
 *
 * <p>Wymagania: NFR-26</p>
 *
 * @since 0.1.0
 */
final class JpegCleanerContractTest {

    @ParameterizedTest
    @ValueSource(
        ints = {
            0xE1, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9,
            0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF, 0xFE
        }
    )
    void nfrTwentySixRemovesEveryPrivateSegment(final int marker) throws Exception {
        final byte[] plain = JpegCleanerContractTest.image(false);
        Assertions.assertArrayEquals(
            plain,
            JpegCleaner.strip(
                JpegCleanerContractTest.insert(
                    plain, marker, "private location".getBytes(StandardCharsets.US_ASCII)
                )
            ),
            "Pozostał prywatny segment JPEG"
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nfrTwentySixKeepsExactImageAndIcc(final boolean progressive) throws Exception {
        final ByteArrayOutputStream profile = new ByteArrayOutputStream();
        profile.write("ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII));
        profile.write(1);
        profile.write(1);
        profile.write(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData());
        final byte[] expected = JpegCleanerContractTest.insert(
            JpegCleanerContractTest.image(progressive), 0xE2, profile.toByteArray()
        );
        byte[] marked = expected;
        for (final String metadata : new String[] {
            "Exif\0\0GPS 51.1 17.0", "http://ns.adobe.com/xap/1.0/\0private XMP",
        }) {
            marked = JpegCleanerContractTest.insert(
                marked, 0xE1, metadata.getBytes(StandardCharsets.US_ASCII)
            );
        }
        marked = JpegCleanerContractTest.insert(
            marked, 0xED, "Photoshop 3.0\0private IPTC".getBytes(StandardCharsets.US_ASCII)
        );
        marked = JpegCleanerContractTest.insert(
            marked, 0xFE, "private comment".getBytes(StandardCharsets.US_ASCII)
        );
        Assertions.assertArrayEquals(
            expected, JpegCleaner.strip(marked), "Zmienił się obraz, JFIF lub profil ICC"
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nfrTwentySixKeepsCleanJpeg(final boolean progressive) throws IOException {
        final byte[] plain = JpegCleanerContractTest.image(progressive);
        Assertions.assertArrayEquals(
            plain, JpegCleaner.strip(plain), "Czysty JPEG został zmieniony"
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 20})
    void nfrTwentySixRejectsJunkBetweenSegments(final int offset) throws IOException {
        final byte[] plain = JpegCleanerContractTest.image(false);
        final ByteArrayOutputStream corrupt = new ByteArrayOutputStream();
        corrupt.write(plain, 0, offset);
        corrupt.write(0);
        corrupt.write(plain, offset, plain.length - offset);
        Assertions.assertThrows(
            IOException.class, () -> JpegCleaner.strip(corrupt.toByteArray()),
            "Pominięto obcy bajt pomiędzy segmentami JPEG"
        );
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
