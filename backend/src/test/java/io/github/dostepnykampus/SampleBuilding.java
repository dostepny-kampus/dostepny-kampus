/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Budynek próbny w tabeli {@code buildings}: jedyne miejsce testów, które zna jej
 * kolumny (#843).
 *
 * <p>Kontur jest w EPSG:4326 i zawsze jako wielokąt złożony, jak wymaga kolumna, a
 * źródłem jest atrybucja OpenStreetMap. Budynek o symbolu, który już jest w tabeli,
 * zostaje bez zmian, więc dane wspólne dla wielu testów można wstawiać przy każdym
 * z nich.</p>
 *
 * @param symbol Symbol budynku
 * @param name Nazwa albo {@code null}, gdy budynek jej nie ma
 * @param level Poziom szczegółowości, np. "BASIC"
 * @since 0.1.0
 */
public record SampleBuilding(String symbol, @Nullable String name, String level) {

    /**
     * Wstawia budynek z konturem w WKT, np. {@code POLYGON((0 0,1 0,1 1,0 0))}.
     *
     * @param jdbc Dostęp do bazy
     * @param wkt Kontur w WKT
     */
    public void wkt(final JdbcClient jdbc, final String wkt) {
        this.insert(jdbc, "ST_GeomFromText(:outline)", wkt);
    }

    /**
     * Wstawia budynek z konturem w GeoJSON.
     *
     * @param jdbc Dostęp do bazy
     * @param json Kontur jako geometria GeoJSON
     */
    public void geoJson(final JdbcClient jdbc, final String json) {
        this.insert(jdbc, "ST_GeomFromGeoJSON(:outline)", json);
    }

    private void insert(final JdbcClient jdbc, final String geometry, final String outline) {
        jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES (:symbol, :name, :level, ST_Multi(ST_SetSRID(%s, 4326)),
                '© OpenStreetMap contributors')
            ON CONFLICT (symbol) DO NOTHING
            """.formatted(geometry)
        ).param("symbol", this.symbol)
            .param("name", this.name)
            .param("level", this.level)
            .param("outline", outline)
            .update();
    }
}
