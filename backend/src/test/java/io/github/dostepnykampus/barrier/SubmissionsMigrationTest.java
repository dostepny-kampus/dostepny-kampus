/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Testy BR-9 i BR-10: migracja V3 zakłada tabelę zgłoszeń z kluczem
 * nadanym w telefonie i wersją zaczynającą się od 1.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
final class SubmissionsMigrationTest {

    @Test
    void brNineKeysSubmissionsByPhoneId(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kluczem tabeli zgłoszeń nie jest identyfikator UUID z telefonu",
            SubmissionsMigrationTest.column(context, "id", "data_type"),
            Matchers.equalTo("uuid")
        );
    }

    @Test
    void brTenStartsAtFirstVersion(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Nowe zgłoszenie nie zaczyna od wersji 1",
            SubmissionsMigrationTest.column(context, "version", "column_default"),
            Matchers.equalTo("1")
        );
    }

    private static String column(
        final ApplicationContext context, final String name, final String property
    ) {
        return context.getBean(JdbcClient.class).sql(
            """
            SELECT %s FROM information_schema.columns
            WHERE table_name = 'submissions' AND column_name = :name
            """.formatted(property)
        ).param("name", name).query(String.class).single();
    }
}
