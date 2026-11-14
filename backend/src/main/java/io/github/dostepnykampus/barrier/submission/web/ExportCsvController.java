/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.shared.geo.Puwg1992;
import io.github.dostepnykampus.shared.geo.Puwg1992Point;
import java.time.Instant;
import java.util.Optional;
import java.util.StringJoiner;
import org.apache.commons.csv.CSVFormat;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nagłówek i wiersze eksportu ze współrzędnymi w EPSG:2180.
 *
 * <p>Wymagania: US13.2, US13.3</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ExportCsvController {

    /** Przyjęte zgłoszenia. */
    private final SubmissionReading submissions;

    ExportCsvController(final SubmissionReading submissions) {
        this.submissions = submissions;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    ResponseEntity<String> csv() {
        final String separator = CSVFormat.RFC4180.getRecordSeparator();
        final StringJoiner csv = new StringJoiner(separator, "", separator).add(
            "submissionId,x,y,barrierType,barrierGroup,severity,buildingSymbol,floor,description,verifiedAt"
        );
        this.submissions.exportable().stream().map(this::row).forEach(csv::add);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv.toString());
    }

    private String row(final Submission submission) {
        final SubmissionPlace place = submission.content().place();
        final SubmissionPoint source = place.point();
        final Puwg1992Point point = new Puwg1992(source.longitude(), source.latitude()).point();
        final BarrierDetails barrier = submission.content().barrier();
        return CSVFormat.RFC4180.format(
            submission.id(), point.easting(), point.northing(),
            Optional.ofNullable(barrier.type()).map(BarrierType::code).orElse(""),
            Optional.ofNullable(barrier.group()).map(ExportCsvController::group).orElse(""),
            Optional.ofNullable(barrier.severity()).map(String::valueOf).orElse(""),
            Optional.ofNullable(place.buildingSymbol()).orElse(""),
            Optional.ofNullable(place.floor())
                .map(floor -> this.floor(place.buildingSymbol(), floor)).orElse(""),
            Optional.ofNullable(barrier.description()).orElse(""),
            Optional.of(submission.verification()).filter(time -> !Instant.MIN.equals(time))
                .map(Instant::toString).orElse("")
        );
    }

    private static String group(final BarrierGroup group) {
        return switch (group) {
            case ENTRANCE -> "ENTRANCE_AND_DOORS";
            case VERTICAL -> "VERTICAL_CIRCULATION";
            case HORIZONTAL -> "HORIZONTAL_CIRCULATION";
            case ORIENTATION -> "ORIENTATION_AND_INFORMATION";
            default -> group.name();
        };
    }

    private String floor(final @Nullable String symbol, final SubmissionFloor floor) {
        final String suffix;
        if (floor.mezzanine()) {
            suffix = " półpiętro";
        } else {
            suffix = "";
        }
        return this.submissions.floorLabel(symbol, floor.floorId()).orElseGet(
            () -> Optional.ofNullable(floor.number()).map(number -> number + suffix)
                .orElseGet(() -> Optional.ofNullable(floor.floorId()).orElse(""))
        );
    }
}
