/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Strażnik kluczy obcych w schemacie po wszystkich migracjach: klucz obcy nie wskazuje tabeli
 * innego modułu ani tabeli bez właściciela. Wyjątki mają numer issue, a ich lista tylko maleje.
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ModuleForeignKeyTest {

    /**
     * Klucze obce między modułami, które dziś istnieją, i issue, które je usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of("submissions -> buildings", 1662);

    @Test
    void foreignKeysStayInsideModuleOutsideExceptionList(final ApplicationContext context)
        throws IOException {
        final Set<String> found = ModuleForeignKeyTest.crossings(context);
        found.removeAll(ModuleForeignKeyTest.KNOWN.keySet());
        Assertions.assertEquals(
            Set.of(), found, "Klucz obcy wskazuje tabelę innego modułu albo tabelę bez właściciela"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingForeignKeys(final ApplicationContext context)
        throws IOException {
        final Set<String> stale = new TreeSet<>(ModuleForeignKeyTest.KNOWN.keySet());
        stale.removeAll(ModuleForeignKeyTest.crossings(context));
        Assertions.assertEquals(
            Set.of(), stale, "Klucza obcego z listy wyjątków już nie ma; usuń wpis"
        );
    }

    private static Set<String> crossings(final ApplicationContext context) throws IOException {
        return ModuleMigrationTest.crossings(
            ModuleMigrationTest.owners(),
            context.getBean(JdbcClient.class).sql(
                String.join(
                    " ",
                    "SELECT conrelid::regclass::text AS source, confrelid::regclass::text AS target",
                    "FROM pg_constraint WHERE contype = 'f'"
                )
            ).query((row, index) -> List.of(row.getString("source"), row.getString("target")))
                .list()
        );
    }
}
