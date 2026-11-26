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
 * te same moduły co {@code allowedDependencies}, także w postaci
 * {@code moduł :: interfejs}, a „brak.” oznacza listę pustą.</p>
 *
 * @since 0.1.0
 */
final class ModuleDescriptionArchitectureIT {

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
     * Zadanie opisane w pierwszym zdaniu Javadoc.
     */
    private static final Pattern TASK_SUMMARY = Pattern.compile(
        "/\\*\\*\\R\\h*\\* Zadanie:"
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
    private static final Pattern MODULE = Pattern.compile("\\{@code (\\w+(?: :: \\w+)?)\\}");

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
            final String problem = ModuleDescriptionArchitectureIT.problem(
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
    void acceptsTaskAsJavadocSummary() {
        Assertions.assertEquals(
            "",
            ModuleDescriptionArchitectureIT.problem(
                """
                %s
                 * Zadanie: składa zmiany od kursora.
                 *
                 * <p>API: HTTP.</p>
                 *
                 * <p>Zależności: {@code barrier :: api}, {@code building :: api},
                 * {@code inspection :: api}, {@code shared}.</p>
                 *
                 * <p>Nie robi: nie przechowuje danych.</p>
                 *
                 * <p>Kiedy dodawać tutaj: pobranie zmian.</p>
                %s
                """.formatted("/**", " */"),
                Set.of("barrier :: api", "building :: api", "inspection :: api", "shared")
            ),
            "Pierwszy akapit zadania w Javadoc nie wymaga znacznika HTML"
        );
    }

    @Test
    void rejectsMissingPartAndWrongDependencies() {
        final String full = """
            <p>Zadanie: a.</p>
            <p>API: b.</p>
            <p>Zależności: {@code shared}.</p>
            <p>Nie robi: c.</p>
            <p>Kiedy dodawać tutaj: d.</p>
            """;
        Assertions.assertEquals(
            List.of("", "", "akapity", "zależności"),
            List.of(
                ModuleDescriptionArchitectureIT.problem(full, Set.of("shared")),
                ModuleDescriptionArchitectureIT.problem(
                    full.replace("{@code shared}", "{@code building :: api}"),
                    Set.of("building :: api")
                ),
                ModuleDescriptionArchitectureIT.problem(
                    full.replace("<p>API: b.</p>", ""), Set.of("shared")
                ),
                ModuleDescriptionArchitectureIT.problem(full, Set.of("building", "shared"))
            ),
            "Reguła przepuściła opis bez akapitu albo z zależnościami spoza listy modułu"
        );
    }

    private static String problem(final String text, final Set<String> allowed) {
        final List<String> parts = new ArrayList<>(5);
        if (ModuleDescriptionArchitectureIT.TASK_SUMMARY.matcher(text).find()) {
            parts.add("Zadanie");
        }
        final Matcher part = ModuleDescriptionArchitectureIT.PART.matcher(text);
        while (part.find()) {
            parts.add(part.group(1));
        }
        final Set<String> described = new TreeSet<>();
        final Matcher block = ModuleDescriptionArchitectureIT.DEPENDENCIES.matcher(text);
        if (block.find()) {
            final Matcher name = ModuleDescriptionArchitectureIT.MODULE.matcher(block.group(1));
            while (name.find()) {
                described.add(name.group(1));
            }
        }
        final boolean complete = ModuleDescriptionArchitectureIT.PARTS.equals(parts);
        final String result;
        if (complete && new TreeSet<>(allowed).equals(described)) {
            result = "";
        } else if (complete) {
            result = "zależności";
        } else {
            result = "akapity";
        }
        return result;
    }
}
