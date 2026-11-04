/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Dwie równoległe edycje tej samej wersji, kolejność wymuszona blokadą wiersza (BR-10).
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcRaceTest {

    @Test
    void brTenSameVersionRaceConflicts(final ApplicationContext context) throws Exception {
        final SubmissionData data = SubmissionData.of(context);
        final UUID id = data.seed(SubmissionStatus.SENT);
        final List<EditResult> results = new ArrayList<>(2);
        try (
            ExecutorService pool = Executors.newFixedThreadPool(2);
            Connection lock = context.getBean(DataSource.class).getConnection()
        ) {
            lock.setAutoCommit(false);
            try (Statement select = lock.createStatement()) {
                select.execute(
                    String.format("SELECT 1 FROM submissions WHERE id = '%s' FOR UPDATE", id)
                );
            }
            final List<Future<EditResult>> edits = List.of(
                pool.submit(this.edit(data, id, "a")), pool.submit(this.edit(data, id, "b"))
            );
            this.awaitBlocked(data);
            lock.commit();
            for (final Future<EditResult> edit : edits) {
                results.add(edit.get(20, TimeUnit.SECONDS));
            }
        } finally {
            data.clean();
        }
        final EditResult saved = results.stream()
            .filter(EditResult.Saved.class::isInstance).findFirst().orElseThrow();
        Assertions.assertEquals(
            Set.of(saved, new EditResult.Conflict(((EditResult.Saved) saved).submission())),
            Set.copyOf(results),
            "Jedna edycja ma się zapisać, a druga dostać konflikt z zapisanym stanem"
        );
    }

    private void awaitBlocked(final SubmissionData data) {
        final long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (data.jdbc().sql(
            "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'"
        ).query(Integer.class).single() < 2) {
            Assertions.assertTrue(System.nanoTime() < limit, "Edycje nie czekają na blokadę");
            Thread.onSpinWait();
        }
    }

    private Callable<EditResult> edit(
        final SubmissionData data, final UUID id, final String text
    ) {
        return () -> data.repo().update(new SubmissionEdit(id, 1, SubmissionData.content(text)));
    }
}
