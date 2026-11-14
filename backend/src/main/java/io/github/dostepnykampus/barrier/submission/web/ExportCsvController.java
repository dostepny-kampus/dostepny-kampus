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
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.barrier.submission.content.json.GroupJson;
import io.github.dostepnykampus.building.api.FloorLabels;
import io.github.dostepnykampus.shared.geo.Puwg1992;
import io.github.dostepnykampus.shared.geo.Puwg1992Point;
import java.time.Instant;
import java.util.Optional;
import java.util.StringJoiner;
import org.apache.commons.csv.CSVFormat;
import org.springframework.http.HttpHeaders;
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
final class ExportCsvController {

    /** Przyjęte zgłoszenia. */
    private final SubmissionReading submissions;

    /** Etykiety pięter identyfikowanych przez plan. */
    private final FloorLabels floors;

    ExportCsvController(final SubmissionReading submissions, final FloorLabels floors) {
        this.submissions = submissions;
        this.floors = floors;
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    ResponseEntity<String> csv() {
        final StringJoiner csv = new StringJoiner("\r\n", "", "\r\n").add(
            "submissionId,x,y,barrierType,barrierGroup,severity,buildingSymbol,floor,description,verifiedAt"
        );
        this.submissions.exportable().stream().map(this::row).forEach(csv::add);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.csv")
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
            Optional.ofNullable(barrier.group()).map(GroupJson::of).map(GroupJson::name).orElse(""),
            Optional.ofNullable(barrier.severity()).map(String::valueOf).orElse(""),
            Optional.ofNullable(place.buildingSymbol()).orElse(""), this.floor(place),
            Optional.ofNullable(barrier.description()).orElse(""),
            Optional.ofNullable(submission.verifiedAt()).map(Instant::toString).orElse("")
        );
    }

    private String floor(final SubmissionPlace place) {
        final SubmissionFloor floor = place.floor();
        if (floor == null) {
            return "";
        }
        return this.floors.label(place.buildingSymbol(), floor.floorId()).orElseGet(
            () -> Optional.ofNullable(floor.number())
                .map(number -> String.format("%d%s", number, floor.mezzanine() ? " półpiętro" : ""))
                .orElseGet(() -> Optional.ofNullable(floor.floorId()).orElse(""))
        );
    }
}
