/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Wpisy warstwy wejść PWr przy wschodniej ścianie budynku według słów scenariusza,
 * np. „dwa bez uwag na wschodniej ścianie 20 m od siebie, 30 m od budynku C-14”.
 *
 * <p>Budynki do 100 m od ściany odsuwa daleko, a budynek z opisu stawia we wskazanej
 * odległości na wschód od wpisów. Zmiany trzeba robić w transakcji wycofywanej po
 * scenariuszu.</p>
 *
 * <p>Wymagania: BR-27</p>
 *
 * @since 0.1.0
 */
final class PlacedEntries {

    /**
     * Metry w stopniu szerokości geograficznej.
     */
    private static final double METRES = 111_320.0;

    /**
     * Opis wpisów słowami scenariusza.
     */
    private static final Pattern WORDS = Pattern.compile(
        String.join(
            "",
            "(jeden|dwa) (?:(?:bez uwag|z uwagą „([^”]+)”) na wschodniej ścianie",
            "(?: (\\d+) m od siebie)?|(przy wejściu głównym) z OpenStreetMap bez nazwy), ",
            "(?:(\\d+) m od budynku (\\S+)|bez innego budynku do 40 m)"
        )
    );

    /**
     * Opis wpisów słowami scenariusza.
     */
    private final String text;

    /**
     * Ctor.
     *
     * @param text Opis wpisów słowami scenariusza
     */
    PlacedEntries(final String text) {
        this.text = text;
    }

    /**
     * Ustawia otoczenie budynku i podaje warstwę wejść z wpisami.
     *
     * @param jdbc Baza aplikacji
     * @param building Symbol budynku
     * @param floor Piętro wpisów
     * @return Warstwa wejść jako GeoJSON
     */
    String layer(final JdbcClient jdbc, final String building, final String floor) {
        final Matcher words = PlacedEntries.WORDS.matcher(this.text);
        if (!words.matches()) {
            throw new IllegalArgumentException(
                String.format("Nieznany opis wpisów: %s", this.text)
            );
        }
        final double[] wall = jdbc.sql(
            """
            SELECT ST_X(spot), ST_Y(spot) FROM (
                SELECT ST_ClosestPoint(ST_Boundary(outline),
                    ST_Translate(ST_Centroid(outline), 0.01, 0)) AS spot
                FROM buildings WHERE symbol = :symbol
            ) AS east
            """
        ).param("symbol", building)
            .query((rows, num) -> new double[] {rows.getDouble(1), rows.getDouble(2)}).single();
        jdbc.sql(
            """
            UPDATE buildings SET outline = ST_Translate(outline, 1, 0)
            WHERE symbol <> :symbol AND ST_DWithin(outline::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, 100)
            """
        ).param("symbol", building).param("lon", wall[0]).param("lat", wall[1]).update();
        if (words.group(5) != null) {
            PlacedEntries.neighbour(jdbc, words, wall);
        }
        if (words.group(4) != null) {
            jdbc.sql(
                """
                INSERT INTO entrances (id, building_symbol, type, main, location, marking,
                    marking_date, marking_source)
                VALUES ('node/9999999901', :symbol, 'main', TRUE,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 'NO_DATA', CURRENT_DATE,
                    'IMPORT')
                """
            ).param("symbol", building).param("lon", wall[0]).param("lat", wall[1]).update();
        }
        final List<String> features = new ArrayList<>(2);
        for (final double lat : PlacedEntries.latitudes(words, wall[1])) {
            features.add(
                String.format(
                    String.join(
                        "",
                        "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[%s,%s]},",
                        "\"properties\":{\"Pietro\":\"%s\",\"Czy_dostos\":\"Nie\",",
                        "\"Szerokosc\":0,\"Uwagi\":%s}}"
                    ),
                    wall[0], lat, floor, PlacedEntries.remarks(words.group(2))
                )
            );
        }
        return String.format("{\"features\":[%s]}", String.join(",", features));
    }

    private static void neighbour(
        final JdbcClient jdbc, final Matcher words, final double... wall
    ) {
        final int moved = jdbc.sql(
            """
            UPDATE buildings SET outline = ST_Multi(ST_MakeEnvelope(:west, :lat - 0.0003,
                :west + 0.0005, :lat + 0.0003, 4326))
            WHERE symbol = :symbol
            """
        ).param(
            "west",
            wall[0] + Double.parseDouble(words.group(5))
                / (PlacedEntries.METRES * Math.cos(Math.toRadians(wall[1])))
        ).param("lat", wall[1]).param("symbol", words.group(6)).update();
        if (moved != 1) {
            throw new IllegalStateException(
                String.format("Brak budynku %s w danych testowych", words.group(6))
            );
        }
    }

    private static String remarks(final @Nullable String text) {
        final String result;
        if (text == null) {
            result = "null";
        } else {
            result = String.format("\"%s\"", text);
        }
        return result;
    }

    private static List<Double> latitudes(final Matcher words, final double lat) {
        final List<Double> result;
        if ("dwa".equals(words.group(1))) {
            final double half = Double.parseDouble(words.group(3)) / 2 / PlacedEntries.METRES;
            result = List.of(lat - half, lat + half);
        } else {
            result = List.of(lat);
        }
        return result;
    }
}
