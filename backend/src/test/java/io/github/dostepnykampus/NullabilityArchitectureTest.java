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
import java.util.function.Predicate;
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
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Strażnik: null tylko na granicy serwera.
 *
 * <p>Każdy pakiet kodu produkcyjnego ma {@code @NullMarked} w package-info,
 * poza listą pakietów czekających na oznaczenie. Lista ma się kurczyć, więc
 * pakiet z listy, który już ma adnotację, łamie regułę. Przykłady negatywne
 * test kompiluje do katalogu tymczasowego; pusty import jest błędem.</p>
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
                NullabilityArchitectureTest.loader(),
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
                NullabilityArchitectureTest.loader(),
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

    @ParameterizedTest(name = "lista czekających: {0}")
    @ValueSource(booleans = {false, true})
    void failsOnEmptyImport(final boolean waiting) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> NullabilityArchitectureTest.state(
                NullabilityArchitectureTest.loader(),
                NullabilityArchitectureTest.WAITING, waiting
            ).check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    private static Stream<Sample> samples() {
        final String pending = String.format("%s.p", NullabilityArchitectureTest.FIXTURES);
        final String plain = "final class Plain { }";
        final Function<ClassLoader, ArchRule> none =
            l -> NullabilityArchitectureTest.state(l, List.of(), false);
        return Stream.of(
            new Sample(
                none, "bez package-info", Map.of("b/Plain", plain), true
            ),
            new Sample(
                none, "bez NullMarked",
                Map.of("r/package-info", "NullUnmarked", "r/Plain", plain), true
            ),
            new Sample(
                l -> NullabilityArchitectureTest.state(
                    l, List.of(String.format("%s.w", NullabilityArchitectureTest.FIXTURES)), true
                ), "pakiet z listy",
                Map.of("w/package-info", "NullMarked", "w/Plain", plain), true
            ),
            new Sample(
                l -> NullabilityArchitectureTest.state(l, List.of(pending), false),
                "oznaczony pakiet i pakiet z listy",
                Map.of(
                    "o/package-info", "NullMarked", "o/Good", plain,
                    "p/Plain", plain
                ), false
            ),
            new Sample(
                l -> NullabilityArchitectureTest.state(l, List.of(pending), true),
                "nieoznaczony pakiet z listy",
                Map.of("p/Plain", plain), false
            )
        );
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
        final Predicate<Class<?>> bad
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
                if (bad.test(type)) {
                    events.add(SimpleConditionEvent.violated(item, item.getName()));
                }
            }
        };
    }

    private static ClassLoader loader() {
        return Thread.currentThread().getContextClassLoader();
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
                code = String.format("package %s;%n%s", pack, file.getValue());
            }
            Files.createDirectories(source.getParent());
            Files.writeString(source, code);
            args.add(source.toString());
        }
        if (ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, args.toArray(new String[0])) != 0) {
            throw new IllegalStateException("Przykład się nie skompilował");
        }
        boolean failed = false;
        try (
            URLClassLoader loader = new URLClassLoader(
                new URL[] {out.toUri().toURL()},
                NullabilityArchitectureTest.loader()
            )
        ) {
            sample.rule().apply(loader).check(new ClassFileImporter().importPath(out));
        } catch (final AssertionError ex) {
            failed = true;
        }
        return failed;
    }

    /**
     * Przykład: reguła, opis, źródła klas i czy reguła ma je odrzucić.
     *
     * @param rule Reguła zbudowana dla ładowarki klas przykładu
     * @param label Opis przykładu
     * @param files Mapa źródeł klas: ścieżka pakietu i klasy na treść
     * @param violating Czy reguła ma zgłosić naruszenie
     * @since 0.1.0
     */
    record Sample(
        Function<ClassLoader, ArchRule> rule, String label,
        Map<String, String> files, boolean violating
    ) {
        @Override
        public String toString() {
            return this.label;
        }
    }
}
