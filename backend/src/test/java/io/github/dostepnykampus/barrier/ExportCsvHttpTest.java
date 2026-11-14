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
import java.util.List;
import java.util.UUID;
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
final class ExportCsvHttpTest {

    @Test
    void exportsOnlyAcceptedRowWithPolishText(final WebApplicationContext context)
        throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final UUID accepted = samples.add("ACCEPTED", ExportCsvHttpTest.content());
        samples.add("ARCHIVED", ExportCsvHttpTest.content());
        samples.add("IN_REVIEW", ExportCsvHttpTest.content());
        final String[] lines = ExportCsvHttpTest.lines(context);
        final String[] point = lines[1].split(",", 4);
        MatcherAssert.assertThat(
            "Nagłówek i jedyny wiersz CSV",
            List.of(lines[0], point[0], point[3], Integer.toString(lines.length)),
            Matchers.contains(
                Matchers.is(
                    String.join(
                        ",", "submissionId", "x", "y", "barrierType", "barrierGroup", "severity",
                        "buildingSymbol", "floor", "description", "verifiedAt"
                    )
                ),
                Matchers.is(accepted.toString()),
                Matchers.matchesPattern(
                    ",OTHER,,A-1,0 półpiętro,\"Schody, \"\"próg\"\" i żółta poręcz\",.+Z"
                ),
                Matchers.is("2")
            )
        );
    }

    @Test
    void exportsPointInPuwg1992(final WebApplicationContext context) throws Exception {
        new SampleSubmissions(context).add("ACCEPTED", ExportCsvHttpTest.content());
        final String[] point = ExportCsvHttpTest.lines(context)[1].split(",", 4);
        MatcherAssert.assertThat(
            "Punkt według PROJ",
            List.of(Double.parseDouble(point[1]), Double.parseDouble(point[2])),
            Matchers.contains(
                Matchers.closeTo(364_229.179_6, 0.001), Matchers.closeTo(362_054.214_1, 0.001)
            )
        );
    }

    private static SubmissionContent content() {
        return SampleSubmissions.quick(
            new SubmissionPlace(
                new SubmissionPoint(17.059_89, 51.109_16), null, false, "A-1",
                new SubmissionFloor(null, 0, true)
            ),
            "Schody, \"próg\" i żółta poręcz", UUID.randomUUID()
        );
    }

    private static String[] lines(final WebApplicationContext context) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/export.csv")
                .header(HttpHeaders.AUTHORIZATION, "Bearer COORDINATOR")
        ).andReturn().getResponse().getContentAsString().lines().toArray(String[]::new);
    }
}
