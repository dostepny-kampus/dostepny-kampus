/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Plik eksportu pobrany przez Koordynatora: CSV albo GeoJSON jako wiersze po
 * identyfikatorze zgłoszenia. Wiersz GeoJSON to właściwości obiektu i jego
 * współrzędne w polach {@code lon} i {@code lat}.
 *
 * <p>Wymagania: US13.1, US13.2, US13.3</p>
 *
 * @param body Treść pliku
 * @param columns Kolumny CSV albo nazwy właściwości obiektów GeoJSON
 * @param rows Wiersze po identyfikatorze zgłoszenia
 * @param ownCrs Czy plik GeoJSON podaje własny układ współrzędnych w polu {@code crs}
 * @since 0.1.0
 */
record ExportFile(
    String body, List<String> columns, Map<String, Map<String, String>> rows, boolean ownCrs
) {

    /**
     * Pobiera eksport z serwera z tokenem Koordynatora i czyta go.
     *
     * @param port Port serwera
     * @param name Nazwa formatu: GeoJSON albo CSV
     * @param mapper Parser JSON
     * @return Odczytany plik
     * @throws Exception Gdy żądanie albo odczyt się nie uda
     */
    static ExportFile download(final String port, final String name, final ObjectMapper mapper)
        throws Exception {
        final HttpResponse<String> reply;
        try (HttpClient client = HttpClient.newHttpClient()) {
            reply = client.send(
                HttpRequest.newBuilder(
                    URI.create(
                        String.format(
                            "http://localhost:%s/export.%s", port, name.toLowerCase(Locale.ROOT)
                        )
                    )
                ).header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR").build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        }
        MatcherAssert.assertThat(
            "Serwer nie dał pliku eksportu ze statusem 200",
            reply.statusCode(), Matchers.equalTo(200)
        );
        final ExportFile file;
        if ("CSV".equals(name)) {
            file = ExportFile.csv(reply.body());
        } else {
            file = ExportFile.geoJson(reply.body(), mapper);
        }
        return file;
    }

    private static ExportFile csv(final String body) throws Exception {
        final Map<String, Map<String, String>> rows = new HashMap<>(4);
        final List<String> columns;
        try (
            CSVParser parser = CSVParser.parse(
                body, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get()
            )
        ) {
            columns = new ArrayList<>(parser.getHeaderNames());
            parser.forEach(row -> rows.put(row.get("submissionId"), row.toMap()));
        }
        return new ExportFile(body, columns, rows, false);
    }

    private static ExportFile geoJson(final String body, final ObjectMapper mapper) {
        final JsonNode root = mapper.readTree(body);
        MatcherAssert.assertThat(
            "Plik nie jest kolekcją obiektów GeoJSON",
            root.path("type").asString(), Matchers.equalTo("FeatureCollection")
        );
        final Map<String, Map<String, String>> rows = new HashMap<>(4);
        final Set<String> columns = new LinkedHashSet<>(8);
        for (final JsonNode feature : root.path("features")) {
            final Map<String, String> row = new HashMap<>(12);
            feature.path("properties").properties().forEach(
                entry -> {
                    columns.add(entry.getKey());
                    row.put(entry.getKey(), entry.getValue().asString());
                }
            );
            MatcherAssert.assertThat(
                "Obiekt GeoJSON nie jest punktem",
                feature.path("geometry").path("type").asString(), Matchers.equalTo("Point")
            );
            final JsonNode point = feature.path("geometry").path("coordinates");
            row.put("lon", point.path(0).asString());
            row.put("lat", point.path(1).asString());
            rows.put(row.get("submissionId"), row);
        }
        return new ExportFile(body, new ArrayList<>(columns), rows, root.has("crs"));
    }
}
