/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Treść pliku konturów OpenStreetMap (ADR-0008) jako budynki i wejścia do zapisu, bez bazy.
 * Kontury jednego symbolu tworzą jeden wielokąt złożony, kontur z kilkoma symbolami daje
 * budynek dla każdego, a nazwą jest pierwsze pole display_name symbolu. Wejście ma wiersz dla
 * każdego budynku; wheelchair: yes i designated dają ACCESSIBLE, no daje INACCESSIBLE, inna
 * wartość i brak tagu NO_DATA. Wyniki idą w kolejności pliku; brak wymaganego pola kończy się
 * wyjątkiem Jacksona.
 *
 * <p>Wymagania: US1, US2, US17.1, BR-24</p>
 *
 * @since 0.1.0
 */
final class OsmFile {

    /**
     * Tworzy węzły wielokątów złożonych i zapisuje je jako tekst.
     */
    private final ObjectMapper mapper;

    /**
     * Korzeń pliku GeoJSON.
     */
    private final JsonNode root;

    /**
     * Plik już wczytany przez parser.
     *
     * @param mapper Parser JSON
     * @param root Korzeń pliku GeoJSON z polami attribution i features
     */
    OsmFile(final ObjectMapper mapper, final JsonNode root) {
        this.mapper = mapper;
        this.root = root;
    }

    /**
     * Atrybucja źródła danych.
     *
     * @return Wartość pola attribution
     */
    String attribution() {
        return this.root.required("attribution").asString();
    }

    /**
     * Budynki z obiektów kind = building, po jednym na symbol.
     *
     * @return Budynki w kolejności pierwszego wystąpienia symbolu
     */
    List<ImportedBuilding> buildings() {
        final Map<String, ArrayNode> outlines = new LinkedHashMap<>(128);
        final Map<String, @Nullable String> names = new LinkedHashMap<>(128);
        for (final JsonNode feature : this.features("building")) {
            final JsonNode props = feature.required("properties");
            for (final JsonNode symbol : props.required("symbols")) {
                this.add(outlines, symbol.asString(), feature.required("geometry"));
                names.putIfAbsent(symbol.asString(), props.path("display_name").asString(null));
            }
        }
        final List<ImportedBuilding> result = new ArrayList<>(outlines.size());
        for (final Map.Entry<String, ArrayNode> entry : outlines.entrySet()) {
            result.add(
                new ImportedBuilding(
                    entry.getKey(), names.get(entry.getKey()), this.multi(entry.getValue())
                )
            );
        }
        return result;
    }

    /**
     * Wejścia z obiektów kind = entrance, po jednym na parę wejście i budynek.
     *
     * @return Wejścia w kolejności pliku i symboli
     */
    List<ImportedEntrance> entrances() {
        final List<ImportedEntrance> result = new ArrayList<>(64);
        for (final JsonNode feature : this.features("entrance")) {
            final JsonNode props = feature.required("properties");
            final String id = props.required("osm_id").asString();
            for (final JsonNode symbol : props.required("symbols")) {
                result.add(
                    new ImportedEntrance(
                        id,
                        symbol.asString(),
                        props.path("ref").asString(null),
                        props.path("entrance").asString(null),
                        "main".equals(props.path("entrance").asString(null)),
                        feature.required("geometry").toString(),
                        OsmFile.marking(props.path("wheelchair").asString(""))
                    )
                );
            }
        }
        return result;
    }

    private List<JsonNode> features(final String kind) {
        final List<JsonNode> result = new ArrayList<>(128);
        for (final JsonNode feature : this.root.required("features")) {
            if (kind.equals(feature.required("properties").required("kind").asString())) {
                result.add(feature);
            }
        }
        return result;
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

    private static String marking(final String wheelchair) {
        final String result;
        if ("yes".equals(wheelchair) || "designated".equals(wheelchair)) {
            result = "ACCESSIBLE";
        } else if ("no".equals(wheelchair)) {
            result = "INACCESSIBLE";
        } else {
            result = "NO_DATA";
        }
        return result;
    }
}
