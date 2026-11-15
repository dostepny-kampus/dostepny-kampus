/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Warstwa usługi ArcGIS jest pobierana stronami od przesunięcia 0 do pustej strony albo strony
 * bez znacznika przekroczenia limitu.
 *
 * <p>Wymagania: US16</p>
 *
 * @since 0.1.0
 */
final class ArcgisLayersTest {

    @Test
    void readsEveryPageOfLayer() throws IOException {
        MatcherAssert.assertThat(
            "Warstwa nie ma obiektów ze wszystkich stron albo nie kończy się na pustej",
            ArcgisLayersTest.features(ArcgisLayersTest::page).stream()
                .map(feature -> feature.path("id").asInt()).toList(),
            Matchers.contains(1, 2, 3)
        );
    }

    private static List<JsonNode> features(final HttpHandler handler) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/FeatureServer/3/query", handler);
        server.start();
        try {
            return new ArcgisLayers(JsonMapper.builder().build()).attributes(
                String.format("http://localhost:%d/FeatureServer", server.getAddress().getPort()),
                3, "id"
            );
        } finally {
            server.stop(0);
        }
    }

    private static void page(final HttpExchange exchange) throws IOException {
        final String query = exchange.getRequestURI().getQuery();
        String features = "";
        if (query.contains("resultOffset=0")) {
            features = "{\"id\":1},{\"id\":2}";
        } else if (query.contains("resultOffset=2")) {
            features = "{\"id\":3}";
        }
        ArcgisLayersTest.send(
            exchange,
            String.format(
                "{\"features\":[%s],\"properties\":{\"exceededTransferLimit\":true}}", features
            )
        );
    }

    private static void send(final HttpExchange exchange, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (exchange) {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
