/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.dostepnykampus.building.path.AccessiblePath;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Klient usługi ArcGIS zamienia wymyśloną odpowiedź warstwy ścieżek dostosowanych na odcinki
 * z identyfikatorem GlobalID i linią GeoJSON.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
final class ArcgisPathsTest {

    /**
     * Linia z dwoma punktami.
     */
    private static final String LINE = "[[17.1,51.1],[17.2,51.2]]";

    @Test
    void readsGlobalIdAndLine() throws IOException {
        MatcherAssert.assertThat(
            "Odcinek nie ma identyfikatora GlobalID albo linii z odpowiedzi usługi",
            ArcgisPathsTest.read(
                ArcgisPathsTest.feature("\"{AB-1}\"", "LineString", ArcgisPathsTest.LINE)
            ),
            Matchers.contains(
                new AccessiblePath(
                    "{AB-1}",
                    String.format(
                        "{\"type\":\"LineString\",\"coordinates\":%s}", ArcgisPathsTest.LINE
                    )
                )
            )
        );
    }

    @Test
    void skipsFeatureWithoutIdOrTwoPoints() throws IOException {
        MatcherAssert.assertThat(
            "Obiekt bez GlobalID, bez linii albo z linią krótszą niż dwa punkty dał odcinek",
            ArcgisPathsTest.read(
                ArcgisPathsTest.feature("null", "LineString", ArcgisPathsTest.LINE),
                ArcgisPathsTest.feature("\" \"", "LineString", ArcgisPathsTest.LINE),
                ArcgisPathsTest.feature("\"B\"", "LineString", "[[17.1,51.1]]"),
                "{\"geometry\":null,\"properties\":{\"GlobalID\":\"C\"}}"
            ),
            Matchers.<AccessiblePath>empty()
        );
    }

    @Test
    void splitsMultiLineIntoNumberedPaths() throws IOException {
        MatcherAssert.assertThat(
            "Części MultiLineString nie dały osobnych odcinków z numerem części",
            ArcgisPathsTest.read(
                ArcgisPathsTest.feature(
                    "\"M\"", "MultiLineString",
                    String.format(
                        "[%s,[[17.3,51.3]],%s]", ArcgisPathsTest.LINE, ArcgisPathsTest.LINE
                    )
                )
            ).stream().map(AccessiblePath::id).toList(),
            Matchers.contains("M/1", "M/3")
        );
    }

    private static String feature(final String id, final String type, final String coordinates) {
        return String.format(
            "{\"geometry\":{\"type\":\"%s\",\"coordinates\":%s},\"properties\":{\"GlobalID\":%s}}",
            type, coordinates, id
        );
    }

    private static List<AccessiblePath> read(final String... features) throws IOException {
        final byte[] body = String.format("{\"features\":[%s]}", String.join(",", features))
            .getBytes(StandardCharsets.UTF_8);
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
            "/FeatureServer/2/query", exchange -> ArcgisPathsTest.send(exchange, body)
        );
        server.start();
        try {
            return new ArcgisPaths(JsonMapper.builder().build()).paths(
                String.format("http://localhost:%d/FeatureServer", server.getAddress().getPort()), 2
            );
        } finally {
            server.stop(0);
        }
    }

    private static void send(final HttpExchange exchange, final byte[] body) throws IOException {
        try (exchange) {
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
