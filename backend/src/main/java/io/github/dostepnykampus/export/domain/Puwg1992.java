/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.export.domain;

/**
 * Punkt WGS84 przeliczony na PUWG 1992 (EPSG:2180) dla eksportu CSV
 * (US13.2, NFR-43).
 *
 * <p>Odwzorowanie Gaussa-Krügera na elipsoidzie GRS80: południk osiowy
 * 19°E, skala 0,9993, przesunięcie 500 000 m na wschód i -5 300 000 m na
 * północ. Szereg Krügera do czwartej potęgi trzeciego spłaszczenia różni się
 * od PROJ o mniej niż milimetr na obszarze Polski. Różnicę między WGS84
 * a ETRF2000 (poniżej metra) pomijamy, tak jak PROJ dla EPSG:2180.</p>
 *
 * @since 0.1.0
 */
public final class Puwg1992 {

    /**
     * Spłaszczenie elipsoidy GRS80.
     */
    private static final double FLATTENING = 1.0 / 298.257_222_101;

    /**
     * Trzecie spłaszczenie n.
     */
    private static final double THIRD = Puwg1992.FLATTENING / (2.0 - Puwg1992.FLATTENING);

    /**
     * Mimośród elipsoidy.
     */
    private static final double ECCENTRICITY =
        Math.sqrt(Puwg1992.FLATTENING * (2.0 - Puwg1992.FLATTENING));

    /**
     * Promień rektyfikacyjny GRS80 (półoś wielka 6 378 137 m) razy skala
     * 0,9993, w metrach.
     */
    private static final double RADIUS = 0.999_3 * 6_378_137.0 / (1.0 + Puwg1992.THIRD)
        * (1.0 + Math.pow(Puwg1992.THIRD, 2) / 4.0 + Math.pow(Puwg1992.THIRD, 4) / 64.0);

    /**
     * Współczynniki szeregu Krügera α1…α4.
     */
    private static final double[] ALPHA = {
        Puwg1992.THIRD / 2.0 - 2.0 * Math.pow(Puwg1992.THIRD, 2) / 3.0
            + 5.0 * Math.pow(Puwg1992.THIRD, 3) / 16.0
            + 41.0 * Math.pow(Puwg1992.THIRD, 4) / 180.0,
        13.0 * Math.pow(Puwg1992.THIRD, 2) / 48.0 - 3.0 * Math.pow(Puwg1992.THIRD, 3) / 5.0
            + 557.0 * Math.pow(Puwg1992.THIRD, 4) / 1440.0,
        61.0 * Math.pow(Puwg1992.THIRD, 3) / 240.0 - 103.0 * Math.pow(Puwg1992.THIRD, 4) / 140.0,
        49_561.0 * Math.pow(Puwg1992.THIRD, 4) / 161_280.0,
    };

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
     * @return Współrzędne wschodnia i północna w metrach
     */
    public Puwg1992Point point() {
        final double lambda = Math.toRadians(this.longitude - 19.0);
        final double sine = Math.sin(Math.toRadians(this.latitude));
        final double conformal = Math.sinh(
            Puwg1992.atanh(sine)
                - Puwg1992.ECCENTRICITY * Puwg1992.atanh(Puwg1992.ECCENTRICITY * sine)
        );
        final double northbase = Math.atan2(conformal, Math.cos(lambda));
        final double eastbase = Puwg1992.atanh(
            Math.sin(lambda) / Math.sqrt(1.0 + conformal * conformal)
        );
        double north = northbase;
        double east = eastbase;
        for (int order = 1; order <= Puwg1992.ALPHA.length; ++order) {
            final double alpha = Puwg1992.ALPHA[order - 1];
            north += alpha * Math.sin(2.0 * order * northbase) * Math.cosh(2.0 * order * eastbase);
            east += alpha * Math.cos(2.0 * order * northbase) * Math.sinh(2.0 * order * eastbase);
        }
        return new Puwg1992Point(
            Puwg1992.RADIUS * east + 500_000.0,
            Puwg1992.RADIUS * north - 5_300_000.0
        );
    }

    private static double atanh(final double value) {
        return 0.5 * Math.log((1.0 + value) / (1.0 - value));
    }
}
