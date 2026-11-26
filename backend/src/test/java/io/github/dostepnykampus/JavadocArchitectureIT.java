/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Javadoc serwera opisuje tylko kontrakt: bez numerów issue i decyzji, z częścią
 * opisową najwyżej sześciu linii poza linią {@code Wymagania:} i identyfikatorami
 * wymagań tylko w tej linii na końcu opisu.
 *
 * @since 0.1.0
 */
final class JavadocArchitectureIT {

    /**
     * Blok Javadoc.
     */
    private static final Pattern BLOCK = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);

    /**
     * Znacznik pracy odłożonej z numerem issue.
     */
    private static final Pattern DEFERRED = Pattern.compile("[T]ODO\\(#\\d+\\)");

    /**
     * Numer issue.
     */
    private static final Pattern ISSUE = Pattern.compile("(?<![\\w&/#])#\\d{1,5}\\b");

    /**
     * Słowo „decyzja” z numerem.
     */
    private static final Pattern DECISION = Pattern.compile(
        "(?iu)\\bdecyzj\\w*\\b[^.;()\\d]{0,25}\\d"
    );

    /**
     * Linia wymagań; w Javadoc jednowierszowym koniec opisu.
     */
    private static final Pattern REQUIREMENTS = Pattern.compile(
        "(^|\\s)(<p>)?Wymagania:[^\\n]*$"
    );

    /**
     * Identyfikator wymagania.
     */
    private static final Pattern REQUIREMENT = Pattern.compile(
        "(?<![\\w-])(?:US\\d+(?:\\.\\d+)?|UC\\d+|BR-G?\\d+|NFR-\\d+|RO-[A-Z]{3}|R-\\d+|N\\d+)(?![\\w°-]|\\.\\d)"
    );

    /**
     * Koniec linii.
     */
    private static final Pattern LINE = Pattern.compile("\\R");

    /**
     * Ozdobnik linii Javadoc.
     */
    private static final Pattern DECORATION = Pattern.compile("^\\s*\\*?\\s?");

    /**
     * Fragment kodu.
     */
    private static final Pattern CODE = Pattern.compile("\\{@code [^}]*}|`[^`]*`");

    @Test
    void describesOnlyContractInServerJavadoc() {
        MatcherAssert.assertThat(
            "Javadoc serwera ma historię, za długi opis albo wymagania poza linią Wymagania",
            Stream.of("src/main/java", "src/test/java")
                .flatMap(JavadocArchitectureIT::sources)
                .flatMap(path -> JavadocArchitectureIT.violations(path).stream())
                .toList(),
            Matchers.empty()
        );
    }

    @Test
    void reportsIssueAndDecisionNumbers() {
        MatcherAssert.assertThat(
            "Numer issue albo decyzji w Javadoc nie jest zgłaszany",
            JavadocArchitectureIT.problems(
                JavadocArchitectureIT.text(
                    "/** Zwraca listę (#123). */", "/**", " * Lista (decyzja", " * 82).", " */"
                )
            ),
            Matchers.contains("1 historia", "2 historia")
        );
    }

    @Test
    void passesDeferredWorkMarker() {
        MatcherAssert.assertThat(
            "Znacznik pracy odłożonej jest zgłaszany jak historia",
            JavadocArchitectureIT.problems(
                "/** Lista. %sODO(#12): sortowanie. */".formatted("T")
            ),
            Matchers.empty()
        );
    }

    @Test
    void reportsLongDescriptionWithoutTags() {
        MatcherAssert.assertThat(
            "Długość opisu liczy znaczniki albo nie zgłasza siedmiu linii",
            JavadocArchitectureIT.problems(
                JavadocArchitectureIT.text(
                    "/**",
                    " * a",
                    " * b",
                    " * c",
                    " * d",
                    " * e",
                    " * f",
                    " *",
                    " * @param x Znaczenie",
                    " * @since 0.1.0",
                    " */",
                    "/**",
                    " * a",
                    " * b",
                    " * c",
                    " * d",
                    " * e",
                    " * f",
                    " * g",
                    " */"
                )
            ),
            Matchers.contains("12 długość")
        );
    }

    @Test
    void keepsRequirementIdsInRequirementsLine() {
        MatcherAssert.assertThat(
            "Identyfikator wymagania poza linią Wymagania nie jest zgłaszany",
            JavadocArchitectureIT.problems(
                JavadocArchitectureIT.text(
                    "/** Odczyt budynków (US1). */",
                    "/**",
                    " * Odczyt budynków.",
                    " *",
                    " * <p>Wymagania: US1, BR-G2</p>",
                    " */",
                    "/** Odczyt. Wymagania: NFR-3 */",
                    "/** Budynek C-13, typ TB-29 i {@code US1}. */",
                    "/**",
                    " * @param a Próg z NFR-3",
                    " */"
                )
            ),
            Matchers.contains("1 wymagania", "9 wymagania")
        );
    }

    private static Stream<Path> sources(final String root) {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            return files.filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith("JavadocArchitectureIT.java"))
                .toList().stream();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static List<String> violations(final Path path) {
        try {
            final int limit;
            if (path.endsWith("package-info.java")) {
                limit = 8;
            } else {
                limit = 6;
            }
            return JavadocArchitectureIT.problems(
                Files.readString(path, StandardCharsets.UTF_8), limit
            ).stream().map(problem -> "%s:%s".formatted(path, problem)).toList();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static List<String> problems(final String source) {
        return JavadocArchitectureIT.problems(source, 6);
    }

    private static List<String> problems(final String source, final int limit) {
        final List<String> found = new ArrayList<>(0);
        final Matcher block = JavadocArchitectureIT.BLOCK.matcher(source);
        int counted = 0;
        int line = 1;
        while (block.find()) {
            line += JavadocArchitectureIT.newlines(source.substring(counted, block.start()));
            counted = block.start();
            final String body = block.group(1);
            final String flat = JavadocArchitectureIT.DEFERRED.matcher(
                body.replaceAll("\\s*\\n\\s*\\*?", " ")
            ).replaceAll("");
            if (JavadocArchitectureIT.ISSUE.matcher(flat).find()
                || JavadocArchitectureIT.DECISION.matcher(flat).find()) {
                found.add("%d historia".formatted(line));
            }
            final List<String> lines = Arrays.stream(JavadocArchitectureIT.LINE.split(body))
                .map(text -> JavadocArchitectureIT.DECORATION.matcher(text).replaceFirst(""))
                .toList();
            if (lines.stream().takeWhile(text -> !text.startsWith("@"))
                .filter(text -> !text.isBlank())
                .filter(text -> !JavadocArchitectureIT.REQUIREMENTS.matcher(text).find())
                .count() > limit) {
                found.add("%d długość".formatted(line));
            }
            if (lines.stream()
                .map(text -> JavadocArchitectureIT.CODE.matcher(text).replaceAll(""))
                .map(text -> JavadocArchitectureIT.REQUIREMENTS.matcher(text).replaceAll(""))
                .anyMatch(text -> JavadocArchitectureIT.REQUIREMENT.matcher(text).find())) {
                found.add("%d wymagania".formatted(line));
            }
        }
        return found;
    }

    private static String text(final String... lines) {
        return String.join(String.valueOf('\n'), lines);
    }

    private static int newlines(final String text) {
        return (int) text.chars().filter(chr -> chr == '\n').count();
    }
}
