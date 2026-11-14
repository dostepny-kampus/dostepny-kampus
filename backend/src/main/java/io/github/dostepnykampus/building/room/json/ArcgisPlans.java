/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczyt pięter i pomieszczeń z warstw usługi ArcGIS PWr (FeatureServer) w formacie
 * GeoJSON w układzie WGS 84; kolejne strony wyników są pobierane do ostatniej. Wielokąt
 * pomieszczenia staje się konturem MultiPolygon, pomieszczenie bez wielokąta jest pomijane.
 * Numer z samych spacji jest pusty, a brak funkcji daje „Inne”. Błąd lub przekroczenie
 * czasu daje IllegalStateException.
 *
 * <p>TODO(#1823): podłączyć klienta do zapisu importu.</p>
 *
 * <p>Wymagania: US16, US18, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
final class ArcgisPlans {

    /**
     * Domyślny limit czasu połączenia i odczytu odpowiedzi usługi.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Klient HTTP.
     */
    private final RestClient client;

    /**
     * Parser odpowiedzi i budowniczy konturu.
     */
    private final ObjectMapper mapper;

    @Autowired
    ArcgisPlans(final ObjectMapper mapper) {
        this(ArcgisPlans.TIMEOUT, mapper);
    }

    ArcgisPlans(final Duration timeout, final ObjectMapper mapper) {
        this(ArcgisPlans.http(timeout), mapper);
    }

    ArcgisPlans(final RestClient client, final ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Piętra z warstwy pięter; piętro bez identyfikatora jest pomijane.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pięter: LEVEL_ID, NAME, LEVEL_NUMBER
     * @return Piętra w kolejności usługi
     */
    List<PlanFloor> floors(final String service, final int layer) {
        return this.features(service, layer, "LEVEL_ID,NAME,LEVEL_NUMBER", false).stream()
            .map(feature -> feature.path("properties"))
            .filter(floor -> !floor.path("LEVEL_ID").asString("").isBlank()).map(
                floor -> new PlanFloor(
                    floor.path("LEVEL_ID").asString("").strip(),
                    floor.path("NAME").asString("").strip(),
                    floor.path("LEVEL_NUMBER").asInt(0)
                )
            ).toList();
    }

    /**
     * Pomieszczenia z warstwy pomieszczeń.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pomieszczeń: Pietro, NUMER, FUNKCJA
     * @return Pomieszczenia z konturem w kolejności usługi
     */
    List<PlanRoom> rooms(final String service, final int layer) {
        return this.features(service, layer, "Pietro,NUMER,FUNKCJA", true).stream().flatMap(
            feature -> this.outline(feature.path("geometry")).map(
                outline -> new PlanRoom(
                    feature.path("properties").path("Pietro").asString(""),
                    feature.path("properties").path("NUMER").asString("").strip(),
                    Optional.of(feature.path("properties").path("FUNKCJA").asString("").strip())
                        .filter(function -> !function.isEmpty()).orElse("Inne"),
                    outline
                )
            ).stream()
        ).toList();
    }

    private List<JsonNode> features(
        final String service, final int layer, final String fields, final boolean geometry
    ) {
        final List<JsonNode> result = new ArrayList<>(0);
        boolean more = true;
        while (more) {
            final JsonNode page = this.page(
                UriComponentsBuilder.fromUriString(service)
                    .pathSegment(String.valueOf(layer), "query")
                    .queryParam("where", "1=1").queryParam("outFields", fields)
                    .queryParam("returnGeometry", geometry).queryParam("outSR", 4326)
                    .queryParam("f", "geojson").queryParam("resultOffset", result.size())
                    .queryParam("resultRecordCount", 1000).build().encode().toUri()
            );
            page.get("features").forEach(result::add);
            more = page.path("properties").path("exceededTransferLimit").asBoolean(false)
                && !page.get("features").isEmpty();
        }
        return result;
    }

    private JsonNode page(final URI uri) {
        final JsonNode root;
        try {
            root = this.mapper.readTree(
                this.client.get().uri(uri).retrieve().requiredBody(String.class)
            );
        } catch (final RestClientException ex) {
            throw new IllegalStateException(
                String.format(
                    "Przekroczono limit czasu albo błąd usługi ArcGIS: %s",
                    uri.getPath()
                ),
                ex
            );
        }
        if (root.has("error") || !root.path("features").isArray()) {
            throw new IllegalStateException(uri.getPath());
        }
        return root;
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

    private static RestClient http(final Duration timeout) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory).build();
    }
}
