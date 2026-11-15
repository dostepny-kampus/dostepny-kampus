/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.geo;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Przeliczenie WGS84 na PUWG 1992 względem PROJ.
 *
 * <p>Wartości oczekiwane pochodzą z {@code cs2cs EPSG:4326 EPSG:2180}
 * (PROJ, ta sama biblioteka co w PostGIS i QGIS).</p>
 *
 * <p>Wymagania: US13.2, NFR-43</p>
 *
 * @since 0.1.0
 */
final class Puwg1992Test {

    /**
     * Dopuszczalna różnica względem PROJ w metrach.
     */
    private static final double MILLIMETRE = 0.001;

    @Test
    void matchesProjOnCampus() {
        Assertions.assertArrayEquals(
            new double[] {364_229.179_6, 362_054.214_1},
            Puwg1992Test.point(17.059_89, 51.109_16),
            Puwg1992Test.MILLIMETRE,
            "Zły punkt PUWG 1992 na kampusie"
        );
    }

    @Test
    void matchesProjAcrossPoland() {
        Assertions.assertAll(
            Stream.of(
                new double[] {23.16, 53.13, 778_187.892_6, 593_050.718_3},
                new double[] {14.55, 53.43, 204_509.297, 627_553.312_6},
                new double[] {19.95, 49.30, 569_045.92, 159_601.314_9},
                new double[] {18.80, 54.61, 487_086.386_1, 749_596.242_2}
            ).map(
                row -> () -> Assertions.assertArrayEquals(
                    new double[] {row[2], row[3]},
                    Puwg1992Test.point(row[0], row[1]),
                    Puwg1992Test.MILLIMETRE,
                    String.format("Zły punkt PUWG 1992 dla %s, %s", row[0], row[1])
                )
            )
        );
    }

    @Test
    void givesFalseEastingOnCentralMeridian() {
        Assertions.assertArrayEquals(
            new double[] {500_000.0, 459_309.209_4},
            Puwg1992Test.point(19.0, 52.0),
            Puwg1992Test.MILLIMETRE,
            "Zły punkt PUWG 1992 na południku osiowym"
        );
    }

    private static double[] point(final double longitude, final double latitude) {
        final Puwg1992Point point = new Puwg1992(longitude, latitude).point();
        return new double[] {point.easting(), point.northing()};
    }
}
