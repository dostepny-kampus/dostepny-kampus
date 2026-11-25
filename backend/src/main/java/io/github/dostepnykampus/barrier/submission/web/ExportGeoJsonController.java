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
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

/**
 * Strumieniowy eksport bieżących przyjętych zgłoszeń jako kolekcja GeoJSON (WGS84)
 * w UTF-8, bez danych autora i bez geometrii budynków.
 *
 * <p>Wymagania: US13.1, US13.3, BR-6, BR-22, NFR-38, NFR-43</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ExportGeoJsonController {

    /** Wspólne stronicowanie i formatowanie eksportu. */
    private final BarrierExport export;

    /** Tworzenie strumienia JSON w UTF-8. */
    private final ObjectMapper mapper;

    ExportGeoJsonController(final SubmissionReading submissions, final ObjectMapper mapper) {
        this.export = new BarrierExport(submissions);
        this.mapper = mapper;
    }

    @GetMapping(value = "/export.geojson", produces = "application/geo+json;charset=UTF-8")
    ResponseEntity<StreamingResponseBody> geoJson() {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/geo+json;charset=UTF-8"))
            .body(this::write);
    }

    private void write(final OutputStream output) throws IOException {
        try (
            JsonGenerator out = this.mapper.createGenerator(
                StreamUtils.nonClosing(output), JsonEncoding.UTF8
            )
        ) {
            out.writeStartObject();
            out.writeStringProperty("type", "FeatureCollection");
            out.writeArrayPropertyStart("features");
            this.export.write(item -> this.writeFeature(out, item), out);
            out.writeEndArray();
            out.writeEndObject();
        }
    }

    private void writeFeature(final JsonGenerator out, final Submission submission) {
        final SubmissionPlace place = submission.content().place();
        final SubmissionPoint point = place.point();
        final BarrierDetails barrier = submission.content().barrier();
        out.writeStartObject();
        out.writeStringProperty("type", "Feature");
        out.writeObjectPropertyStart("geometry");
        out.writeStringProperty("type", "Point");
        out.writeArrayPropertyStart("coordinates");
        out.writeNumber(point.longitude());
        out.writeNumber(point.latitude());
        out.writeEndArray();
        out.writeEndObject();
        out.writeObjectPropertyStart("properties");
        out.writeStringProperty("submissionId", submission.id().toString());
        ExportGeoJsonController.prop(out, "barrierType",
            Optional.ofNullable(barrier.type()).map(BarrierType::code).orElse(null));
        ExportGeoJsonController.prop(out, "barrierGroup", BarrierExport.group(barrier.group()));
        final @Nullable Integer severity = barrier.severity();
        if (severity != null) {
            out.writeNumberProperty("severity", severity);
        }
        ExportGeoJsonController.prop(out, "buildingSymbol", place.buildingSymbol());
        ExportGeoJsonController.prop(
            out, "floor", this.export.floor(place.buildingSymbol(), place.floor())
        );
        ExportGeoJsonController.prop(out, "description", barrier.description());
        ExportGeoJsonController.prop(out, "verifiedAt",
            Optional.ofNullable(BarrierExport.verifiedAt(submission))
                .map(Instant::toString).orElse(null));
        out.writeEndObject();
        out.writeEndObject();
    }

    private static void prop(
        final JsonGenerator out, final String name, final @Nullable String value
    ) {
        if (value != null) {
            out.writeStringProperty(name, value);
        }
    }
}
