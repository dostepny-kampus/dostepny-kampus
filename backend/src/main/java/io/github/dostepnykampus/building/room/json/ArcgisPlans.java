/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import io.github.dostepnykampus.building.room.PlanEntrance;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PlanSource;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Piętra, pomieszczenia i wejścia z obiektów warstw usługi ArcGIS PWr, które pobiera
 * {@link ArcgisLayers}. Wielokąt pomieszczenia staje się konturem MultiPolygon, pomieszczenie
 * bez wielokąta jest pomijane. Piętro bez identyfikatora LEVEL_ID jest pomijane. Wielkość
 * liter nazw pól pomieszczenia jest dowolna, a funkcja nazywa się Funkcja, Funkcje albo
 * d_funkcja. Numer z samych spacji jest pusty, a brak funkcji daje „Inne”. Pola wejść opisuje
 * {@link PlanSource#entrances(String, int)}.
 *
 * <p>Wymagania: US16, US18, BR-24, BR-27, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
final class ArcgisPlans implements PlanSource {

    /**
     * Szerokość drzwi w centymetrach, liczba całkowita albo z zerami po kropce.
     */
    private static final Pattern WIDTH = Pattern.compile("(\\d{1,4})(?:\\.0*)?");

    /**
     * Warstwy usługi ArcGIS.
     */
    private final ArcgisLayers layers;

    /**
     * Budowniczy konturu.
     */
    private final ObjectMapper mapper;

    @Autowired
    ArcgisPlans(final ObjectMapper mapper) {
        this(new ArcgisLayers(mapper), mapper);
    }

    ArcgisPlans(final Duration timeout, final ObjectMapper mapper) {
        this(new ArcgisLayers(timeout, mapper), mapper);
    }

    ArcgisPlans(final ArcgisLayers layers, final ObjectMapper mapper) {
        this.layers = layers;
        this.mapper = mapper;
    }

    @Override
    public List<PlanFloor> floors(final String service, final int layer) {
        return this.layers.attributes(service, layer, "LEVEL_ID,NAME,LEVEL_NUMBER").stream()
            .map(feature -> feature.path("properties"))
            .filter(floor -> !floor.path("LEVEL_ID").asString("").isBlank()).map(
                floor -> new PlanFloor(
                    floor.path("LEVEL_ID").asString("").strip(),
                    new FloorName(floor.path("NAME").asString("")).label(),
                    floor.path("LEVEL_NUMBER").asInt(0)
                )
            ).toList();
    }

    @Override
    public List<PlanRoom> rooms(final String service, final int layer) {
        return this.layers.shapes(service, layer).stream().flatMap(
            feature -> this.outline(feature.path("geometry")).map(
                outline -> new PlanRoom(
                    feature.path("id").asString(""),
                    ArcgisPlans.field(feature, "pietro"),
                    ArcgisPlans.field(feature, "numer"),
                    Optional.of(ArcgisPlans.field(feature, "funkcja", "funkcje", "d_funkcja"))
                        .filter(function -> !function.isEmpty()).orElse("Inne"),
                    outline
                )
            ).stream()
        ).toList();
    }

    @Override
    public List<PlanEntrance> entrances(final String service, final int layer) {
        return this.layers.shapes(service, layer).stream()
            .filter(feature -> "Point".equals(feature.path("geometry").path("type").asString("")))
            .filter(feature -> !ArcgisPlans.field(feature, "pietro").isEmpty())
            .map(ArcgisPlans::entrance).toList();
    }

    private static String marking(final String adapted) {
        final String result;
        if ("tak".equalsIgnoreCase(adapted)) {
            result = "ACCESSIBLE";
        } else if ("nie".equalsIgnoreCase(adapted)) {
            result = "INACCESSIBLE";
        } else {
            result = "NO_DATA";
        }
        return result;
    }

    private static @Nullable Integer width(final String text) {
        final Matcher number = ArcgisPlans.WIDTH.matcher(text);
        final Integer result;
        if (number.matches() && Integer.parseInt(number.group(1)) > 0) {
            result = Integer.valueOf(number.group(1));
        } else {
            result = null;
        }
        return result;
    }

    private static PlanEntrance entrance(final JsonNode feature) {
        final EntranceNote note = new EntranceNote(ArcgisPlans.field(feature, "uwagi"));
        return new PlanEntrance(
            ArcgisPlans.field(feature, "pietro"), feature.get("geometry").toString(),
            ArcgisPlans.marking(ArcgisPlans.field(feature, "czy_dostos")), note.name(),
            note.approach(), ArcgisPlans.width(ArcgisPlans.field(feature, "szerokosc")),
            note.exit()
        );
    }

    private static String field(final JsonNode feature, final String... names) {
        final List<String> wanted = List.of(names);
        return feature.path("properties").properties().stream()
            .filter(entry -> wanted.contains(entry.getKey().toLowerCase(Locale.ROOT)))
            .map(entry -> entry.getValue().asString("").strip())
            .filter(value -> !value.isEmpty()).findFirst().orElse("");
    }

    private Optional<String> outline(final JsonNode geometry) {
        final String type = geometry.path("type").asString("");
        final Optional<String> result;
        if ("Polygon".equals(type)) {
            result = Optional.of(
                this.mapper.createObjectNode().put("type", "MultiPolygon").set(
                    "coordinates", this.mapper.createArrayNode().add(geometry.get("coordinates"))
                ).toString()
            );
        } else if ("MultiPolygon".equals(type)) {
            result = Optional.of(geometry.toString());
        } else {
            result = Optional.empty();
        }
        return result;
    }
}
