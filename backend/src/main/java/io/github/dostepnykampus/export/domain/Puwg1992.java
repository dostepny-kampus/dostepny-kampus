/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.export.domain;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

/**
 * Punkt WGS84 przeliczony na PUWG 1992 (EPSG:2180) dla eksportu CSV
 * (US13.2, NFR-43).
 *
 * <p>Przeliczenie robi biblioteka proj4j (ADR-0004). Parametry układu
 * są te same co w PROJ dla EPSG:2180: odwzorowanie Gaussa-Krügera na
 * elipsoidzie GRS80, południk osiowy 19°E, skala 0,9993, przesunięcie
 * 500 000 m na wschód i -5 300 000 m na północ. Różnicę między WGS84
 * a ETRF2000 (poniżej metra) pomijamy, tak jak PROJ dla EPSG:2180. Bazy
 * EPSG ({@code proj4j-epsg}) nie dodajemy, bo ma własną licencję, a układ
 * jest jeden.</p>
 *
 * @since 0.1.0
 */
public final class Puwg1992 {

    /**
     * Układ WGS84 w stopniach.
     */
    private static final CoordinateReferenceSystem WGS84 = new CRSFactory().createFromParameters(
        "EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs"
    );

    /**
     * Układ PUWG 1992 w metrach, parametry jak w PROJ dla EPSG:2180.
     */
    private static final CoordinateReferenceSystem PUWG = new CRSFactory().createFromParameters(
        "EPSG:2180",
        String.join(
            " ",
            "+proj=tmerc +lat_0=0 +lon_0=19 +k=0.9993 +x_0=500000 +y_0=-5300000",
            "+ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"
        )
    );

    /**
     * Długość geograficzna w stopniach.
     */
    private final double longitude;

    /**
     * Szerokość geograficzna w stopniach.
     */
    private final double latitude;

    /**
     * Punkt WGS84 do przeliczenia.
     *
     * @param longitude Długość geograficzna w stopniach
     * @param latitude Szerokość geograficzna w stopniach
     */
    public Puwg1992(final double longitude, final double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Współrzędne punktu w PUWG 1992.
     *
     * <p>Przekształcenie powstaje przy każdym wywołaniu, bo obiekt proj4j
     * trzyma stan pośredni i nie jest bezpieczny dla wielu wątków.</p>
     *
     * @return Współrzędne wschodnia i północna w metrach
     */
    public Puwg1992Point point() {
        final ProjCoordinate target = new ProjCoordinate();
        new CoordinateTransformFactory()
            .createTransform(Puwg1992.WGS84, Puwg1992.PUWG)
            .transform(new ProjCoordinate(this.longitude, this.latitude), target);
        return new Puwg1992Point(target.x, target.y);
    }
}
