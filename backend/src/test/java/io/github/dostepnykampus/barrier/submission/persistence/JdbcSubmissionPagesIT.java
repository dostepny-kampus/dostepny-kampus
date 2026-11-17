/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Strony listy zgłoszeń na prawdziwej bazie: kursor, limit, kolejność identyfikatorów
 * i zgłoszenia jednego autora.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JdbcSubmissionPagesIT {

    @AfterEach
    void clean(final ApplicationContext context) {
        SubmissionData.of(context).clean();
    }

    @Test
    void readsPagesAfterCursorInIdOrder(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final SubmissionContent content = SubmissionData.content("o");
        data.seed(3, content, SubmissionStatus.SENT);
        data.seed(1, content, SubmissionStatus.SENT);
        data.seed(2, content, SubmissionStatus.SENT);
        final SubmissionFinder.Pages pages = context.getBean(SubmissionFinder.Pages.class);
        final SubmissionQuery query = new SubmissionQuery(
            Set.of(SubmissionStatus.SENT), SubmissionData.BUILDING, null
        );
        final SubmissionFinder.Page first = pages.page(
            query, new SubmissionFinder.Window(SubmissionFinder.Page.START, 2)
        );
        final SubmissionFinder.Page second = pages.page(
            query, new SubmissionFinder.Window(first.last(), 2)
        );
        Assertions.assertEquals(
            List.of(
                List.of(SubmissionData.id(1), SubmissionData.id(2)), true,
                List.of(SubmissionData.id(3)), false
            ),
            List.of(
                first.items().stream().map(Submission::id).toList(), first.more(),
                second.items().stream().map(Submission::id).toList(), second.more()
            ),
            "Strony nie idą po kursorze w kolejności id albo źle mówią o dalszej stronie"
        );
    }

    @Test
    void leavesOtherAuthorsOutOfOwnPage(final ApplicationContext context) {
        final SubmissionData data = SubmissionData.of(context);
        final SubmissionContent content = SubmissionData.content("o");
        data.seed(1, content, SubmissionStatus.REJECTED);
        data.seed(2, content, SubmissionStatus.SENT);
        data.jdbc().sql("UPDATE submissions SET author = 'inne-konto' WHERE id = :id")
            .param("id", SubmissionData.id(2)).update();
        Assertions.assertEquals(
            List.of(SubmissionData.id(1)),
            context.getBean(SubmissionFinder.Pages.class).own(
                SubmissionData.AUTHOR,
                new SubmissionQuery(
                    Set.of(SubmissionStatus.values()), SubmissionData.BUILDING, null
                ),
                new SubmissionFinder.Window(SubmissionFinder.Page.START, 500)
            ).items().stream().map(Submission::id).toList(),
            "Strona własnych zgłoszeń ma cudze albo gubi własne"
        );
    }
}
