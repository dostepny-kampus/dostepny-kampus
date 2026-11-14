/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Strażnik nazw pakietów serwera. Każdy segment pakietu pod korzeniem modułu jest nazwą
 * warstwy albo tematu ze słownika {@code docs/docs/architektura/tematy.tsv}, więc nazwy
 * {@code util}, {@code common} czy {@code helper} nie przechodzą. Pod korzeniem modułu leżą
 * najwyżej trzy segmenty.
 *
 * @since 0.1.0
 */
final class PackageNameArchitectureTest {

    /**
     * Nazwy warstw, dozwolone obok tematów.
     */
    private static final Set<String> LAYERS = Set.of(
        "api", "domain", "application", "web", "persistence", "json", "storage"
    );

    @Test
    void productionPackagesUseDictionaryNames() throws IOException {
        Assertions.assertEquals(
            List.of(),
            PackageNameArchitectureTest.problems(
                new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackagesOf(DostepnyKampusApplication.class)
                    .stream()
                    .map(JavaClass::getPackageName)
                    .collect(Collectors.toCollection(TreeSet::new)),
                ApplicationModules.of(DostepnyKampusApplication.class).stream()
                    .map(ApplicationModule::getBasePackage)
                    .map(JavaPackage::getName)
                    .toList(),
                PackageNameArchitectureTest.dictionary()
            ),
            "Pakiet ma nazwę spoza słownika tematów i warstw albo leży za głęboko"
        );
    }

    @Test
    void rejectsUnknownNameAndTooDeepPackage() {
        Assertions.assertEquals(
            List.of("m.submission.content.json.web: głębokość", "m.util: util"),
            PackageNameArchitectureTest.problems(
                new TreeSet<>(
                    List.of(
                        "m", "m.submission", "m.submission.content.json",
                        "m.submission.content.json.web", "m.util", "other.util"
                    )
                ),
                List.of("m"),
                Set.of("submission", "content")
            ),
            "Reguła przepuściła nazwę spoza słownika albo zbyt głęboki pakiet"
        );
    }

    private static List<String> problems(
        final Collection<String> packages, final Collection<String> roots, final Set<String> names
    ) {
        final List<String> found = new ArrayList<>(0);
        for (final String pack : packages) {
            for (final String root : roots) {
                if (pack.startsWith(String.format("%s.", root))) {
                    found.addAll(PackageNameArchitectureTest.wrong(pack, root, names));
                }
            }
        }
        return found;
    }

    private static List<String> wrong(
        final String pack, final String root, final Set<String> names
    ) {
        final List<String> found = new ArrayList<>(0);
        final String[] parts = pack.substring(root.length() + 1).split("\\.", -1);
        if (parts.length > 3) {
            found.add(String.format("%s: głębokość", pack));
        }
        for (final String part : parts) {
            if (!names.contains(part) && !PackageNameArchitectureTest.LAYERS.contains(part)) {
                found.add(String.format("%s: %s", pack, part));
            }
        }
        return found;
    }

    private static Set<String> dictionary() throws IOException {
        return Files.readAllLines(Path.of("..", "docs", "docs", "architektura", "tematy.tsv"))
            .stream()
            .skip(1)
            .filter(line -> !line.isBlank())
            .map(line -> line.split("\t", -1)[0])
            .collect(Collectors.toSet());
    }
}
