/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Plik JPEG, którego dane obrazu dekoduje ImageIO bez ostrzeżeń, o wymiarach najwyżej
 * 4096 × 4096 pikseli. Wymiary są czytane z nagłówka przed dekodowaniem, a obraz jest
 * dekodowany co szesnasty piksel w każdym kierunku, więc zajęta pamięć zależy od wymiarów
 * dopuszczonych, nie od deklarowanych.
 *
 * <p>Wymagania: NFR-26, NFR-34</p>
 *
 * @since 0.1.0
 */
final class JpegImage {

    /**
     * Największa szerokość i wysokość obrazu w pikselach.
     */
    private static final int MAX_SIDE = 4096;

    /**
     * Krok próbkowania przy dekodowaniu.
     */
    private static final int STEP = 16;

    /**
     * Bajty pliku.
     */
    private final byte[] jpeg;

    JpegImage(final byte[] jpeg) {
        this.jpeg = jpeg.clone();
    }

    /**
     * Sprawdza wymiary i dane obrazu.
     *
     * @throws IOException Gdy plik nie jest JPEG, wymiary przekraczają granicę albo dekoder
     *  zgłasza uszkodzone dane obrazu, w tym brak znacznika końca
     */
    void verify() throws IOException {
        final ImageReader reader = ImageIO.getImageReadersByFormatName("jpeg").next();
        try (ImageInputStream input = ImageIO.createImageInputStream(
            new ByteArrayInputStream(this.jpeg)
        )) {
            final List<String> warnings = new ArrayList<>(1);
            reader.setInput(input, true, true);
            reader.addIIOReadWarningListener((source, warning) -> warnings.add(warning));
            if (Math.max(reader.getWidth(0), reader.getHeight(0)) > JpegImage.MAX_SIDE) {
                throw new IOException("Obraz większy niż 4096 × 4096 pikseli");
            }
            final ImageReadParam sampled = reader.getDefaultReadParam();
            sampled.setSourceSubsampling(JpegImage.STEP, JpegImage.STEP, 0, 0);
            reader.read(0, sampled);
            if (!warnings.isEmpty()) {
                throw new IOException(
                    "Uszkodzone dane obrazu JPEG: %s".formatted(warnings.get(0))
                );
            }
        } finally {
            reader.dispose();
        }
    }
}
