/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Strażnik: null tylko na granicy serwera.
 *
 * <p>Każdy pakiet kodu produkcyjnego ma {@code @NullMarked} w package-info,
 * poza listą pakietów czekających na oznaczenie. Lista ma się kurczyć, więc
 * pakiet z listy, który już ma adnotację, łamie regułę.
 * Każda reguła ma przykłady negatywne, które test kompiluje do katalogu
 * tymczasowego, i odrzuca pusty zakres importu.</p>
 *
 * @since 0.1.0
 */
final class NullabilityArchitectureTest {

    /**
     * Pakiet klas przykładów, poniżej pakietu bazowego.
     */
    private static final String FIXTURES = "io.github.dostepnykampus.fixture";

    /**
     * Pakiety (z podpakietami) czekające na oznaczenie, do scalenia I3.
     */
    private static final List<String> WAITING =
        List.of("io.github.dostepnykampus.barrier");

    /**
     * Katalog na źródła i klasy przykładów, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void nullMarkedOutsideWaitingPackages() {
        Assertions.assertDoesNotThrow(
            () -> NullabilityArchitectureTest.state(
                NullabilityArchitectureTest.loader(), WAITING, false
            ).check(NullabilityArchitectureTest.production()),
            "Pakiet poza listą czekających nie ma @NullMarked"
        );
    }

    @Test
    void waitingPackagesAreNotMarkedYet() {
        Assumptions.assumeFalse(WAITING.isEmpty(), "Lista czekających pusta");
        Assertions.assertDoesNotThrow(
            () -> NullabilityArchitectureTest.state(
                NullabilityArchitectureTest.loader(), WAITING, true
            ).check(NullabilityArchitectureTest.production()),
            "Pakiet z listy czekających ma już @NullMarked albo nie istnieje"
        );
    }

    @Test
    void productionImportReachesSubpackagesAndSkipsTests() {
        final JavaClasses all = NullabilityArchitectureTest.production();
        MatcherAssert.assertThat(
            "Import nie obejmuje podpakietów albo obejmuje klasy testowe",
            all.contain("io.github.dostepnykampus.barrier.domain.SubmissionPoint")
                && !all.contain(NullabilityArchitectureTest.class),
            Matchers.is(true)
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("cases")
    void judgesFixtureClasses(
        final Function<ClassLoader, ArchRule> rule, final String label,
        final Map<String, String> files, final boolean violating
    ) throws IOException, URISyntaxException {
        MatcherAssert.assertThat(
            String.format("Reguła źle oceniła przykład %s", label),
            NullabilityArchitectureTest.violated(rule, this.dir, files),
            Matchers.is(violating)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void failsOnEmptyImport(final Function<ClassLoader, ArchRule> rule) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> rule.apply(NullabilityArchitectureTest.loader())
                .check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    private static Stream<Arguments> cases() {
        final String wait = String.format("%s.wait", FIXTURES);
        final String plain = "final class Plain { }";
        final String pending = String.format("%s.pending", FIXTURES);
        final Function<ClassLoader, ArchRule> marked =
            l -> NullabilityArchitectureTest.state(l, List.of(pending), false);
        return Stream.of(
            Arguments.of(
                NullabilityArchitectureTest.unmarked(), "bez package-info",
                Map.of("bare/Plain", plain), true
            ),
            Arguments.of(
                NullabilityArchitectureTest.unmarked(), "bez NullMarked",
                Map.of("raw/package-info", "NullUnmarked", "raw/Plain", plain), true
            ),
            Arguments.of(
                NullabilityArchitectureTest.listed(wait), "pakiet z listy",
                Map.of("wait/package-info", "NullMarked", "wait/Plain", plain), true
            ),
            Arguments.of(
                NullabilityArchitectureTest.listed(wait), "podpakiet z listy",
                Map.of(
                    "wait/sub/package-info", "NullMarked", "wait/sub/Plain", plain
                ), true
            ),
            Arguments.of(
                marked, "oznaczony pakiet i pakiet z listy",
                Map.of(
                    "ok/package-info", "NullMarked", "ok/Good", plain,
                    "pending/Plain", plain
                ), false
            ),
            Arguments.of(
                NullabilityArchitectureTest.listed(pending),
                "nieoznaczony pakiet z listy",
                Map.of("pending/Plain", plain), false
            )
        );
    }

    private static Stream<Arguments> rules() {
        return Stream.<Function<ClassLoader, ArchRule>>of(
            l -> NullabilityArchitectureTest.state(l, WAITING, false),
            l -> NullabilityArchitectureTest.state(l, WAITING, true)
        ).map(Arguments::of);
    }

    private static Function<ClassLoader, ArchRule> unmarked() {
        return l -> NullabilityArchitectureTest.state(l, List.of(), false);
    }

    private static Function<ClassLoader, ArchRule> listed(final String pack) {
        return l -> NullabilityArchitectureTest.state(l, List.of(pack), true);
    }

    private static ArchRule state(
        final ClassLoader loader, final List<String> names, final boolean waiting
    ) {
        return ArchRuleDefinition.classes()
            .that(NullabilityArchitectureTest.real())
            .and(NullabilityArchitectureTest.within(names, waiting))
            .should(
                NullabilityArchitectureTest.condition(
                    loader, "reside in a package with the expected @NullMarked",
                    type -> type.getPackage().isAnnotationPresent(NullMarked.class)
                        == waiting
                        ? List.of(waiting ? "pakiet z listy ma już @NullMarked"
                            : "pakiet bez @NullMarked")
                        : List.of()
                )
            );
    }

    private static DescribedPredicate<JavaClass> real() {
        return DescribedPredicate.describe(
            "are not package-info",
            item -> !"package-info".equals(item.getSimpleName())
        );
    }

    private static DescribedPredicate<JavaClass> within(
        final List<String> names, final boolean waiting
    ) {
        return DescribedPredicate.describe(
            "reside in a package selected by the waiting list",
            item -> waiting == names.stream().anyMatch(
                name -> item.getPackageName().equals(name)
                    || item.getPackageName().startsWith(String.format("%s.", name))
            )
        );
    }

    private static ArchCondition<JavaClass> condition(
        final ClassLoader loader, final String text,
        final Function<Class<?>, List<String>> check
    ) {
        return new ArchCondition<JavaClass>(text) {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                final Class<?> type;
                try {
                    type = loader.loadClass(item.getName());
                } catch (final ClassNotFoundException ex) {
                    throw new IllegalStateException(item.getName(), ex);
                }
                for (final String problem : check.apply(type)) {
                    events.add(
                        SimpleConditionEvent.violated(
                            item, String.format("%s: %s", item.getName(), problem)
                        )
                    );
                }
            }
        };
    }

    private static ClassLoader loader() {
        return DostepnyKampusApplication.class.getClassLoader();
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private static boolean violated(
        final Function<ClassLoader, ArchRule> rule, final Path dir,
        final Map<String, String> files
    ) throws IOException, URISyntaxException {
        final Path out = Files.createDirectories(dir.resolve("out"));
        final String jspecify = Path.of(
            Nullable.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ).toString();
        final List<String> args =
            new ArrayList<>(List.of("-d", out.toString(), "-classpath", jspecify));
        for (final Map.Entry<String, String> file : files.entrySet()) {
            final Path source = dir.resolve(file.getKey() + ".java");
            final String pack = String.format(
                "%s.%s", FIXTURES, file.getKey().replaceFirst("/[^/]+$", "")
                    .replace('/', '.')
            );
            Files.createDirectories(source.getParent());
            Files.writeString(
                source,
                file.getKey().endsWith("package-info")
                    ? String.format(
                        "@org.jspecify.annotations.%s%npackage %s;",
                        file.getValue(), pack
                    )
                    : String.format("package %s;%n%s", pack, file.getValue())
            );
            args.add(source.toString());
        }
        if (ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, args.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Przykład się nie skompilował");
        }
        boolean failed = false;
        try (URLClassLoader loader = new URLClassLoader(
            new URL[] {out.toUri().toURL()}, NullabilityArchitectureTest.loader()
        )) {
            rule.apply(loader).check(new ClassFileImporter().importPath(out));
        } catch (final AssertionError ex) {
            failed = true;
        }
        return failed;
    }
}
