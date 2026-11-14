/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Strażnik migracji modułów serwera. Migracja modułu leży w {@code db/migration/<moduł>}
 * i ma własną historię Flyway; {@code db/migration/__root} zawiera tylko migracje sprzed
 * podziału na moduły. Tabela należy do modułu, w którego katalogu powstała; tabele z
 * {@code __root} mają właściciela na stałej liście. Klucz obcy nie wskazuje tabeli innego
 * modułu; wyjątki mają numer issue, a ich lista tylko maleje.
 *
 * @since 0.1.0
 */
final class ModuleMigrationTest {

    /**
     * Katalog migracji SQL.
     */
    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    /**
     * Katalog migracji wspólnych.
     */
    private static final String ROOT = "__root";

    /**
     * Migracje sprzed podziału na moduły, już wykonane na istniejących bazach.
     */
    private static final List<String> BEFORE_MODULES = List.of(
        "V1__budynki.sql", "V2__wejscia.sql", "V3__zgloszenia.sql",
        "V4__nazwa_budynku_opcjonalna.sql", "V5__zgloszenie_jedno_zrodlo.sql",
        "V6__autor_zgloszenia.sql"
    );

    /**
     * Właściciele tabel utworzonych w migracjach sprzed podziału.
     */
    private static final Map<String, String> ROOT_OWNERS = Map.of(
        "buildings", "building", "entrances", "building", "submissions", "barrier"
    );

    /**
     * Klucze obce między modułami, które dziś istnieją, i issue, które je usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of("submissions -> buildings", 1662);

    /**
     * Tabela tworzona albo zmieniana w instrukcji.
     */
    private static final Pattern TABLE = Pattern.compile(
        "(?:CREATE|ALTER)\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE
    );

    /**
     * Tabela tworzona w instrukcji.
     */
    private static final Pattern CREATED = Pattern.compile(
        "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE
    );

    /**
     * Tabela wskazana kluczem obcym.
     */
    private static final Pattern REFERENCE = Pattern.compile(
        "REFERENCES\\s+(\\w+)", Pattern.CASE_INSENSITIVE
    );

    @Test
    void rootHoldsOnlyMigrationsBeforeModules() throws IOException {
        Assertions.assertEquals(
            ModuleMigrationTest.BEFORE_MODULES,
            ModuleMigrationTest.names(
                ModuleMigrationTest.MIGRATIONS.resolve(ModuleMigrationTest.ROOT)
            ),
            "Nowa migracja należy do katalogu modułu, nie do __root"
        );
    }

    @Test
    void migrationFoldersAreModules() throws IOException {
        final Set<String> allowed = ApplicationModules.of(DostepnyKampusApplication.class)
            .stream()
            .map(ApplicationModule::getIdentifier)
            .map(Object::toString)
            .collect(Collectors.toCollection(TreeSet::new));
        allowed.add(ModuleMigrationTest.ROOT);
        Assertions.assertEquals(
            List.of(),
            ModuleMigrationTest.names(ModuleMigrationTest.MIGRATIONS).stream()
                .filter(name -> !allowed.contains(name))
                .toList(),
            "Migracja leży poza katalogiem modułu albo __root"
        );
    }

    @Test
    void foreignKeysStayInsideModuleOutsideExceptionList() throws IOException {
        final Set<String> found = ModuleMigrationTest.production();
        found.removeAll(ModuleMigrationTest.KNOWN.keySet());
        Assertions.assertEquals(
            Set.of(), found, "Klucz obcy wskazuje tabelę innego modułu albo tabelę bez właściciela"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingForeignKeys() throws IOException {
        final Set<String> stale = new TreeSet<>(ModuleMigrationTest.KNOWN.keySet());
        stale.removeAll(ModuleMigrationTest.production());
        Assertions.assertEquals(
            Set.of(), stale, "Klucza obcego z listy wyjątków już nie ma; usuń wpis"
        );
    }

    @Test
    void rejectsForeignKeyToOtherModuleAndAcceptsOwnTable() {
        Assertions.assertEquals(
            Set.of("notes -> areas", "points -> missing"),
            ModuleMigrationTest.crossings(
                Map.of("areas", "geo", "points", "geo", "notes", "note"),
                List.of(
                    "CREATE TABLE points (id INT, area INT REFERENCES areas (id));",
                    "ALTER TABLE notes ADD COLUMN area INT REFERENCES areas (id);",
                    "ALTER TABLE points ADD COLUMN x INT REFERENCES missing (id);"
                )
            ),
            "Reguła przepuściła klucz obcy do innego modułu albo odrzuciła klucz własny"
        );
    }

    private static Set<String> production() throws IOException {
        final Map<String, String> owners = new HashMap<>(ModuleMigrationTest.ROOT_OWNERS);
        final List<String> scripts = new ArrayList<>(16);
        try (Stream<Path> files = Files.walk(ModuleMigrationTest.MIGRATIONS)) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                final String sql = Files.readString(file);
                final String folder = file.getParent().getFileName().toString();
                if (!ModuleMigrationTest.ROOT.equals(folder)) {
                    ModuleMigrationTest.created(sql).forEach(table -> owners.put(table, folder));
                }
                scripts.add(sql);
            }
        }
        return ModuleMigrationTest.crossings(owners, scripts);
    }

    private static List<String> created(final String sql) {
        final List<String> tables = new ArrayList<>(2);
        final Matcher matcher = ModuleMigrationTest.CREATED.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tables;
    }

    private static Set<String> crossings(
        final Map<String, String> owners, final List<String> scripts
    ) {
        final Set<String> found = new TreeSet<>();
        for (final String script : scripts) {
            for (final String statement : script.split(";", -1)) {
                found.addAll(ModuleMigrationTest.foreign(owners, statement));
            }
        }
        return found;
    }

    private static List<String> foreign(final Map<String, String> owners, final String statement) {
        final List<String> found = new ArrayList<>(1);
        final Matcher table = ModuleMigrationTest.TABLE.matcher(statement);
        if (table.find()) {
            final String source = table.group(1).toLowerCase(Locale.ROOT);
            final Matcher target = ModuleMigrationTest.REFERENCE.matcher(statement);
            while (target.find()) {
                final String name = target.group(1).toLowerCase(Locale.ROOT);
                final String owner = owners.get(name);
                if (owner == null || !owner.equals(owners.get(source))) {
                    found.add(String.format("%s -> %s", source, name));
                }
            }
        }
        return found;
    }

    private static List<String> names(final Path folder) throws IOException {
        try (Stream<Path> items = Files.list(folder)) {
            return items.map(item -> item.getFileName().toString()).sorted().toList();
        }
    }
}
