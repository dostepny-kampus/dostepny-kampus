/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Plik JPEG z więcej niż 64 segmentami przed danymi obrazu jest odrzucany przed rozkładem
 * na segmenty, a 64 segmenty przechodzą.
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
    void rejectsMillionEmptySegmentsThroughCleaner() {
        Assertions.assertThrows(
            IOException.class,
            () -> new JpegCleaner().strip(JpegLayoutTest.comments(1_000_000)),
            "Plik z milionem pustych segmentów przeszedł"
        );
    }

    private static byte[] comments(final int count) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream((count * 4) + 8);
        bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xD8});
        for (int idx = 0; idx < count; idx += 1) {
            bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 2});
        }
        bytes.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xDA, 0, 2, (byte) 0xFF, (byte) 0xD9});
        return bytes.toByteArray();
    }
}
