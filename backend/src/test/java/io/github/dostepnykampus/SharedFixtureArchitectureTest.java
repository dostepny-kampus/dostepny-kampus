/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        "ClassNameArchitectureIT",
        "ImmutableDomainArchitectureIT",
        "LayerDirectionArchitectureIT",
        "MockFreeArchitectureIT",
        "ModularityTest",
        "ModuleRootArchitectureIT",
        "NullabilityArchitectureIT",
        "PackageCycleArchitectureIT",
        "PackageDescriptionArchitectureIT",
        "PackageSizeArchitectureIT",
        "TechnologyArchitectureIT",
        "barrier.BarrierModuleTest",
        "building.BuildingReadContractIT",
        "building.BuildingsHttpIT"
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

    /**
     * Reguła: klasa z metodami testów nie ma wspólnego pola ani metody przygotowania.
     */
    private static final ExceptionListRule RULE = new ExceptionListRule(
        type -> SharedFixtureArchitectureTest.tested(type)
            && SharedFixtureArchitectureTest.shared(type),
        SharedFixtureArchitectureTest.KNOWN
    );

    @Test
    void testsPrepareOwnData() {
        Assertions.assertEquals(
            List.of(),
            SharedFixtureArchitectureTest.RULE.fresh(),
            "Klasa testu ma pole obiektu albo metodę przygotowania; dane przygotuj w teście"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        Assertions.assertEquals(
            List.of(),
            SharedFixtureArchitectureTest.RULE.stale(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
            private int count; @org.junit.jupiter.api.Test void check() { } | true
            @org.junit.jupiter.api.BeforeEach void prepare() { } @Test void check() { } | true
            @org.junit.jupiter.api.BeforeAll static void prepare() { } @Test void check() { } | true
            private static final int LIMIT = 1; @Test void check() { } | false
            @org.junit.jupiter.api.AfterEach void clean() { } @Test void check() { } | false
            private int count; void helper() { } | false
            """
    )
    void judgesSampleClass(final String members, final boolean rejected, @TempDir final Path dir)
        throws IOException {
        Assertions.assertEquals(
            rejected,
            SharedFixtureArchitectureTest.sample(dir, members).contains(
                String.format("%s.Sample", SharedFixtureArchitectureTest.SAMPLES)
            ),
            String.format("Reguła źle oceniła klasę z elementami: %s", members)
        );
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

    private static Set<String> sample(final Path dir, final String members) throws IOException {
        return SharedFixtureArchitectureTest.RULE.sample(
            dir, SharedFixtureArchitectureTest.SAMPLES,
            String.format(
                "package %s;%nimport org.junit.jupiter.api.Test;%nfinal class Sample { %s }%n",
                SharedFixtureArchitectureTest.SAMPLES, members
            )
        );
    }
}
