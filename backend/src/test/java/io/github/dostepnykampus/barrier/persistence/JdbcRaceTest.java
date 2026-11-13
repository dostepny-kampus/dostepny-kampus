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
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Dwie równoległe edycje tej samej wersji, kolejność wymuszona blokadą wiersza.
 *
 * <p>Wymagania: BR-10</p>
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
            final int holder;
            try (Statement select = lock.createStatement()) {
                select.execute(
                    String.format("SELECT 1 FROM submissions WHERE id = '%s' FOR UPDATE", id)
                );
                holder = JdbcRaceTest.session(select);
            }
            final List<Future<EditResult>> edits = List.of(
                pool.submit(this.edit(data, id, "a")), pool.submit(this.edit(data, id, "b"))
            );
            JdbcRaceTest.awaitBlocked(data, holder);
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

    private static int session(final Statement select) throws SQLException {
        try (ResultSet pid = select.executeQuery("SELECT pg_backend_pid()")) {
            if (pid.next()) {
                return pid.getInt(1);
            }
        }
        throw new IllegalStateException("Baza nie podała identyfikatora sesji");
    }

    private static void awaitBlocked(final SubmissionData data, final int holder) {
        final long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (data.jdbc().sql(
            """
            -- sesje czekające na blokadę sesji testu wprost albo za drugą edycją w kolejce
            -- do wiersza, bez sesji bazy spoza testu
            WITH RECURSIVE waiting(pid) AS (
                SELECT :holder
                UNION
                SELECT activity.pid FROM pg_stat_activity AS activity
                JOIN waiting ON waiting.pid = ANY(pg_blocking_pids(activity.pid))
            )
            SELECT count(*) - 1 FROM waiting
            """
        ).param("holder", holder).query(Integer.class).single() < 2) {
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
