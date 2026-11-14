/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegRewriter;

/**
 * Usuwa metadane ze zdjęcia JPEG przed zapisem, bez ponownego kodowania obrazu:
 * segmenty APP1 (EXIF, XMP), APP3–APP15 (m.in. IPTC) i COM przed pierwszym SOS.
 * Zostają APP0 (JFIF) i APP2 z profilem ICC; dane od pierwszego SOS nie są analizowane.
 * Z EXIF znika orientacja, więc zdjęcie ma przyjść już obrócone. Obce bajty pomiędzy
 * segmentami są odrzucane, także gdy parser biblioteki umie je pominąć.
 *
 * <p>Wymagania: NFR-26</p>
 *
 * @since 0.1.0
 */
public final class JpegCleaner extends JpegRewriter {

    /**
     * Tworzy czyściciela bez stanu.
     */
    public JpegCleaner() {
        super();
    }

    /**
     * Usuwa prywatne segmenty nagłówka JPEG, zachowując dane obrazu.
     *
     * @param jpeg Plik JPEG z telefonu
     * @return Plik JPEG bez EXIF, XMP, IPTC i komentarzy
     * @throws IOException Gdy nagłówek nie jest poprawnym JPEG
     */
    public byte[] strip(final byte[] jpeg) throws IOException {
        try {
            return this.rewrite(jpeg);
        } catch (final ImagingException ex) {
            throw new IOException("Uszkodzony nagłówek JPEG", ex);
        }
    }

    private byte[] rewrite(final byte[] jpeg) throws ImagingException, IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream(jpeg.length);
        final JFIFPieces parsed = this.analyzeJfif(ByteSource.array(jpeg));
        this.writeSegments(output, parsed.pieces);
        if (output.size() != jpeg.length) {
            throw new IOException("Obce bajty pomiędzy segmentami JPEG");
        }
        output.reset();
        this.writeSegments(
            output,
            parsed.pieces.stream().filter(JpegCleaner::keep).toList()
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
