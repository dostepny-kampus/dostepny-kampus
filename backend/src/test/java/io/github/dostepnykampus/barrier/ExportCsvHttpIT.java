/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Eksport przyjętych barier w formacie CSV dla QGIS.
 *
 * <p>Wymagania: US13.2, US13.3, NFR-43, BR-6, BR-17, BR-22</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ExportCsvHttpIT {

    @Test
    void exportsAcceptedRowInPolishGrid(
        final WebApplicationContext context, @LocalServerPort final int port
    ) throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final SubmissionContent content = SampleSubmissions.quick(
            new SubmissionPlace(
                new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1",
                new SubmissionFloor(null, 0, true)
            ),
            "Schody, \"próg\" i żółta poręcz", UUID.randomUUID()
        );
        final UUID accepted = UUID.randomUUID();
        final UUID archived = UUID.randomUUID();
        final UUID reviewing = UUID.randomUUID();
        final String[] lines;
        try {
            samples.add(accepted, "ACCEPTED", content);
            samples.add(archived, "ARCHIVED", content);
            samples.add(reviewing, "IN_REVIEW", content);
            lines = ExportCsvHttpIT.get(port).body().lines().toArray(String[]::new);
        } finally {
            ExportCsvHttpIT.remove(context, List.of(accepted, archived, reviewing));
        }
        final String[] row = lines[1].split(",", 4);
        MatcherAssert.assertThat(
            "Nagłówek, jedyny wiersz CSV i punkt według PROJ w milimetrach",
            List.of(
                lines[0], row[0], ExportCsvHttpIT.millimetres(row[1]),
                ExportCsvHttpIT.millimetres(row[2]), row[3], Integer.toString(lines.length)
            ),
            Matchers.contains(
                Matchers.is(
                    String.join(
                        ",", "submissionId", "x", "y", "barrierType", "barrierGroup", "severity",
                        "buildingSymbol", "floor", "description", "verifiedAt"
                    )
                ),
                Matchers.is(accepted.toString()),
                Matchers.is("364229180"), Matchers.is("362054214"),
                Matchers.matchesPattern(
                    ",OTHER,,A-1,0 półpiętro,\"Schody, \"\"próg\"\" i żółta poręcz\",.+Z"
                ),
                Matchers.is("2")
            )
        );
    }

    @Test
    void prefixesFormulaLikeDescriptionWithApostrophe(
        final WebApplicationContext context, @LocalServerPort final int port
    ) throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final List<String> starts = Stream.of('=', '+', '-', '@', '\t', '\r')
            .map(String::valueOf).toList();
        final List<UUID> ids = new ArrayList<>(starts.size());
        final String csv;
        try {
            for (final String start : starts) {
                final UUID id = UUID.randomUUID();
                ids.add(id);
                samples.add(
                    id, "ACCEPTED",
                    SampleSubmissions.quick(
                        new SubmissionPlace(
                            new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1", null
                        ),
                        start.concat("1+1"), UUID.randomUUID()
                    )
                );
            }
            csv = ExportCsvHttpIT.get(port).body();
        } finally {
            ExportCsvHttpIT.remove(context, ids);
        }
        MatcherAssert.assertThat(
            "Opis zaczynający się od znaku formuły nie ma apostrofu na początku",
            ExportCsvHttpIT.column(csv, "description"),
            Matchers.containsInAnyOrder(
                starts.stream().map(start -> "'".concat(start).concat("1+1")).toArray()
            )
        );
    }

    @Test
    void streamsAcceptedRowsAcrossPagesInIdOrder(
        final WebApplicationContext context, @LocalServerPort final int port
    ) throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final SubmissionContent content = SampleSubmissions.quick(UUID.randomUUID());
        final List<UUID> ids = IntStream.rangeClosed(1, 501)
            .mapToObj(number -> new UUID(1L, number)).toList();
        final HttpResponse<String> reply;
        try {
            for (final UUID id : ids.reversed()) {
                samples.add(id, "ACCEPTED", content);
            }
            reply = ExportCsvHttpIT.get(port);
        } finally {
            ExportCsvHttpIT.remove(context, ids);
        }
        MatcherAssert.assertThat(
            "Strumień CSV bez podanej długości i z wierszami w kolejności identyfikatorów",
            List.of(
                reply.headers().firstValue(HttpHeaders.CONTENT_LENGTH).isEmpty(),
                ExportCsvHttpIT.column(reply.body(), "submissionId")
            ),
            Matchers.is(List.of(true, ids.stream().map(UUID::toString).toList()))
        );
    }

    private static void remove(final WebApplicationContext context, final List<UUID> ids) {
        if (!ids.isEmpty()) {
            context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id IN (:ids)")
                .param("ids", ids).update();
        }
    }

    private static List<String> column(final String csv, final String name) throws Exception {
        final CSVFormat format = CSVFormat.RFC4180.builder().setHeader()
            .setSkipHeaderRecord(true).get();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            return parser.stream().map(record -> record.get(name)).toList();
        }
    }

    private static HttpResponse<String> get(final int port) throws Exception {
        final HttpResponse<String> reply;
        try (HttpClient client = HttpClient.newHttpClient()) {
            reply = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:%d/export.csv".formatted(port)))
                    .timeout(Duration.ofSeconds(20))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR").build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        }
        MatcherAssert.assertThat(
            "Status i typ treści eksportu",
            List.of(
                reply.statusCode(),
                MediaType.parseMediaType(
                    reply.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElseThrow()
                )
            ),
            Matchers.is(List.of(200, MediaType.parseMediaType("text/csv;charset=UTF-8")))
        );
        return reply;
    }

    private static String millimetres(final String metres) {
        return Long.toString(Math.round(Double.parseDouble(metres) * 1000));
    }
}
