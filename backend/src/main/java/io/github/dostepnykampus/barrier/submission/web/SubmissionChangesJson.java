/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.api.ChangedSubmission;
import io.github.dostepnykampus.barrier.api.SubmissionChanges;
import io.github.dostepnykampus.barrier.submission.SubmissionChange;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.shared.json.RawJson;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Zgłoszenia zmienione od kursora dla innych modułów, zapisane tą samą odpowiedzią co
 * {@code GET /submissions/{id}}, więc mają kształt schematu Submission kontraktu.
 *
 * <p>Wymagania: US8.1, US1.3, US10.6</p>
 *
 * @since 0.1.0
 */
@Component
public final class SubmissionChangesJson implements SubmissionChanges {

    /** Odczyt zgłoszeń. */
    private final SubmissionReading reading;

    /** Zapis odpowiedzi w JSON. */
    private final ObjectMapper mapper;

    /**
     * Tworzy odczyt zmian.
     *
     * @param reading Odczyt zgłoszeń
     * @param mapper Zapis odpowiedzi w JSON
     */
    public SubmissionChangesJson(final SubmissionReading reading, final ObjectMapper mapper) {
        this.reading = reading;
        this.mapper = mapper;
    }

    @Override
    public List<ChangedSubmission> changedSince(final Instant since, final String reader) {
        return this.reading.changedSince(since, reader).stream().map(this::changed).toList();
    }

    private ChangedSubmission changed(final SubmissionChange change) {
        return new ChangedSubmission(
            change.changed(),
            new RawJson(
                this.mapper.writeValueAsString(new SubmissionResponse(change.submission()))
            )
        );
    }
}
