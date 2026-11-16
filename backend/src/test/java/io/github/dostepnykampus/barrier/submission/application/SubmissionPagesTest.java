/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Zgłoszenia ze wszystkich stron listy, czytane po kolei: następna strona dopiero wtedy,
 * gdy odbiorca przeczyta poprzednią.
 *
 * @since 0.1.0
 */
final class SubmissionPagesTest {

    @Test
    void readsNextPageOnlyAfterPreviousIsConsumed() {
        final Submission first = SubmissionPagesTest.submission();
        final Submission second = SubmissionPagesTest.submission();
        final List<String> asked = new ArrayList<>(2);
        final Iterator<Submission> rows = new SubmissionPages(
            after -> {
                asked.add(String.valueOf(after));
                final SubmissionFinder.Page page;
                if (after.equals(SubmissionFinder.Page.START)) {
                    page = new SubmissionFinder.Page(List.of(first), first.id(), true);
                } else {
                    page = new SubmissionFinder.Page(List.of(second), second.id(), false);
                }
                return page;
            }
        ).stream().iterator();
        rows.next();
        final List<String> before = List.copyOf(asked);
        rows.next();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                List.of(SubmissionFinder.Page.START.toString()), before, "Druga strona przyszła przed przeczytaniem pierwszej"
            ),
            () -> Assertions.assertEquals(
                List.of(SubmissionFinder.Page.START.toString(), first.id().toString()), asked,
                "Druga strona nie zaczęła się po kursorze pierwszej"
            ),
            () -> Assertions.assertFalse(rows.hasNext(), "Po ostatniej stronie są dalsze wiersze")
        );
    }

    private static Submission submission() {
        return new Submission(
            UUID.randomUUID(), 1, SubmissionStatus.ACCEPTED, Instant.parse("2026-11-01T10:00:00Z"),
            new SubmissionContent(
                SubmissionKind.QUICK,
                new SubmissionPlace(new SubmissionPoint(17.06, 51.1), null, false, null, null),
                new BarrierDetails(BarrierGroup.OTHER, null, 2, "opis", false),
                null, null, null, List.of(UUID.randomUUID())
            )
        );
    }
}
