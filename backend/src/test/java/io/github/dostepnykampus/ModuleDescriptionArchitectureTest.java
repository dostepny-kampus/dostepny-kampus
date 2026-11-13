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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Strażnik opisu modułu serwera w Javadoc {@code package-info.java}.
 *
 * <p>Opis ma akapity w stałej kolejności: zadanie, API, zależności, czego
 * moduł nie robi i kiedy dodawać do niego kod. Akapit zależności wymienia
 * te same moduły co {@code allowedDependencies}, a „brak.” oznacza listę
 * pustą.</p>
 *
 * @since 0.1.0
 */
final class ModuleDescriptionArchitectureTest {

    /**
     * Akapity opisu modułu w wymaganej kolejności.
     */
    private static final List<String> PARTS = List.of(
        "Zadanie", "API", "Zależności", "Nie robi", "Kiedy dodawać tutaj"
    );

    /**
     * Początek akapitu opisu z jego nazwą.
     */
    private static final Pattern PART = Pattern.compile(
        "<p>(Zadanie|API|Zależności|Nie robi|Kiedy dodawać tutaj):"
    );

    /**
     * Akapit zależności.
     */
    private static final Pattern DEPENDENCIES = Pattern.compile(
        "<p>Zależności:(.*?)</p>", Pattern.DOTALL
    );

    /**
     * Nazwa modułu w akapicie zależności.
     */
    private static final Pattern MODULE = Pattern.compile("\\{@code (\\w+)\\}");

    @Test
    void modulesDescribeAllPartsAndDependencies() throws IOException {
        final Map<String, String> found = new TreeMap<>();
        for (final org.springframework.modulith.core.ApplicationModule module
            : ApplicationModules.of(DostepnyKampusApplication.class)) {
            final JavaPackage pack = module.getBasePackage();
            final String text = Files.readString(
                Path.of("src", "main", "java", pack.getName().replace('.', '/'))
                    .resolve("package-info.java")
            );
            final String problem = ModuleDescriptionArchitectureTest.problem(
                text,
                pack.getAnnotation(ApplicationModule.class)
                    .map(annotation -> Set.of(annotation.allowedDependencies()))
                    .orElse(Set.of())
            );
            if (!problem.isEmpty()) {
                found.put(pack.getName(), problem);
            }
        }
        Assertions.assertEquals(
            Map.of(), found,
            "Opis modułu w package-info.java nie ma wszystkich akapitów albo zależności"
        );
    }

    @Test
    void rejectsMissingPartAndWrongDependencies() {
        final String full = String.join(
            "\n", "<p>Zadanie: a.</p>", "<p>API: b.</p>",
            "<p>Zależności: {@code shared}.</p>", "<p>Nie robi: c.</p>",
            "<p>Kiedy dodawać tutaj: d.</p>"
        );
        Assertions.assertEquals(
            List.of("", "akapity", "zależności"),
            List.of(
                ModuleDescriptionArchitectureTest.problem(full, Set.of("shared")),
                ModuleDescriptionArchitectureTest.problem(
                    full.replace("<p>API: b.</p>", ""), Set.of("shared")
                ),
                ModuleDescriptionArchitectureTest.problem(full, Set.of("building", "shared"))
            ),
            "Reguła przepuściła opis bez akapitu albo z zależnościami spoza listy modułu"
        );
    }

    private static String problem(final String text, final Set<String> allowed) {
        final List<String> parts = new ArrayList<>();
        final Matcher part = ModuleDescriptionArchitectureTest.PART.matcher(text);
        while (part.find()) {
            parts.add(part.group(1));
        }
        final Set<String> described = new TreeSet<>();
        final Matcher block = ModuleDescriptionArchitectureTest.DEPENDENCIES.matcher(text);
        if (block.find()) {
            final Matcher name = ModuleDescriptionArchitectureTest.MODULE.matcher(block.group(1));
            while (name.find()) {
                described.add(name.group(1));
            }
        }
        final String result;
        if (!ModuleDescriptionArchitectureTest.PARTS.equals(parts)) {
            result = "akapity";
        } else if (!new TreeSet<>(allowed).equals(described)) {
            result = "zależności";
        } else {
            result = "";
        }
        return result;
    }
}
