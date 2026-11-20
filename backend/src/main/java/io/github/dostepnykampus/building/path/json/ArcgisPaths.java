/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.json;

import io.github.dostepnykampus.building.json.ArcgisLayers;
import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.PathSource;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Odcinki ścieżek dostosowanych z obiektów warstwy usługi ArcGIS PWr, które pobiera
 * {@link ArcgisLayers}. Identyfikator odcinka to pole GlobalID. Każda część MultiLineString
 * staje się osobnym odcinkiem z numerem części po ukośniku, liczonym od 1 według kolejności
 * w obiekcie, więc numer nie zależy od pominiętych części.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
final class ArcgisPaths implements PathSource {

    /**
     * Warstwy usługi ArcGIS.
     */
    private final ArcgisLayers layers;

    /**
     * Budowniczy linii GeoJSON.
     */
    private final ObjectMapper mapper;

    @Autowired
    ArcgisPaths(final ObjectMapper mapper) {
        this(new ArcgisLayers(mapper), mapper);
    }

    ArcgisPaths(final ArcgisLayers layers, final ObjectMapper mapper) {
        this.layers = layers;
        this.mapper = mapper;
    }

    @Override
    public List<AccessiblePath> paths(final String service, final int layer) {
        return this.layers.shapes(service, layer).stream().flatMap(this::segments).toList();
    }

    private Stream<AccessiblePath> segments(final JsonNode feature) {
        final String id = feature.path("properties").path("GlobalID").asString("").strip();
        final JsonNode geometry = feature.path("geometry");
        final JsonNode coordinates = geometry.path("coordinates");
        final String type = geometry.path("type").asString("");
        final Stream<AccessiblePath> result;
        if (id.isEmpty()) {
            result = Stream.empty();
        } else if ("LineString".equals(type)) {
            result = Stream.of(coordinates).filter(ArcgisPaths::line)
                .map(line -> this.path(id, line));
        } else if ("MultiLineString".equals(type)) {
            result = IntStream.range(0, coordinates.size())
                .filter(part -> ArcgisPaths.line(coordinates.get(part)))
                .mapToObj(part -> this.path(ArcgisPaths.numbered(id, part), coordinates.get(part)));
        } else {
            result = Stream.empty();
        }
        return result;
    }

    private AccessiblePath path(final String id, final JsonNode line) {
        return new AccessiblePath(
            id,
            this.mapper.createObjectNode().put("type", "LineString").set("coordinates", line)
                .toString()
        );
    }

    private static String numbered(final String id, final int part) {
        return String.format("%s/%d", id, part + 1);
    }

    private static boolean line(final JsonNode coordinates) {
        return coordinates.isArray() && coordinates.size() >= 2;
    }
}
