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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Eksport przyjętych barier w formacie CSV dla QGIS.
 *
 * <p>Wymagania: US13.2, US13.3, NFR-43, BR-6, BR-17, BR-22</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ExportCsvHttpIT {

    @Test
    void exportsAcceptedRowInPolishGrid(final WebApplicationContext context) throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final SubmissionContent content = SampleSubmissions.quick(
            new SubmissionPlace(
                new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1",
                new SubmissionFloor(null, 0, true)
            ),
            "Schody, \"próg\" i żółta poręcz", UUID.randomUUID()
        );
        final UUID accepted = samples.add("ACCEPTED", content);
        samples.add("ARCHIVED", content);
        samples.add("IN_REVIEW", content);
        final String[] lines = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/export.csv")
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
        ).andReturn().getResponse().getContentAsString().lines().toArray(String[]::new);
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
    void prefixesFormulaLikeDescriptionWithApostrophe(final WebApplicationContext context)
        throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final List<String> starts = Stream.of('=', '+', '-', '@', '\t', '\r')
            .map(String::valueOf).toList();
        for (final String start : starts) {
            samples.add(
                "ACCEPTED",
                SampleSubmissions.quick(
                    new SubmissionPlace(
                        new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1", null
                    ),
                    start.concat("1+1"), UUID.randomUUID()
                )
            );
        }
        final String csv = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/export.csv")
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
        ).andReturn().getResponse().getContentAsString();
        final List<String> descriptions = new ArrayList<>(starts.size());
        final CSVFormat format = CSVFormat.RFC4180.builder().setHeader()
            .setSkipHeaderRecord(true).get();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            parser.forEach(record -> descriptions.add(record.get("description")));
        }
        MatcherAssert.assertThat(
            "Opis zaczynający się od znaku formuły nie ma apostrofu na początku",
            descriptions,
            Matchers.containsInAnyOrder(
                starts.stream().map(start -> "'".concat(start).concat("1+1")).toArray()
            )
        );
    }

    private static String millimetres(final String metres) {
        return Long.toString(Math.round(Double.parseDouble(metres) * 1000));
    }
}
