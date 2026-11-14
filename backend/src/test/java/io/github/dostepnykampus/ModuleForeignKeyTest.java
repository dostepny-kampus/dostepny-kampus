/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Strażnik kluczy obcych w schemacie po wszystkich migracjach: klucz obcy nie wskazuje tabeli
 * innego modułu ani tabeli bez właściciela.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ModuleForeignKeyTest {

    @Test
    void foreignKeysStayInsideModule(final ApplicationContext context) throws IOException {
        Assertions.assertEquals(
            Set.of(), ModuleForeignKeyTest.crossings(context),
            "Klucz obcy wskazuje tabelę innego modułu albo tabelę bez właściciela"
        );
    }

    private static Set<String> crossings(final ApplicationContext context) throws IOException {
        return ModuleMigrationTest.crossings(
            ModuleMigrationTest.owners(),
            context.getBean(JdbcClient.class).sql(
                String.join(
                    " ",
                    "SELECT conrelid::regclass::text AS source, confrelid::regclass::text AS target",
                    "FROM pg_constraint WHERE contype = 'f'",
                    "AND connamespace = current_schema()::regnamespace"
                )
            ).query((row, index) -> List.of(row.getString("source"), row.getString("target")))
                .list()
        );
    }
}
