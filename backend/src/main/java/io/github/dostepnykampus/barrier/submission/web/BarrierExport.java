/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import java.io.Flushable;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Wspólne stronicowanie i formatowanie pól przy eksporcie przyjętych zgłoszeń.
 *
 * <p>Wymagania: US13.1, US13.2, US13.3</p>
 *
 * @since 0.1.0
 */
final class BarrierExport {

    /** Przyjęte zgłoszenia. */
    private final SubmissionReading submissions;

    BarrierExport(final SubmissionReading submissions) {
        this.submissions = submissions;
    }

    void write(final Writer writer, final Flushable flush) throws IOException {
        UUID after = SubmissionFinder.Page.START;
        boolean more;
        do {
            final SubmissionFinder.Page page = this.submissions.exportablePage(after);
            for (final Submission submission : page.items()) {
                writer.write(submission);
            }
            flush.flush();
            after = page.last();
            more = page.more();
        } while (more);
    }

    @Nullable String floor(
        final @Nullable String symbol, final @Nullable SubmissionFloor floor
    ) {
        @Nullable String result = null;
        if (floor != null) {
            result = this.submissions.floorLabel(symbol, floor.floorId())
                .orElseGet(() -> BarrierExport.number(floor));
        }
        return result;
    }

    private static @Nullable String number(final SubmissionFloor floor) {
        final String suffix;
        if (floor.mezzanine()) {
            suffix = " półpiętro";
        } else {
            suffix = "";
        }
        return Optional.ofNullable(floor.number())
            .map(number -> number + suffix).orElse(floor.floorId());
    }

    static @Nullable String group(final @Nullable BarrierGroup group) {
        return switch (group) {
            case ENTRANCE -> "ENTRANCE_AND_DOORS";
            case VERTICAL -> "VERTICAL_CIRCULATION";
            case HORIZONTAL -> "HORIZONTAL_CIRCULATION";
            case ORIENTATION -> "ORIENTATION_AND_INFORMATION";
            case null -> null;
            default -> group.name();
        };
    }

    static @Nullable Instant verifiedAt(final Submission submission) {
        final @Nullable Instant verified = submission.verification();
        final @Nullable Instant result;
        if (Instant.MIN.equals(verified)) {
            result = null;
        } else {
            result = verified;
        }
        return result;
    }

    /**
     * Zapis pojedynczego zgłoszenia.
     *
     * @since 0.1.0
     */
    @FunctionalInterface
    interface Writer {

        void write(Submission submission) throws IOException;
    }
}
