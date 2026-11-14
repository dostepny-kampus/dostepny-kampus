/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plik konturów OpenStreetMap daje wejścia z wierszem na budynek i oznaczeniem z tagu
 * wheelchair; obiekty budynków nie są wejściami (ADR-0008).
 *
 * <p>Wymagania: US17.1, BR-24</p>
 *
 * @since 0.1.0
 */
final class OsmFileTest {

    @Test
    void brTwentyFourMarksEntrancesFromWheelchairTag() {
        MatcherAssert.assertThat(
            "Wejścia z pliku mają złe budynki, opisy, rodzaje albo oznaczenia",
            OsmFileTest.file().entrances(),
            Matchers.contains(
                OsmFileTest.entrance("node/1", "A", "W-1", "main", "[1,1]", "ACCESSIBLE"),
                OsmFileTest.entrance("node/1", "C", "W-1", "main", "[1,1]", "ACCESSIBLE"),
                OsmFileTest.entrance("node/2", "B", null, "service", "[2,2]", "ACCESSIBLE"),
                OsmFileTest.entrance("node/3", "B", null, null, "[3,3]", "INACCESSIBLE"),
                OsmFileTest.entrance("node/4", "B", null, null, "[4,4]", "NO_DATA"),
                OsmFileTest.entrance("node/5", "B", null, null, "[5,5]", "NO_DATA")
            )
        );
    }

    private static OsmFile file() {
        return new OsmFile(
            JsonMapper.builder().build().readTree(
            """
            {"attribution": "Dane testowe", "features": [
                {"properties": {"kind": "building", "symbols": ["A", "B"],
                    "display_name": "Pierwsza"},
                    "geometry": {"type": "Polygon", "coordinates": [[[1,1]]]}},
                {"properties": {"kind": "building", "symbols": ["A"], "display_name": "Druga"},
                    "geometry": {"type": "MultiPolygon", "coordinates": [[[[2,2]]],[[[3,3]]]]}},
                {"properties": {"kind": "building", "symbols": ["C"]},
                    "geometry": {"type": "Polygon", "coordinates": [[[4,4]]]}},
                {"properties": {"kind": "entrance", "symbols": ["A", "C"], "osm_id": "node/1",
                    "ref": "W-1", "entrance": "main", "wheelchair": "yes"},
                    "geometry": {"type": "Point", "coordinates": [1,1]}},
                {"properties": {"kind": "entrance", "symbols": ["B"], "osm_id": "node/2",
                    "entrance": "service", "wheelchair": "designated"},
                    "geometry": {"type": "Point", "coordinates": [2,2]}},
                {"properties": {"kind": "entrance", "symbols": ["B"], "osm_id": "node/3",
                    "wheelchair": "no"}, "geometry": {"type": "Point", "coordinates": [3,3]}},
                {"properties": {"kind": "entrance", "symbols": ["B"], "osm_id": "node/4",
                    "wheelchair": "limited"}, "geometry": {"type": "Point", "coordinates": [4,4]}},
                {"properties": {"kind": "entrance", "symbols": ["B"], "osm_id": "node/5"},
                    "geometry": {"type": "Point", "coordinates": [5,5]}}
            ]}
            """
            )
        );
    }

    private static ImportedEntrance entrance(
        final String id, final String building, final @Nullable String name,
        final @Nullable String type,
        final String point, final String marking
    ) {
        return new ImportedEntrance(
            id, building, name, type, "main".equals(type),
            "{\"type\":\"Point\",\"coordinates\":%s}".formatted(point), marking
        );
    }
}
