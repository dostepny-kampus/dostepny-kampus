/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.json;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Warstwy usługi ArcGIS PWr (FeatureServer): obiekty warstwy w formacie GeoJSON w układzie
 * WGS 84, pobierane stronami po 1000 do ostatniej strony, oraz opis warstwy. Błąd usługi,
 * odpowiedź strony bez listy obiektów albo przekroczenie czasu daje IllegalStateException ze
 * ścieżką zapytania. Wspólny dla warstw planów budynków i warstw mapy kampusu.
 *
 * <p>Wymagania: US16, NFR-40</p>
 *
 * @since 0.1.0
 */
public final class ArcgisLayers {

    /**
     * Domyślny limit czasu połączenia i odczytu odpowiedzi usługi.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Klient HTTP.
     */
    private final RestClient client;

    /**
     * Parser odpowiedzi.
     */
    private final ObjectMapper mapper;

    /**
     * Tworzy klienta z domyślnym limitem czasu.
     *
     * @param mapper Parser odpowiedzi
     */
    public ArcgisLayers(final ObjectMapper mapper) {
        this(ArcgisLayers.TIMEOUT, mapper);
    }

    /**
     * Tworzy klienta z podanym limitem czasu połączenia i odczytu.
     *
     * @param timeout Limit czasu
     * @param mapper Parser odpowiedzi
     */
    public ArcgisLayers(final Duration timeout, final ObjectMapper mapper) {
        this(ArcgisLayers.http(timeout), mapper);
    }

    private ArcgisLayers(final RestClient client, final ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Wszystkie obiekty warstwy z wybranymi polami, bez geometrii.
     *
     * @param service Adres usługi FeatureServer
     * @param layer Numer warstwy
     * @param fields Pola obiektu, rozdzielone przecinkami
     * @return Obiekty GeoJSON ze wszystkich stron w kolejności usługi
     */
    public List<JsonNode> attributes(final String service, final int layer, final String fields) {
        return this.features(service, layer, fields, false);
    }

    /**
     * Wszystkie obiekty warstwy ze wszystkimi polami i geometrią.
     *
     * @param service Adres usługi FeatureServer
     * @param layer Numer warstwy
     * @return Obiekty GeoJSON ze wszystkich stron w kolejności usługi
     */
    public List<JsonNode> shapes(final String service, final int layer) {
        return this.features(service, layer, "*", true);
    }

    /**
     * Opis warstwy z usługi, m.in. z informacją o ostatniej edycji (editingInfo).
     *
     * @param service Adres usługi FeatureServer
     * @param layer Numer warstwy
     * @return Opis warstwy w formacie JSON usługi
     */
    public JsonNode description(final String service, final int layer) {
        return this.fetch(
            UriComponentsBuilder.fromUriString(service).pathSegment(String.valueOf(layer))
                .queryParam("f", "json").build().encode().toUri()
        );
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
        final JsonNode root = this.fetch(uri);
        if (!root.path("features").isArray()) {
            throw new IllegalStateException(uri.getPath());
        }
        return root;
    }

    private JsonNode fetch(final URI uri) {
        final JsonNode root;
        try {
            root = this.mapper.readTree(
                this.client.get().uri(uri).retrieve().requiredBody(byte[].class)
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
        if (root.has("error")) {
            throw new IllegalStateException(uri.getPath());
        }
        return root;
    }

    private static RestClient http(final Duration timeout) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(factory).build();
    }
}
