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
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Strażnik: null tylko na granicy serwera.
 *
 * <p>Każdy pakiet kodu produkcyjnego ma {@code @NullMarked} w package-info,
 * poza listą pakietów czekających na oznaczenie. Lista ma się kurczyć, więc
 * pakiet z listy, który już ma adnotację, łamie regułę. Przykłady negatywne
 * test kompiluje do katalogu tymczasowego; pusty import jest błędem. {@code Optional} jest tylko
 * wynikiem metody.</p>
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
     * Katalog na źródła i klasy przykładów.
     */
    @TempDir
    private Path dir;

    @Test
    void nullMarkedOutsideWaitingPackages() {
        Assertions.assertDoesNotThrow(
            () -> NullabilityArchitectureTest.state(
                NullabilityArchitectureTest.WAITING, false
            ).check(NullabilityArchitectureTest.production()),
            "Pakiet poza listą czekających nie ma @NullMarked"
        );
    }

    @Test
    void waitingPackagesAreNotMarkedYet() {
        Assumptions.assumeFalse(NullabilityArchitectureTest.WAITING.isEmpty());
        Assertions.assertDoesNotThrow(
            () -> NullabilityArchitectureTest.state(
                NullabilityArchitectureTest.WAITING, true
            ).check(NullabilityArchitectureTest.production()),
            "Pakiet z listy czekających ma już @NullMarked albo nie istnieje"
        );
    }

    @Test
    void productionImportReachesSubpackages() {
        MatcherAssert.assertThat(
            "Import kodu produkcyjnego pomija podpakiety",
            NullabilityArchitectureTest.production().contain(
                "io.github.dostepnykampus.barrier.domain.SubmissionPoint"
            ),
            Matchers.is(true)
        );
    }

    @Test
    void optionalOnlyAsMethodResult() {
        Assertions.assertDoesNotThrow(
            () -> NullabilityArchitectureTest.optionals()
                .check(NullabilityArchitectureTest.production()),
            "Optional jest polem, parametrem albo komponentem rekordu"
        );
    }

    @ParameterizedTest
    @MethodSource("samples")
    void judgesFixtureClasses(final Sample sample)
        throws IOException, URISyntaxException {
        MatcherAssert.assertThat(
            String.format("Reguła źle oceniła przykład %s", sample.label()),
            NullabilityArchitectureTest.violated(this.dir, sample),
            Matchers.is(sample.violating())
        );
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("emptyScopeRules")
    void failsOnEmptyImport(final ArchRule rule) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> rule.check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    private static Stream<ArchRule> emptyScopeRules() {
        return Stream.of(
            NullabilityArchitectureTest.state(NullabilityArchitectureTest.WAITING, false),
            NullabilityArchitectureTest.state(NullabilityArchitectureTest.WAITING, true),
            NullabilityArchitectureTest.optionals()
        );
    }

    private static Stream<Sample> samples() {
        final String pending = String.format("%s.p", NullabilityArchitectureTest.FIXTURES);
        final String plain = "final class Plain { }";
        final ArchRule none = NullabilityArchitectureTest.state(List.of(), false);
        final ArchRule optional = NullabilityArchitectureTest.optionals();
        return Stream.of(
            new Sample(
                none, "bez package-info", Map.of("b/Plain", plain), true
            ),
            new Sample(
                none, "pakiet tylko z package-info",
                Map.of("t/package-info", "NullUnmarked"), true
            ),
            new Sample(
                none, "bez NullMarked",
                Map.of("r/package-info", "NullUnmarked", "r/Plain", plain), true
            ),
            new Sample(
                NullabilityArchitectureTest.state(
                    List.of(String.format("%s.w", NullabilityArchitectureTest.FIXTURES)), true
                ), "pakiet z listy",
                Map.of("w/package-info", "NullMarked", "w/Plain", plain), true
            ),
            new Sample(
                NullabilityArchitectureTest.state(List.of(pending), false),
                "oznaczony pakiet i pakiet z listy",
                Map.of(
                    "o/package-info", "NullMarked", "o/Good", plain,
                    "p/Plain", plain
                ), false
            ),
            new Sample(
                NullabilityArchitectureTest.state(List.of(pending), true),
                "nieoznaczony pakiet z listy",
                Map.of("p/Plain", plain), false
            ),
            new Sample(
                optional, "pole", Map.of("a/A", "class A { Optional<String> v; }"), true
            ),
            new Sample(
                optional, "parametr metody",
                Map.of("a/A", "class A { void f(Optional<String> v) { } }"), true
            ),
            new Sample(
                optional, "parametr konstruktora",
                Map.of("a/A", "class A { A(Optional<String> v) { } }"), true
            ),
            new Sample(
                optional, "komponent rekordu",
                Map.of("a/A", "record A(Optional<String> v) { }"), true
            ),
            new Sample(
                optional, "argument typu",
                Map.of("a/A", "class A { List<Optional<String>> v; }"), true
            ),
            new Sample(
                optional, "wynik metody",
                Map.of("a/A", "class A { Optional<String> f() { return null; } }"), false
            )
        );
    }

    private static ArchRule optionals() {
        return ArchRuleDefinition.classes().should(
            NullabilityArchitectureTest.problems(
                "use Optional only as a result", NullabilityArchitectureTest::optionalSites
            )
        );
    }

    private static ArchCondition<JavaClass> problems(
        final String text, final Function<Class<?>, List<String>> check
    ) {
        return new ArchCondition<JavaClass>(text) {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                for (final String found : check.apply(item.reflect())) {
                    events.add(SimpleConditionEvent.violated(item, found));
                }
            }
        };
    }

    private static List<String> optionalSites(final Class<?> type) {
        final List<String> found = new ArrayList<>(0);
        Arrays.stream(type.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> NullabilityArchitectureTest.mentions(field.getGenericType()))
            .forEach(field -> found.add(String.format("%s.%s", type, field.getName())));
        NullabilityArchitectureTest.executables(type).forEach(
            code -> Arrays.stream(code.getParameters())
                .filter(arg -> NullabilityArchitectureTest.mentions(arg.getParameterizedType()))
                .forEach(arg -> found.add(String.format("%s.%s", type, code.getName())))
        );
        return found;
    }

    private static Stream<Executable> executables(final Class<?> type) {
        return Stream.<Executable>concat(
            Arrays.stream(type.getDeclaredConstructors()),
            Arrays.stream(type.getDeclaredMethods())
        ).filter(code -> !code.isSynthetic());
    }

    private static boolean mentions(final Type type) {
        final boolean found;
        if (type instanceof ParameterizedType generic) {
            found = NullabilityArchitectureTest.mentions(generic.getRawType())
                || Arrays.stream(generic.getActualTypeArguments())
                .anyMatch(NullabilityArchitectureTest::mentions);
        } else if (type instanceof GenericArrayType array) {
            found = NullabilityArchitectureTest.mentions(array.getGenericComponentType());
        } else if (type instanceof WildcardType wildcard) {
            found = Stream.concat(
                Arrays.stream(wildcard.getUpperBounds()),
                Arrays.stream(wildcard.getLowerBounds())
            ).anyMatch(NullabilityArchitectureTest::mentions);
        } else {
            found = type == Optional.class;
        }
        return found;
    }

    private static ArchRule state(final List<String> names, final boolean waiting) {
        return ArchRuleDefinition.classes()
            .that(NullabilityArchitectureTest.within(names, waiting))
            .should(NullabilityArchitectureTest.marking(waiting));
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

    private static ArchCondition<JavaClass> marking(final boolean waiting) {
        return new ArchCondition<JavaClass>("reside in a package with the expected @NullMarked") {
            @Override
            public void check(final JavaClass item, final ConditionEvents events) {
                if (item.getPackage().isAnnotatedWith(NullMarked.class) == waiting) {
                    events.add(SimpleConditionEvent.violated(item, item.getName()));
                }
            }
        };
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private static boolean violated(final Path dir, final Sample sample)
        throws IOException, URISyntaxException {
        final Path out = Files.createDirectories(dir.resolve("out"));
        final List<String> args = new ArrayList<>(
            List.of(
                "-d", out.toString(), "-classpath",
                Path.of(
                    Nullable.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI()
                ).toString()
            )
        );
        for (final Map.Entry<String, String> file : sample.files().entrySet()) {
            final Path source = dir.resolve(String.format("%s.java", file.getKey()));
            final String pack = String.format(
                "%s.%s", NullabilityArchitectureTest.FIXTURES,
                file.getKey().replaceFirst("/[^/]+$", "").replace('/', '.')
            );
            final String code;
            if (file.getKey().endsWith("package-info")) {
                code = String.format(
                    "@org.jspecify.annotations.%s%npackage %s;", file.getValue(), pack
                );
            } else {
                code = String.format(
                    "package %s;%nimport java.util.*;%nimport org.jspecify.annotations.*;%n%s",
                    pack, file.getValue()
                );
            }
            Files.createDirectories(source.getParent());
            Files.writeString(source, code);
            args.add(source.toString());
        }
        if (ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, args.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Przykład się nie skompilował");
        }
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();
        boolean failed = false;
        try (
            URLClassLoader loader = new URLClassLoader(
                new URL[] {out.toUri().toURL()}, before
            )
        ) {
            thread.setContextClassLoader(loader);
            sample.rule().check(new ClassFileImporter().importPath(out));
        } catch (final AssertionError ex) {
            failed = true;
        } finally {
            thread.setContextClassLoader(before);
        }
        return failed;
    }

    /**
     * Przykład: reguła, opis, źródła klas i czy reguła ma je odrzucić.
     *
     * @param rule Reguła sprawdzana na przykładzie
     * @param label Opis przykładu
     * @param files Mapa źródeł klas: ścieżka pakietu i klasy na treść
     * @param violating Czy reguła ma zgłosić naruszenie
     * @since 0.1.0
     */
    record Sample(
        ArchRule rule, String label,
        Map<String, String> files, boolean violating
    ) {
        @Override
        public String toString() {
            return this.label;
        }
    }
}
