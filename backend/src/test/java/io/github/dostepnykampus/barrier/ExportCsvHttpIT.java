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
import org.junit.jupiter.api.AfterEach;
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

    /** Identyfikatory zgłoszeń próbnych do usunięcia po żądaniu HTTP. */
    private final List<UUID> inserted = new ArrayList<>(0);

    @AfterEach
    void removeSamples(final WebApplicationContext context) {
        if (!this.inserted.isEmpty()) {
            context.getBean(JdbcClient.class).sql("DELETE FROM submissions WHERE id IN (:ids)")
                .param("ids", this.inserted).update();
        }
    }

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
        this.add(samples, accepted, "ACCEPTED", content);
        this.add(samples, UUID.randomUUID(), "ARCHIVED", content);
        this.add(samples, UUID.randomUUID(), "IN_REVIEW", content);
        final String[] lines = ExportCsvHttpIT.get(port).body().lines().toArray(String[]::new);
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
        for (final String start : starts) {
            this.add(
                samples, UUID.randomUUID(), "ACCEPTED",
                SampleSubmissions.quick(
                    new SubmissionPlace(
                        new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1", null
                    ),
                    start.concat("1+1"), UUID.randomUUID()
                )
            );
        }
        final List<String> descriptions = ExportCsvHttpIT.column(
            ExportCsvHttpIT.get(port).body(), "description"
        );
        MatcherAssert.assertThat(
            "Opis zaczynający się od znaku formuły nie ma apostrofu na początku",
            descriptions,
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
        for (final UUID id : ids.reversed()) {
            this.add(samples, id, "ACCEPTED", content);
        }
        final HttpResponse<String> reply = ExportCsvHttpIT.get(port);
        MatcherAssert.assertThat(
            "Eksport podaje długość odpowiedzi z góry",
            reply.headers().firstValue(HttpHeaders.CONTENT_LENGTH).isEmpty(), Matchers.is(true)
        );
        MatcherAssert.assertThat(
            "Wiersze eksportu z więcej niż jednej strony w kolejności identyfikatorów",
            ExportCsvHttpIT.column(reply.body(), "submissionId"),
            Matchers.is(ids.stream().map(UUID::toString).toList())
        );
    }

    private void add(
        final SampleSubmissions samples, final UUID id, final String status,
        final SubmissionContent content
    ) {
        this.inserted.add(id);
        samples.add(id, status, content);
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
                MediaType.parseMediaType(reply.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                    .orElseThrow())
            ),
            Matchers.is(List.of(200, MediaType.parseMediaType("text/csv;charset=UTF-8")))
        );
        return reply;
    }

    private static String millimetres(final String metres) {
        return Long.toString(Math.round(Double.parseDouble(metres) * 1000));
    }
}
