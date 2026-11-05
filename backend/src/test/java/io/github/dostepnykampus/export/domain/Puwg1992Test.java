/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.export.domain;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Przeliczenie WGS84 na PUWG 1992 (US13.2, NFR-43) względem PROJ.
 *
 * <p>Wartości oczekiwane pochodzą z {@code cs2cs EPSG:4326 EPSG:2180}
 * (PROJ, ta sama biblioteka co w PostGIS i QGIS).</p>
 *
 * @since 0.1.0
 */
final class Puwg1992Test {

    /**
     * Dopuszczalna różnica względem PROJ w metrach.
     */
    private static final double MILLIMETRE = 0.001;

    @Test
    void usThirteenTwoMatchesProjOnCampus() {
        Puwg1992Test.assertPoint(17.059_89, 51.109_16, 364_229.179_6, 362_054.214_1);
    }

    @Test
    void usThirteenTwoMatchesProjAcrossPoland() {
        Assertions.assertAll(
            Stream.of(
                new double[] {23.16, 53.13, 778_187.892_6, 593_050.718_3},
                new double[] {14.55, 53.43, 204_509.297, 627_553.312_6},
                new double[] {19.95, 49.30, 569_045.92, 159_601.314_9},
                new double[] {18.80, 54.61, 487_086.386_1, 749_596.242_2}
            ).map(
                row -> () -> Puwg1992Test.assertPoint(row[0], row[1], row[2], row[3])
            )
        );
    }

    @Test
    void usThirteenTwoCentralMeridianHasFalseEasting() {
        Puwg1992Test.assertPoint(19.0, 52.0, 500_000.0, 459_309.209_4);
    }

    private static void assertPoint(
        final double longitude, final double latitude,
        final double easting, final double northing
    ) {
        final Puwg1992Point point = new Puwg1992(longitude, latitude).point();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                easting, point.easting(), Puwg1992Test.MILLIMETRE,
                String.format("Zła współrzędna wschodnia dla %s, %s", longitude, latitude)
            ),
            () -> Assertions.assertEquals(
                northing, point.northing(), Puwg1992Test.MILLIMETRE,
                String.format("Zła współrzędna północna dla %s, %s", longitude, latitude)
            )
        );
    }
}
