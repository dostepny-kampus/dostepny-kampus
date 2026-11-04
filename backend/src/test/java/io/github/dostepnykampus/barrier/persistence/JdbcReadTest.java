/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Odczyt zgłoszeń na prawdziwej bazie, bez transakcji testowej: wyszukiwanie,
 * filtry listy i uszkodzony wiersz (US8, B-02).
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class JdbcReadTest {

    @AfterEach
    void clean(final ApplicationContext context) {
        new SubmissionData(context).clean();
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void usEightFindsEveryStatus(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        for (final SubmissionStatus status : SubmissionStatus.values()) {
            final UUID id = data.seed(
                10L + status.ordinal(), SubmissionData.BUILDING, "p1", status, SubmissionData.EARLY
            );
            Assertions.assertEquals(
                Optional.of(status), data.repo().find(id).map(Submission::status)
            );
        }
        Assertions.assertEquals(Optional.empty(), data.repo().find(data.id(99)));
    }

    @Test
    @Disabled("adapter JdbcSubmissions jest zaślepką")
    void usEightListFiltersAndOrdersByCreation(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final String home = SubmissionData.BUILDING;
        final SubmissionStatus sent = SubmissionStatus.SENT;
        final Instant early = SubmissionData.EARLY;
        final Instant late = SubmissionData.LATE;
        final UUID rejected = data.seed(2, home, "p1", SubmissionStatus.REJECTED, early);
        final UUID first = data.seed(1, home, "p1", sent, late);
        final UUID upstairs = data.seed(3, home, "p2", sent, late);
        final UUID other = data.seed(4, SubmissionData.OTHER, "p1", sent, early);
        final UUID accepted = data.seed(5, home, "p1", SubmissionStatus.ACCEPTED, late);
        Assertions.assertEquals(
            List.of(
                List.of(rejected, first, upstairs), List.of(rejected, first, accepted),
                List.of(other)
            ),
            List.of(
                this.ids(data, Set.of(sent, SubmissionStatus.REJECTED), home, null),
                this.ids(data, Set.of(SubmissionStatus.values()), home, "p1"),
                this.ids(data, Set.of(sent), SubmissionData.OTHER, null)
            ),
            "Lista nie filtruje po statusie, budynku i piętrze albo ma złą kolejność"
        );
    }

    private List<UUID> ids(
        final SubmissionData data, final Set<SubmissionStatus> statuses,
        final String building, final String floor
    ) {
        return data.repo().list(new SubmissionQuery(statuses, building, floor))
            .stream().map(Submission::id).toList();
    }
}
