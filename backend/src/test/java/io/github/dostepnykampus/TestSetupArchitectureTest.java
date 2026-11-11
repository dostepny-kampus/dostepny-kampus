/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.tools.ToolProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Strażnik: testy na pełnym kontekście dzielą jeden kontekst Springa.
 *
 * <p>{@code @SpringBootTest} tylko w {@code IntegrationTest} i w klasach
 * z listy z powodem. Klasa z {@code @IntegrationTest}, która dokłada
 * {@code @TestPropertySource}, {@code @ActiveProfiles},
 * {@code @ContextConfiguration}, własny {@code @Import} albo metodę
 * {@code @DynamicPropertySource}, zakłada osobny kontekst, więc też musi być
 * na liście. {@code @DirtiesContext} jest zakazany, mocki Mockito tylko
 * z listy.</p>
 *
 * @since 0.1.0
 */
final class TestSetupArchitectureTest {

    /**
     * Klasy z własnym kontekstem, z powodem.
     */
    private static final Map<String, String> OWN_CONTEXT = Map.of(
        "io.github.dostepnykampus.barrier.PhotoBucketStartupTest",
        String.join(
            " ",
            "sprawdza utworzenie kubełka przy starcie kontekstu,",
            "więc potrzebuje świeżego kontekstu z własną nazwą kubełka"
        ),
        "io.github.dostepnykampus.KeycloakTokenTest",
        String.join(
            " ",
            "sprawdza token z prawdziwego Keycloaka domyślnym dekoderem,",
            "więc potrzebuje kontekstu bez tożsamości testowej"
        )
    );

    /**
     * Adnotacje klasy, które zmieniają klucz kontekstu.
     */
    private static final List<Class<? extends Annotation>> CONTEXT_MARKS = List.of(
        TestPropertySource.class, ActiveProfiles.class,
        ContextConfiguration.class, Import.class
    );

    /**
     * Klasy z {@code @MockitoBean} lub {@code @MockitoSpyBean}, z powodem.
     */
    private static final Map<String, String> MOCKED = Map.of();

    /** Klasy testowe aplikacji, importowane raz. */
    private static final JavaClasses TESTS = new ClassFileImporter()
        .withImportOption(location -> location.contains("/test-classes/"))
        .importPackagesOf(IntegrationTest.class);

    /**
     * Przykłady, każdy w pakiecie {@code fixture.s<numer>}, i ich skompilowane klasy.
     */
    private static final List<Sample> SAMPLES = TestSetupArchitectureTest.samples();

    /** Klasy przykładów, skompilowane raz. */
    private static final JavaClasses COMPILED = TestSetupArchitectureTest.compile(
        TestSetupArchitectureTest.SAMPLES
    );

    @Test
    void springBootTestOnlyInIntegrationTestAndListedClasses() {
        final Set<String> all = TestSetupArchitectureTest.OWN_CONTEXT.keySet();
        TestSetupArchitectureTest.bare(all).check(TestSetupArchitectureTest.TESTS);
        TestSetupArchitectureTest.listed(all).check(TestSetupArchitectureTest.TESTS);
        TestSetupArchitectureTest.unlisted(all).check(TestSetupArchitectureTest.TESTS);
    }

    @Test
    void noDirtiesContext() {
        TestSetupArchitectureTest.forbidden(Set.of(), List.of(DirtiesContext.class))
            .check(TestSetupArchitectureTest.TESTS);
    }

    @Test
    void mockitoBeansOnlyInListedClasses() {
        TestSetupArchitectureTest.forbidden(
            TestSetupArchitectureTest.MOCKED.keySet(),
            List.of(MockitoBean.class, MockitoSpyBean.class)
        ).check(TestSetupArchitectureTest.TESTS);
    }

