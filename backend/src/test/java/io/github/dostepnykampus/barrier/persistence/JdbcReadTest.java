/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Odczyt zgłoszeń na prawdziwej bazie, bez transakcji testowej: wyszukiwanie,
 * filtry listy i uszkodzony wiersz (B-02).
 *
 * <p>Wymagania: US8</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcReadTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void usEightFindsEveryStatus(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        for (final SubmissionStatus status : SubmissionStatus.values()) {
            Assertions.assertEquals(
                Optional.of(status), data.repo().find(data.seed(status)).map(Submission::status)
            );
        }
    }

    @Test
    void usEightListFiltersAndOrdersByCreation(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final String home = SubmissionData.BUILDING;
        final SubmissionStatus sent = SubmissionStatus.SENT;
        final SubmissionContent ground = SubmissionData.content(home, "p1", "o");
        final UUID upper = JdbcReadTest.created(
            data, data.seed(1, SubmissionData.content(home, "p2", "o"), sent), 2
        );
        final UUID accepted = JdbcReadTest.created(
            data, data.seed(5, ground, SubmissionStatus.ACCEPTED), 3
        );
        final UUID other = JdbcReadTest.created(
            data, data.seed(4, SubmissionData.content(SubmissionData.OTHER, "p1", "o"), sent), 4
        );
        final UUID first = JdbcReadTest.created(data, data.seed(3, ground, sent), 0);
        final UUID rejected = JdbcReadTest.created(
            data, data.seed(2, ground, SubmissionStatus.REJECTED), 1
        );
        Assertions.assertEquals(
            List.of(
                List.of(first, rejected, upper),
                List.of(first, rejected, accepted),
                List.of(other)
            ),
            List.of(
                JdbcReadTest.ids(
                    data, new SubmissionQuery(Set.of(sent, SubmissionStatus.REJECTED), home, null)
                ),
                JdbcReadTest.ids(
                    data, new SubmissionQuery(Set.of(SubmissionStatus.values()), home, "p1")
                ),
                JdbcReadTest.ids(
                    data, new SubmissionQuery(Set.of(sent), SubmissionData.OTHER, null)
                )
            ),
            "Lista nie filtruje po statusie, budynku i piętrze albo ma złą kolejność"
        );
    }

    @Test
    void usEightListOrdersByCreationThenId(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final SubmissionContent content = SubmissionData.content("o");
        final SubmissionStatus sent = SubmissionStatus.SENT;
        JdbcReadTest.created(data, data.seed(3, content, sent), 2);
        JdbcReadTest.created(data, data.seed(1, content, sent), 3);
        JdbcReadTest.created(data, data.seed(2, content, sent), 2);
        JdbcReadTest.created(data, data.seed(4, content, sent), 0);
        Assertions.assertEquals(
            List.of(
                SubmissionData.id(4), SubmissionData.id(2), SubmissionData.id(3),
                SubmissionData.id(1)
            ),
            JdbcReadTest.ids(
                data, new SubmissionQuery(Set.of(sent), SubmissionData.BUILDING, null)
            ),
            "Lista nie jest posortowana po chwili utworzenia, a przy równych chwilach po id"
        );
    }

    @Test
    void usEightOwnListLeavesOtherAuthorsOut(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final SubmissionContent content = SubmissionData.content("o");
        final UUID own = data.seed(1, content, SubmissionStatus.REJECTED);
        final UUID foreign = data.seed(2, content, SubmissionStatus.SENT);
        data.jdbc().sql("UPDATE submissions SET author = 'inne-konto' WHERE id = :id")
            .param("id", foreign).update();
        final SubmissionQuery all = new SubmissionQuery(
            Set.of(SubmissionStatus.values()), SubmissionData.BUILDING, null
        );
        Assertions.assertEquals(
            List.of(List.of(own), List.of(foreign)),
            List.of(
                data.repo().own(SubmissionData.AUTHOR, all).stream().map(Submission::id).toList(),
                data.repo().own("inne-konto", all).stream().map(Submission::id).toList()
            ),
            "Lista własnych zgłoszeń ma cudze albo gubi własne"
        );
    }

    private static UUID created(final SubmissionData data, final UUID id, final int minutes) {
        data.jdbc().sql("UPDATE submissions SET created_at = :at WHERE id = :id")
            .param("at", OffsetDateTime.parse("2026-01-01T10:00:00Z").plusMinutes(minutes))
            .param("id", id).update();
        return id;
    }

    private static List<UUID> ids(final SubmissionData data, final SubmissionQuery query) {
        return data.repo().list(query).stream().map(Submission::id).toList();
    }
}
