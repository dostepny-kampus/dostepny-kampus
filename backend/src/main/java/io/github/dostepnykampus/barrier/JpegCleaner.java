/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegRewriter;

/**
 * Usuwa metadane ze zdjęcia JPEG przed zapisem (NFR-26): EXIF i XMP
 * z miejscem, modelem telefonu i czasem, IPTC i komentarze.
 *
 * <p>Obraz zostaje bez ponownego kodowania. Usuwane są segmenty APP1 (EXIF, XMP),
 * APP3–APP15 (m.in. IPTC) i COM przed pierwszym SOS. Zostaje APP0 (JFIF)
 * i APP2 z profilem kolorów ICC. Dane od pierwszego SOS nie są analizowane.
 * Razem z EXIF znika orientacja, więc telefon wysyła zdjęcie już obrócone.</p>
 *
 * @since 0.1.0
 */
final class JpegCleaner extends JpegRewriter {

    private JpegCleaner() {
    }

    /**
     * Usuwa prywatne segmenty nagłówka JPEG, zachowując dane obrazu.
     *
     * @param jpeg Plik JPEG z telefonu
     * @return Plik JPEG bez EXIF, XMP, IPTC i komentarzy
     * @throws IOException Gdy nagłówek nie jest poprawnym JPEG
     */
    static byte[] strip(final byte[] jpeg) throws IOException {
        try {
            return new JpegCleaner().rewrite(jpeg);
        } catch (final ImagingException ex) {
            throw new IOException("Uszkodzony nagłówek JPEG", ex);
        }
    }

    private byte[] rewrite(final byte[] jpeg) throws ImagingException, IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream(jpeg.length);
        this.writeSegments(
            output,
            this.analyzeJfif(ByteSource.array(jpeg)).pieces.stream()
                .filter(JpegCleaner::keep).toList()
        );
        return output.toByteArray();
    }

    private static boolean keep(final JFIFPiece piece) {
        boolean allowed = true;
        if (piece instanceof JFIFPieceSegment segment) {
            allowed = segment.marker != JpegConstants.COM_MARKER;
            if (segment.isAppSegment()) {
                allowed = segment.marker == JpegConstants.JPEG_APP0_MARKER
                    || segment.marker == JpegConstants.JPEG_APP2_MARKER;
            }
        }
        return allowed;
    }
}