    @ParameterizedTest
    @MethodSource("indexes")
    void judgesFixtureClasses(final int index) {
        final Sample sample = TestSetupArchitectureTest.SAMPLES.get(index);
        final String pack = String.format("fixture.s%d", index);
        boolean failed = false;
        try {
            sample.rule().apply(Set.of(pack.concat(".Kept"))).check(
                TestSetupArchitectureTest.COMPILED.that(
                    DescribedPredicate.describe(pack, item -> pack.equals(item.getPackageName()))
                )
            );
        } catch (final AssertionError ex) {
            failed = true;
        }
        MatcherAssert.assertThat(
            sample.code(), failed, Matchers.is(sample.violating())
        );
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("indexes")
    void failsOnEmptyImport(final int index) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> TestSetupArchitectureTest.SAMPLES.get(index).rule().apply(Set.of())
                .check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    private static IntStream indexes() {
        return IntStream.range(0, TestSetupArchitectureTest.SAMPLES.size());
    }

    private static List<Sample> samples() {
        final Function<Set<String>, ArchRule> bare = TestSetupArchitectureTest::bare;
        final Function<Set<String>, ArchRule> listed = TestSetupArchitectureTest::listed;
        final Function<Set<String>, ArchRule> unlisted = TestSetupArchitectureTest::unlisted;
        final Function<Set<String>, ArchRule> dirty = kept -> TestSetupArchitectureTest
            .forbidden(Set.of(), List.of(DirtiesContext.class));
        final Function<Set<String>, ArchRule> mock = kept -> TestSetupArchitectureTest
            .forbidden(kept, List.of(MockitoBean.class, MockitoSpyBean.class));
        return List.of(
            new Sample(bare, "@SpringBootTest class A { }", true),
            new Sample(bare, "@SpringBootTest class Kept { }", false),
            new Sample(bare, "@IntegrationTest class A { }", false),
            new Sample(listed, "@SpringBootTest class Kept { }", false),
            new Sample(listed, "@IntegrationTest class Kept { }", true),
            new Sample(listed, "class Other { }", true),
            new Sample(listed, "@IntegrationTest @ActiveProfiles(\"x\") class Kept { }", false),
            new Sample(unlisted, "@IntegrationTest @TestPropertySource(\"a=b\") class A { }", true),
            new Sample(unlisted, "@IntegrationTest @ActiveProfiles(\"x\") class A { }", true),
            new Sample(
                unlisted,
                String.join(
                    " ", "@IntegrationTest class A {",
                    "@DynamicPropertySource static void f(DynamicPropertyRegistry r) { } }"
                ),
                true
            ),
            new Sample(unlisted, "@IntegrationTest @ContextConfiguration class A { }", true),
            new Sample(unlisted, "@IntegrationTest @Import(Object.class) class A { }", true),
            new Sample(unlisted, "@IntegrationTest @ActiveProfiles(\"x\") class Kept { }", false),
            new Sample(unlisted, "@IntegrationTest class A { }", false),
            new Sample(dirty, "@DirtiesContext class A { }", true),
            new Sample(dirty, "class A { @DirtiesContext void f() { } }", true),
            new Sample(dirty, "class A { void f() { } }", false),
            new Sample(mock, "class A { @MockitoBean Object v; }", true),
            new Sample(mock, "class A { @MockitoSpyBean Object v; }", true),
            new Sample(mock, "class Kept { @MockitoBean Object v; }", false)
        );
    }

    private static ArchRule bare(final Set<String> allowed) {
        return TestSetupArchitectureTest.rule(
            "have @SpringBootTest only when listed",
            item -> item.isAnnotatedWith(SpringBootTest.class)
                && !allowed.contains(item.getName())
                && !IntegrationTest.class.getName().equals(item.getName()),
            Set.of()
        );
    }

    private static ArchRule rule(
        final String text, final Predicate<JavaClass> bad, final Set<String> expected
    ) {
        return ArchRuleDefinition.classes().should(
            new ArchCondition<JavaClass>(text) {
                private final Set<String> seen = new HashSet<>();

                @Override
                public void check(final JavaClass item, final ConditionEvents events) {
                    this.seen.add(item.getName());
                    if (bad.test(item)) {
                        events.add(SimpleConditionEvent.violated(item, item.getName()));
                    }
                }

                @Override
                public void finish(final ConditionEvents events) {
                    expected.stream().filter(name -> !this.seen.contains(name)).forEach(
                        name -> events.add(SimpleConditionEvent.violated(name, name))
                    );
                }
            }
        );
    }

    private static ArchRule listed(final Set<String> names) {
        return TestSetupArchitectureTest.rule(
            "be listed only while it has its own context",
            item -> names.contains(item.getName())
                && !item.isAnnotatedWith(SpringBootTest.class)
                && !TestSetupArchitectureTest.own(item),
            names
        );
    }

    private static ArchRule unlisted(final Set<String> allowed) {
        return TestSetupArchitectureTest.rule(
            "have its own context settings only when listed",
            item -> TestSetupArchitectureTest.own(item) && !allowed.contains(item.getName()),
            Set.of()
        );
    }

    private static boolean own(final JavaClass item) {
        return item.isAnnotatedWith(IntegrationTest.class)
            && (TestSetupArchitectureTest.marked(item) || TestSetupArchitectureTest.dynamic(item));
    }

    private static boolean marked(final JavaClass item) {
        return TestSetupArchitectureTest.CONTEXT_MARKS.stream().anyMatch(item::isAnnotatedWith);
    }

    private static boolean dynamic(final JavaClass item) {
        return item.getMethods().stream().anyMatch(TestSetupArchitectureTest::dynamic);
    }

    private static boolean dynamic(final JavaMethod method) {
        return method.isAnnotatedWith(DynamicPropertySource.class);
    }

    private static ArchRule forbidden(
        final Set<String> allowed, final List<Class<? extends Annotation>> marks
    ) {
        return TestSetupArchitectureTest.rule(
            "not use the annotations outside the listed classes",
            item -> {
                final List<HasAnnotations<?>> places = new ArrayList<>(List.of(item));
                places.addAll(item.getMethods());
                places.addAll(item.getFields());
                return !allowed.contains(item.getName()) && places.stream().anyMatch(
                    place -> marks.stream().anyMatch(
                        mark -> place.isAnnotatedWith(mark) || place.isMetaAnnotatedWith(mark)
                    )
                );
            },
            Set.of()
        );
    }

    private static JavaClasses compile(final List<Sample> samples) {
        try {
            final Path root = Files.createTempDirectory("fixtures");
            final Path out = root.resolve("out");
            final List<String> args = new ArrayList<>(
                List.of("-d", out.toString(), "-classpath", TestSetupArchitectureTest.classpath())
            );
            for (int idx = 0; idx < samples.size(); ++idx) {
                final Path source = root.resolve(String.format("S%d.java", idx));
                Files.writeString(
                    source,
                    String.format(
                        "package fixture.s%d;%n%s%n%s%n", idx,
                        String.join(
                            " ", "import io.github.dostepnykampus.IntegrationTest;",
                            "import org.springframework.context.annotation.Import;",
                            "import org.springframework.test.context.*;",
                            "import org.springframework.boot.test.context.*;",
                            "import org.springframework.test.annotation.*;",
                            "import org.springframework.test.context.bean.override.mockito.*;"
                        ), samples.get(idx).code()
                    )
                );
                args.add(source.toString());
            }
            if (ToolProvider.getSystemJavaCompiler().run(
                null, null, null, args.toArray(new String[0])
            ) != 0) {
                throw new IllegalStateException("Przykłady się nie skompilowały");
            }
            return new ClassFileImporter().importPath(out);
        } catch (final IOException | URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String classpath() throws URISyntaxException {
        final List<String> parts = new ArrayList<>(4);
        for (final Class<?> type : List.of(
            SpringBootTest.class, Import.class, MockitoBean.class, IntegrationTest.class
        )) {
            parts.add(
                Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toString()
            );
        }
        return String.join(File.pathSeparator, parts);
    }

    /**
     * Przykład: reguła z nazwy pakietu, źródło klas i czy reguła ma je odrzucić.
     *
     * @param rule Reguła sprawdzana na przykładzie
     * @param code Kod klas przykładu
     * @param violating Czy reguła ma zgłosić naruszenie
     * @since 0.1.0
     */
    record Sample(Function<Set<String>, ArchRule> rule, String code, boolean violating) {
    }
}
