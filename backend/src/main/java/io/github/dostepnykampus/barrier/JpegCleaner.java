/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.io.ByteArrayOutputStream;

/**
 * Usuwa metadane ze zdjęcia JPEG przed zapisem (NFR-26): EXIF i XMP
 * z miejscem, modelem telefonu i czasem, IPTC i komentarze.
 *
 * <p>Obraz zostaje bez zmian, bo klasa kopiuje segmenty pliku bez ponownego
 * kodowania. Usuwa segmenty APP1 (EXIF, XMP), APP3–APP15 (m.in. IPTC) i COM.
 * Zostaje APP0 (JFIF) i APP2 z profilem kolorów ICC. Skanowanie kończy
 * segment SOS (0xDA), po którym są już tylko dane obrazu, albo EOI (0xD9).
 * Razem z EXIF znika orientacja, więc telefon wysyła zdjęcie już obrócone.</p>
 *
 * @since 0.1.0
 */
final class JpegCleaner {

    /**
     * Bajt rozpoczynający każdy znacznik JPEG.
     */
    private static final int MARK = 0xFF;

    private JpegCleaner() {
    }

    /**
     * Zwraca kopię zdjęcia bez metadanych; plik spoza formatu JPEG daje 400.
     *
     * @param jpeg Plik JPEG z telefonu
     * @return Plik JPEG bez EXIF, XMP, IPTC i komentarzy
     */
    static byte[] strip(final byte[] jpeg) {
        SubmissionDraft.require(
            jpeg.length > 3 && JpegCleaner.at(jpeg, 0) == JpegCleaner.MARK
                && JpegCleaner.at(jpeg, 1) == 0xD8,
            "Zdjęcie nie jest plikiem JPEG"
        );
        final ByteArrayOutputStream out = new ByteArrayOutputStream(jpeg.length);
        out.write(jpeg, 0, 2);
        int pos = 2;
        while (true) {
            SubmissionDraft.require(
                pos + 1 < jpeg.length && JpegCleaner.at(jpeg, pos) == JpegCleaner.MARK,
                "Uszkodzony plik JPEG"
            );
            final int marker = JpegCleaner.at(jpeg, pos + 1);
            if (marker == 0xDA || marker == 0xD9) {
                out.write(jpeg, pos, jpeg.length - pos);
                break;
            }
            SubmissionDraft.require(pos + 3 < jpeg.length, "Uszkodzony plik JPEG");
            final int size = 2
                + (JpegCleaner.at(jpeg, pos + 2) << 8 | JpegCleaner.at(jpeg, pos + 3));
            SubmissionDraft.require(pos + size <= jpeg.length, "Uszkodzony plik JPEG");
            if (!JpegCleaner.metadata(marker)) {
                out.write(jpeg, pos, size);
            }
            pos += size;
        }
        return out.toByteArray();
    }

    private static boolean metadata(final int marker) {
        return marker == 0xFE || marker >= 0xE1 && marker <= 0xEF && marker != 0xE2;
    }

    private static int at(final byte[] jpeg, final int pos) {
        return jpeg[pos] & JpegCleaner.MARK;
    }
}
