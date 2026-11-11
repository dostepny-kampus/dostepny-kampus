/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Budynki i ich wejścia z pliku konturów OpenStreetMap w tabelach
 * buildings i entrances (US1, US17.1, ADR-0008).
 *
 * <p>Migracja powtarzalna: Flyway wykonuje ją przy pierwszym starcie
 * i ponownie po zmianie pliku lub wersji importu zapisanej w sumie kontrolnej.
 * Kontury z tym samym symbolem tworzą jeden wielokąt złożony, a kontur
 * z kilkoma symbolami daje osobny budynek dla każdego symbolu, tak jak
 * w aplikacji. Nazwa złożona wyłącznie z symboli budynków albo jej brak
 * to budynek bez nazwy (decyzja 75). Ta sama reguła działa w aplikacji
 * w buildingName() w BuildingName.kt (core:geo); obie muszą być zgodne.
 * Istniejący budynek dostaje nowy kontur, nazwę i atrybucję, a poziom
 * szczegółowości zostaje bez zmian.</p>
 *
 * @since 0.1.0
 */
@Component
final class OsmBuildings implements JavaMigration {

    /**
     * Zapis budynku: nowy symbol dodaje wiersz, znany symbol zmienia kontur,
     * nazwę i atrybucję, a poziom szczegółowości zostaje.
     */
    static final String UPSERT = """
        INSERT INTO buildings (symbol, name, outline, source)
        VALUES (?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), ?)
        ON CONFLICT (symbol) DO UPDATE SET name = EXCLUDED.name,
            outline = EXCLUDED.outline, source = EXCLUDED.source
        """;

    /**
     * Wersja importu w sumie kontrolnej: zmiana wykonuje import ponownie
     * także bez zmiany pliku; 4 synchronizuje wejścia z eksportem.
     */
    static final int FORMAT = 4;

    /**
     * Nazwa złożona wyłącznie z symboli budynków, np. C-13 albo C-3/C-4.
     */
    private static final Pattern SYMBOLS = Pattern.compile(
        "[A-Z]-\\d+[a-z]?(\\s*/\\s*[A-Z]-\\d+[a-z]?)*"
    );

    /**
     * Parser pliku GeoJSON.
     */
    private final ObjectMapper mapper;

    /**
     * Plik konturów kopiowany przez Maven z data/buildings.
     */
    private final Resource file;

    OsmBuildings(
        final ObjectMapper mapper,
        @Value("classpath:data/pwr-buildings.geojson") final Resource file
    ) {
        this.mapper = mapper;
        this.file = file;
    }

    @Override
    public @Nullable MigrationVersion getVersion() {
        return null;
    }

    @Override
    public String getDescription() {
        return "budynki z OpenStreetMap";
    }

    @Override
    public Integer getChecksum() {
        return OsmBuildings.checksum(this.content(), OsmBuildings.FORMAT);
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(final Context context) throws SQLException {
        final JsonNode root = this.mapper.readTree(this.content());
        final String source = root.required("attribution").asString();
        final Map<String, ArrayNode> outlines = new LinkedHashMap<>(128);
        final Map<String, @Nullable String> names = new LinkedHashMap<>(128);
        final List<JsonNode> entrances = new ArrayList<>(128);
        for (final JsonNode feature : root.required("features")) {
            final JsonNode props = feature.required("properties");
            if ("entrance".equals(props.required("kind").asString())) {
                entrances.add(feature);
            }
            if ("building".equals(props.required("kind").asString())) {
                for (final JsonNode symbol : props.required("symbols")) {
                    this.add(outlines, symbol.asString(), feature.required("geometry"));
                    names.putIfAbsent(symbol.asString(), props.path("name").asString(null));
                }
            }
        }
        this.save(context.getConnection(), outlines, names, source);
        new OsmEntrances(entrances).save(context.getConnection());
    }

    static int checksum(final byte[] content, final int format) {
        final CRC32 crc = new CRC32();
        crc.update(content);
        crc.update(format);
        return (int) crc.getValue();
    }

    private void save(
        final Connection connection,
        final Map<String, ArrayNode> outlines,
        final Map<String, @Nullable String> names,
        final String source
    ) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(OsmBuildings.UPSERT)) {
            for (final Map.Entry<String, ArrayNode> entry : outlines.entrySet()) {
                insert.setString(1, entry.getKey());
                insert.setString(2, OsmBuildings.named(names.get(entry.getKey())));
                insert.setString(3, this.multi(entry.getValue()));
                insert.setString(4, source);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void add(final Map<String, ArrayNode> outlines, final String symbol,
        final JsonNode geometry) {
        final ArrayNode polygons = outlines.computeIfAbsent(
            symbol, key -> this.mapper.createArrayNode()
        );
        final JsonNode coordinates = geometry.required("coordinates");
        if ("MultiPolygon".equals(geometry.required("type").asString())) {
            coordinates.forEach(polygons::add);
        } else {
            polygons.add(coordinates);
        }
    }

    private String multi(final ArrayNode polygons) {
        return this.mapper.writeValueAsString(
            this.mapper.createObjectNode()
                .put("type", "MultiPolygon")
                .set("coordinates", polygons)
        );
    }

    private static @Nullable String named(final @Nullable String name) {
        final @Nullable String result;
        if (name == null || OsmBuildings.SYMBOLS.matcher(name.trim()).matches()) {
            result = null;
        } else {
            result = name;
        }
        return result;
    }

    private byte[] content() {
        try {
            return this.file.getContentAsByteArray();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
