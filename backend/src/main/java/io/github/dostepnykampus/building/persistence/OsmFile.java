/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Treść pliku konturów OpenStreetMap (ADR-0008) jako wejścia do zapisu, bez bazy.
 * Wejście z kilkoma symbolami daje wiersz dla każdego budynku; oznaczenie z tagu
 * wheelchair: yes i designated dają ACCESSIBLE, no daje INACCESSIBLE, inna wartość i brak
 * tagu NO_DATA. Kolejność wyników jest kolejnością obiektów w pliku. Brak wymaganego pola
 * kończy się wyjątkiem Jacksona.
 *
 * <p>Wymagania: US17.1, BR-24</p>
 *
 * @since 0.1.0
 */
final class OsmFile {

    /**
     * Korzeń pliku GeoJSON.
     */
    private final JsonNode root;

    /**
     * Plik już wczytany przez parser.
     *
     * @param root Korzeń pliku GeoJSON z polem features
     */
    OsmFile(final JsonNode root) {
        this.root = root;
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
