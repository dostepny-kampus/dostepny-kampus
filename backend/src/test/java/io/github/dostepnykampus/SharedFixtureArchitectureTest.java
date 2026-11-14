/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test serwera przygotowuje swoje dane sam: klasa z metodami testów nie ma pól obiektu ani metod
 * {@code @BeforeEach} i {@code @BeforeAll}. Stałe {@code static} i sprzątanie w
 * {@code @AfterEach} są dozwolone. Klasy, które dziś łamią regułę, są na liście wyjątków
 * z numerem issue; lista tylko maleje.
 *
 * @since 0.1.0
 */
final class SharedFixtureArchitectureTest {

    /**
     * Klasy testów ze wspólnym polem albo metodą przygotowania i issue, które to usuwa; nazwy
     * bez wspólnego początku pakietu.
     */
    private static final Map<String, Integer> KNOWN = Stream.of(
        "ClassNameArchitectureTest",
        "ImmutableDomainArchitectureTest",
        "LayerDirectionArchitectureTest",
        "MockFreeArchitectureTest",
        "ModularityTest",
        "ModuleRootArchitectureTest",
        "NullabilityArchitectureTest",
        "PackageCycleArchitectureTest",
        "PackageDescriptionArchitectureTest",
        "PackageSizeArchitectureTest",
        "TechnologyArchitectureTest",
        "barrier.BarrierModuleTest",
        "building.BuildingReadContractTest",
        "building.BuildingsHttpTest"
    ).collect(
        Collectors.toMap(
            name -> String.format("io.github.dostepnykampus.%s", name), name -> 1963
        )
    );

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.fixture";

    /**
     * Adnotacje metod testów.
     */
    private static final List<String> TESTS = List.of(
        "org.junit.jupiter.api.Test", "org.junit.jupiter.params.ParameterizedTest",
        "org.junit.jupiter.api.RepeatedTest", "org.junit.jupiter.api.TestFactory"
    );

    /**
     * Adnotacje metod przygotowania.
     */
    private static final List<String> SETUPS = List.of(
        "org.junit.jupiter.api.BeforeEach", "org.junit.jupiter.api.BeforeAll"
    );

    @Test
    void testsPrepareOwnData() {
        Assertions.assertEquals(
            List.of(),
            SharedFixtureArchitectureTest.violators(SharedFixtureArchitectureTest.tests())
                .stream()
                .filter(name -> !SharedFixtureArchitectureTest.KNOWN.containsKey(name))
                .toList(),
            "Klasa testu ma pole obiektu albo metodę przygotowania; dane przygotuj w teście"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final Set<String> found = SharedFixtureArchitectureTest.violators(
            SharedFixtureArchitectureTest.tests()
        );
        Assertions.assertEquals(
            List.of(),
            SharedFixtureArchitectureTest.KNOWN.keySet().stream()
                .filter(name -> !found.contains(name))
                .sorted()
                .toList(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "private int count; @org.junit.jupiter.api.Test void check() { }",
            "@org.junit.jupiter.api.BeforeEach void prepare() { } @Test void check() { }",
            "@org.junit.jupiter.api.BeforeAll static void prepare() { } @Test void check() { }"
        }
    )
    void rejectsSharedFixture(final String members, @TempDir final Path dir) throws IOException {
        Assertions.assertEquals(
            Set.of(String.format("%s.Sample", SharedFixtureArchitectureTest.SAMPLES)),
            SharedFixtureArchitectureTest.violators(
                SharedFixtureArchitectureTest.compiled(dir, members)
            ),
            "Reguła przepuściła wspólne pole albo metodę przygotowania"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "private static final int LIMIT = 1; @Test void check() { }",
            "@org.junit.jupiter.api.AfterEach void clean() { } @Test void check() { }",
            "private int count; void helper() { }"
        }
    )
    void acceptsOwnDataAndHelpers(final String members, @TempDir final Path dir)
        throws IOException {
        Assertions.assertEquals(
            Set.of(),
            SharedFixtureArchitectureTest.violators(
                SharedFixtureArchitectureTest.compiled(dir, members)
            ),
            "Reguła odrzuciła stałą, sprzątanie albo klasę pomocniczą bez testów"
        );
    }

    private static Set<String> violators(final JavaClasses classes) {
        return classes.stream()
            .filter(SharedFixtureArchitectureTest::tested)
            .filter(SharedFixtureArchitectureTest::shared)
            .map(type -> type.getName().replaceAll("\\$.*", ""))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean tested(final JavaClass type) {
        return type.getMethods().stream().anyMatch(
            method -> SharedFixtureArchitectureTest.TESTS.stream()
                .anyMatch(method::isAnnotatedWith)
        );
    }

    private static boolean shared(final JavaClass type) {
        return Stream.concat(
            type.getFields().stream().map(
                field -> !field.getModifiers().contains(JavaModifier.STATIC)
                    && !field.getModifiers().contains(JavaModifier.SYNTHETIC)
            ),
            type.getMethods().stream().map(
                method -> SharedFixtureArchitectureTest.SETUPS.stream()
                    .anyMatch(method::isAnnotatedWith)
            )
        ).anyMatch(Boolean::booleanValue);
    }

    private static JavaClasses tests() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages("io.github.dostepnykampus");
    }

    private static JavaClasses compiled(final Path dir, final String members)
        throws IOException {
        final ArchSamples samples = new ArchSamples(dir);
        samples.write(
            SharedFixtureArchitectureTest.SAMPLES, "Sample",
            String.format(
                "package %s;%nimport org.junit.jupiter.api.Test;%nfinal class Sample { %s }%n",
                SharedFixtureArchitectureTest.SAMPLES, members
            )
        );
        return samples.compiled(SharedFixtureArchitectureTest.SAMPLES);
    }
}
