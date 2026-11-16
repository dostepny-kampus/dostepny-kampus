/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.formats.jpeg.JpegConstants;
import org.apache.commons.imaging.formats.jpeg.xmp.JpegRewriter;

/**
 * Usuwa metadane JPEG bez ponownego kodowania: APP1 (EXIF, XMP), APP3–APP15 i COM przed SOS.
 * Zostają tylko APP0 JFIF i APP2 ICC_PROFILE; miniatura JFXX i inne APP2 (MPF, FPXR) znikają.
 * Wynik musi dać się zdekodować jako obraz najwyżej 4096 × 4096 pikseli ({@link JpegImage}).
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
     * @throws IOException Gdy nagłówek nie jest poprawnym JPEG, ma ponad 64 segmenty, obraz
     *  jest za duży albo jego dane są uszkodzone
     */
    public byte[] strip(final byte[] jpeg) throws IOException {
        new JpegLayout(jpeg).verify();
        final byte[] clean;
        try {
            clean = this.rewrite(jpeg);
        } catch (final ImagingException ex) {
            throw new IOException("Uszkodzony nagłówek JPEG", ex);
        }
        new JpegImage(clean).verify();
        return clean;
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

    private static boolean starts(final JFIFPieceSegment segment, final String name) {
        final byte[] prefix = name.getBytes(StandardCharsets.US_ASCII);
        final byte[] data = segment.getSegmentData();
        return data.length >= prefix.length
            && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static boolean keep(final JFIFPiece piece) {
        boolean allowed = true;
        if (piece instanceof JFIFPieceSegment segment) {
            allowed = segment.marker != JpegConstants.COM_MARKER;
            if (segment.isAppSegment()) {
                allowed = JpegCleaner.marked(segment, JpegConstants.JPEG_APP0_MARKER, "JFIF\0")
                    || JpegCleaner.marked(
                        segment, JpegConstants.JPEG_APP2_MARKER, "ICC_PROFILE\0"
                    );
            }
        }
        return allowed;
    }

    private static boolean marked(
        final JFIFPieceSegment segment, final int marker, final String id
    ) {
        return segment.marker == marker && JpegCleaner.starts(segment, id);
    }
}
