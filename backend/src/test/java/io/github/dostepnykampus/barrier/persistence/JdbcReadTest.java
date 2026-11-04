/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
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
 * filtry listy i uszkodzony wiersz (US8, B-02).
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
        final SubmissionContent upper = SubmissionData.content(home, "p2", "o");
        final SubmissionContent other = SubmissionData.content(SubmissionData.OTHER, "p1", "o");
        final UUID first = data.seed(3, ground, sent);
        final UUID rejected = data.seed(2, ground, SubmissionStatus.REJECTED);
        Assertions.assertEquals(
            List.of(
                List.of(first, rejected, data.seed(1, upper, sent)),
                List.of(first, rejected, data.seed(5, ground, SubmissionStatus.ACCEPTED)),
                List.of(data.seed(4, other, sent))
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

    private static List<UUID> ids(final SubmissionData data, final SubmissionQuery query) {
        return data.repo().list(query).stream().map(Submission::id).toList();
    }
}
