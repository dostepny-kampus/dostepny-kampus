/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reguła na klasach testów serwera z listą wyjątków, która tylko maleje. Klasa zagnieżdżona
 * liczy się jako klasa, w której leży.
 *
 * @since 0.1.0
 */
final class ExceptionListRule {

    /**
     * Warunek klasy łamiącej regułę.
     */
    private final Predicate<JavaClass> broken;

    /**
     * Pełne nazwy klas, które dziś łamią regułę, i issue, które to usuwa.
     */
    private final Map<String, Integer> known;

    /**
     * Tworzy regułę.
     *
     * @param broken Warunek klasy łamiącej regułę
     * @param known Pełne nazwy klas z listy wyjątków i numery issue
     */
    ExceptionListRule(final Predicate<JavaClass> broken, final Map<String, Integer> known) {
        this.broken = broken;
        this.known = known;
    }

    /**
     * Klasy testów serwera łamiące regułę, których nie ma na liście wyjątków.
     *
     * @return Pełne nazwy klas w porządku alfabetycznym; pusta lista, gdy ich nie ma
     */
    List<String> fresh() {
        return this.violators(ExceptionListRule.tests()).stream()
            .filter(name -> !this.known.containsKey(name))
            .toList();
    }

    /**
     * Wpisy listy wyjątków, które już nie łamią reguły.
     *
     * @return Pełne nazwy klas w porządku alfabetycznym; pusta lista, gdy ich nie ma
     */
    List<String> stale() {
        final Set<String> found = this.violators(ExceptionListRule.tests());
        return this.known.keySet().stream()
            .filter(name -> !found.contains(name))
            .sorted()
            .toList();
    }

    /**
     * Klasy łamiące regułę wśród klas przykładu skompilowanego w katalogu.
     *
     * @param dir Katalog przykładu, zwykle tymczasowy katalog testu
     * @param pack Pakiet przykładu
     * @param code Cała treść pliku {@code Sample.java}, z deklaracją pakietu
     * @return Pełne nazwy klas łamiących regułę
     * @throws IOException Gdy zapis albo odczyt przykładu się nie udał
     */
    Set<String> sample(final Path dir, final String pack, final String code) throws IOException {
        final ArchSamples samples = new ArchSamples(dir);
        samples.write(pack, "Sample", code);
        return this.violators(samples.compiled(pack));
    }

    private Set<String> violators(final JavaClasses classes) {
        return classes.stream()
            .filter(this.broken)
            .map(type -> type.getName().replaceAll("\\$.*", ""))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static JavaClasses tests() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages("io.github.dostepnykampus");
    }
}
