/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.shared.geo.Puwg1992;
import io.github.dostepnykampus.shared.geo.Puwg1992Point;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Nagłówek i wiersze eksportu ze współrzędnymi w EPSG:2180. Opis zaczynający się od
 * {@code =}, {@code +}, {@code -}, {@code @}, tabulatora albo powrotu karetki dostaje na
 * początku apostrof, więc arkusz kalkulacyjny pokazuje go jako tekst, nie formułę.
 *
 * <p>Wymagania: US13.2, US13.3</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ExportCsvController {

    /**
     * Pierwsze znaki, od których arkusz kalkulacyjny zaczyna formułę.
     */
    private static final Set<Character> FORMULA = Set.of('=', '+', '-', '@', '\t', '\r');

    /** Format i nagłówek eksportu. */
    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder().setHeader(
        "submissionId", "x", "y", "barrierType", "barrierGroup", "severity",
        "buildingSymbol", "floor", "description", "verifiedAt"
    ).get();

    /** Wspólne stronicowanie i formatowanie eksportu. */
    private final BarrierExport export;

    ExportCsvController(final SubmissionReading submissions) {
        this.export = new BarrierExport(submissions);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    ResponseEntity<StreamingResponseBody> csv() {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(this::write);
    }

    private void write(final OutputStream output) throws IOException {
        try (
            CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(StreamUtils.nonClosing(output), StandardCharsets.UTF_8),
                ExportCsvController.FORMAT
            )
        ) {
            this.export.write(submission -> this.printRow(printer, submission), printer);
        }
    }

    private void printRow(final CSVPrinter printer, final Submission submission)
        throws IOException {
        final SubmissionPlace place = submission.content().place();
        final SubmissionPoint source = place.point();
        final Puwg1992Point point = new Puwg1992(source.longitude(), source.latitude()).point();
        final BarrierDetails barrier = submission.content().barrier();
        printer.printRecord(
            submission.id(), point.easting(), point.northing(),
            Optional.ofNullable(barrier.type()).map(BarrierType::code).orElse(""),
            Objects.requireNonNullElse(BarrierExport.group(barrier.group()), ""),
            Optional.ofNullable(barrier.severity()).map(String::valueOf).orElse(""),
            Optional.ofNullable(place.buildingSymbol()).orElse(""),
            Objects.requireNonNullElse(
                this.export.floor(place.buildingSymbol(), place.floor()), ""
            ),
            Optional.ofNullable(barrier.description()).map(ExportCsvController::text).orElse(""),
            Optional.ofNullable(BarrierExport.verifiedAt(submission))
                .map(Instant::toString).orElse("")
        );
    }

    private static String text(final String value) {
        String result = value;
        if (!value.isEmpty() && ExportCsvController.FORMULA.contains(value.charAt(0))) {
            result = "'".concat(value);
        }
        return result;
    }
}
