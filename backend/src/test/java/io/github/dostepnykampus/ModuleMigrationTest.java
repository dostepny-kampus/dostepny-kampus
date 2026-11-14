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
 * {@code __root} mają właściciela na stałej liście. Klucze obce w schemacie po migracjach
 * sprawdza {@link ModuleForeignKeyTest}.
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
     * Tabela tworzona w instrukcji.
     */
    private static final Pattern CREATED = Pattern.compile(
        "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE
    );

    @Test
    void holdsOnlyMigrationsBeforeModulesInRoot() throws IOException {
        Assertions.assertEquals(
            ModuleMigrationTest.BEFORE_MODULES,
            ModuleMigrationTest.names(
                ModuleMigrationTest.MIGRATIONS.resolve(ModuleMigrationTest.ROOT)
            ),
            "Nowa migracja należy do katalogu modułu, nie do __root"
        );
    }

    @Test
    void matchesMigrationFoldersToModules() throws IOException {
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
    void rejectsForeignKeyToOtherModuleAndAcceptsOwnTable() {
        Assertions.assertEquals(
            Set.of("notes -> areas", "points -> missing"),
            ModuleMigrationTest.crossings(
                Map.of("areas", "geo", "points", "geo", "notes", "note"),
                List.of(
                    List.of("points", "areas"), List.of("notes", "areas"),
                    List.of("points", "missing")
                )
            ),
            "Reguła przepuściła klucz obcy do innego modułu albo odrzuciła klucz własny"
        );
    }

    /**
     * Właściciele tabel: moduł z katalogu migracji, w której tabela powstała, a dla tabel
     * z {@code __root} stała lista.
     *
     * @return Moduł po nazwie tabeli
     * @throws IOException Gdy nie da się przeczytać migracji
     */
    static Map<String, String> owners() throws IOException {
        final Map<String, String> owners = new HashMap<>(ModuleMigrationTest.ROOT_OWNERS);
        try (Stream<Path> files = Files.walk(ModuleMigrationTest.MIGRATIONS)) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                final String folder = file.getParent().getFileName().toString();
                if (!ModuleMigrationTest.ROOT.equals(folder)) {
                    ModuleMigrationTest.created(Files.readString(file))
                        .forEach(table -> owners.put(table, folder));
                }
            }
        }
        return owners;
    }

    /**
     * Klucze obce, które wychodzą poza moduł tabeli albo wskazują tabelę bez właściciela.
     *
     * @param owners Moduł po nazwie tabeli
     * @param keys Klucze obce jako pary: tabela, tabela wskazana
     * @return Opisy {@code tabela -> wskazana} kluczy łamiących regułę
     */
    static Set<String> crossings(
        final Map<String, String> owners, final List<List<String>> keys
    ) {
        return keys.stream()
            .filter(key -> !ModuleMigrationTest.inside(owners, key.get(0), key.get(1)))
            .map(key -> String.format("%s -> %s", key.get(0), key.get(1)))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean inside(
        final Map<String, String> owners, final String source, final String target
    ) {
        return owners.containsKey(target) && owners.get(target).equals(owners.get(source));
    }

    private static List<String> created(final String sql) {
        final List<String> tables = new ArrayList<>(2);
        final Matcher matcher = ModuleMigrationTest.CREATED.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tables;
    }

    private static List<String> names(final Path folder) throws IOException {
        try (Stream<Path> items = Files.list(folder)) {
            return items.map(item -> item.getFileName().toString()).sorted().toList();
        }
    }
}
