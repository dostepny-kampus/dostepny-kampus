/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Ciało zdjęcia mieści się w 5 000 000 bajtów według nagłówka i według odczytu.
 *
 * <p>Wymagania: BR-8</p>
 *
 * @since 0.1.0
 */
final class PhotoUploadTest {

    /**
     * Limit rozmiaru zdjęcia w bajtach.
     */
    private static final int LIMIT = 5_000_000;

    @Test
    void readsBodyAtLimit() throws Exception {
        final byte[] jpeg = new byte[PhotoUploadTest.LIMIT];
        jpeg[PhotoUploadTest.LIMIT - 1] = 7;
        Assertions.assertArrayEquals(
            jpeg,
            new PhotoUpload(new ByteArrayInputStream(jpeg), PhotoUploadTest.LIMIT).read()
        );
    }

    @Test
    void rejectsDeclaredAboveLimit() {
        Assertions.assertThrows(
            PhotoTooLargeException.class,
            () -> new PhotoUpload(
                new ByteArrayInputStream(new byte[] {1}), PhotoUploadTest.LIMIT + 1
            ).read()
        );
    }

    @Test
    void rejectsBodyAboveLimitWithoutHeader() {
        Assertions.assertThrows(
            PhotoTooLargeException.class,
            () -> new PhotoUpload(
                new ByteArrayInputStream(new byte[PhotoUploadTest.LIMIT + 1]), -1
            ).read()
        );
    }
}
