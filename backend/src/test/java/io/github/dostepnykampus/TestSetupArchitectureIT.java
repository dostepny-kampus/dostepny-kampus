/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.servlet.Filter;
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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.WebApplicationContext;

/**
 * Strażnik: testy dzielą wspólne konteksty Springa. {@code @SpringBootTest} tylko w
 * {@code IntegrationTest}, {@code @ApplicationModuleTest} tylko w {@code ModuleTest}; klasa
 * z własną konfiguracją kontekstu ({@code @Import}, profile, właściwości) tylko z listy.
 * {@code @DirtiesContext} jest zakazany, mocki Mockito tylko z listy. Klienta HTTP buduje tylko
 * {@link MockMvcTestConfiguration}; wyjątki są na liście z powodem, a lista tylko maleje.
 *
 * @since 0.1.0
 */
final class TestSetupArchitectureIT {

    /**
     * Klasy z własnym kontekstem, z powodem.
     */
    private static final Map<String, String> OWN_CONTEXT = Map.of(
        "io.github.dostepnykampus.barrier.photo.storage.PhotoBucketStartupIT",
        String.join(
            " ",
            "sprawdza utworzenie kubełka przy starcie kontekstu,",
            "więc potrzebuje świeżego kontekstu z własną nazwą kubełka"
        ),
        "io.github.dostepnykampus.barrier.BarrierModuleIT",
        String.join(
            " ",
            "kontekst modułu zgłoszeń potrzebuje magazynu zdjęć,",
            "którego konteksty modułów budynków i tożsamości nie mają"
        ),
        "io.github.dostepnykampus.KeycloakTokenIT",
        String.join(
            " ",
            "sprawdza token z prawdziwego Keycloaka domyślnym dekoderem,",
            "więc potrzebuje kontekstu bez tożsamości testowej"
        ),
        "io.github.dostepnykampus.loginspecs.SelfRegistrationSteps",
        String.join(
            " ",
            "kroki scenariuszy zakładają konto w prawdziwym Keycloaku,",
            "więc potrzebują kontekstu bez tożsamości testowej"
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
    private static final Map<String, String> MOCKED = Map.of(
        "io.github.dostepnykampus.barrier.BarrierModuleIT",
        "zastępuje typy API modułu budynków, którego kontekst modułu zgłoszeń nie ładuje"
    );

    /**
     * Klasy, które budują klienta HTTP same, z powodem; lista może tylko maleć.
     */
    private static final Map<String, String> OWN_CLIENT = Map.of(
        "io.github.dostepnykampus.OpenApiContractIT",
        String.join(
            " ",
            "sprawdza samą kontrolę kontraktu na kontrolerze z gotową odpowiedzią,",
            "bez kontekstu aplikacji"
        ),
        "io.github.dostepnykampus.barrier.SubmissionPhotosHttpIT",
        String.join(
            " ",
            "buduje kontroler zdjęć z kubełkiem, którego nie ma,",
            "a wspólne MockMvc ma kubełek z konfiguracji"
        )
    );

    /**
     * Metody budujące klienta testowego HTTP, po klasie właściciela.
     */
    private static final Map<String, Predicate<String>> BUILDERS = Map.of(
        "org.springframework.test.web.servlet.setup.MockMvcBuilders", name -> true,
        "org.springframework.test.web.servlet.assertj.MockMvcTester",
        name -> List.of("create", "from", "of").contains(name),
        "org.springframework.test.web.reactive.server.WebTestClient",
        name -> name.startsWith("bindTo")
    );

    /** Klasy testowe aplikacji, importowane raz. */
    private static final JavaClasses TESTS = new ClassFileImporter()
        .withImportOption(location -> location.contains("/test-classes/"))
        .importPackagesOf(IntegrationTest.class);

    /**
     * Przykłady, każdy w pakiecie {@code fixture.s<numer>}, i ich skompilowane klasy.
     */
    private static final List<Sample> SAMPLES = TestSetupArchitectureIT.samples();

    /** Klasy przykładów, skompilowane raz. */
    private static final JavaClasses COMPILED = TestSetupArchitectureIT.compile(
        TestSetupArchitectureIT.SAMPLES
    );

    @Test
    void limitsOwnSpringContextToListedClasses() {
        final Set<String> all = TestSetupArchitectureIT.OWN_CONTEXT.keySet();
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                TestSetupArchitectureIT.TESTS,
                TestSetupArchitectureIT.bare(all),
                TestSetupArchitectureIT.listed(all),
                TestSetupArchitectureIT.unlisted(all)
            ),
            "Kontekst Springa poza wspólnymi adnotacjami i listą klas z własnym kontekstem"
        );
    }

    @Test
    void forbidsDirtiesContext() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                TestSetupArchitectureIT.TESTS,
                TestSetupArchitectureIT.forbidden(Set.of(), List.of(DirtiesContext.class))
            ),
            "Test używa @DirtiesContext"
        );
    }

    @Test
    void allowsMockitoBeansOnlyInListedClasses() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                TestSetupArchitectureIT.TESTS,
                TestSetupArchitectureIT.forbidden(
                    TestSetupArchitectureIT.MOCKED.keySet(),
                    List.of(MockitoBean.class, MockitoSpyBean.class)
                )
            ),
            "Mock Springa w klasie spoza listy"
        );
    }

    @Test
    void buildsHttpClientsOnlyInConfiguration() {
        final Set<String> all = TestSetupArchitectureIT.OWN_CLIENT.keySet();
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                TestSetupArchitectureIT.TESTS,
                TestSetupArchitectureIT.clients(all),
                TestSetupArchitectureIT.ownClients(all)
            ),
            "Klient HTTP zbudowany poza wspólną konfiguracją i listą"
        );
    }

    @ParameterizedTest
    @MethodSource("indexes")
    void judgesFixtureClasses(final int index) {
        final Sample sample = TestSetupArchitectureIT.SAMPLES.get(index);
        final String pack = String.format("fixture.s%d", index);
        boolean failed = false;
        try {
            sample.rule().apply(Set.of(pack.concat(".Kept"))).check(
                TestSetupArchitectureIT.COMPILED.that(
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
            () -> TestSetupArchitectureIT.SAMPLES.get(index).rule().apply(Set.of())
                .check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    private static IntStream indexes() {
        return IntStream.range(0, TestSetupArchitectureIT.SAMPLES.size());
    }

    private static List<Sample> samples() {
        final Function<Set<String>, ArchRule> bare = TestSetupArchitectureIT::bare;
        final Function<Set<String>, ArchRule> listed = TestSetupArchitectureIT::listed;
        final Function<Set<String>, ArchRule> unlisted = TestSetupArchitectureIT::unlisted;
        final Function<Set<String>, ArchRule> dirty = kept -> TestSetupArchitectureIT
            .forbidden(Set.of(), List.of(DirtiesContext.class));
        final Function<Set<String>, ArchRule> mock = kept -> TestSetupArchitectureIT
            .forbidden(kept, List.of(MockitoBean.class, MockitoSpyBean.class));
        final Function<Set<String>, ArchRule> client = TestSetupArchitectureIT::clients;
        final Function<Set<String>, ArchRule> owned = TestSetupArchitectureIT::ownClients;
        final String context = String.join(
            " ", "Object f(WebApplicationContext c) {",
            "return MockMvcBuilders.webAppContextSetup(c); } }"
        );
        return List.of(
            new Sample(bare, "@SpringBootTest class A { }", true),
            new Sample(bare, "@SpringBootTest class Kept { }", false),
            new Sample(bare, "@IntegrationTest class A { }", false),
            new Sample(bare, "@ApplicationModuleTest class A { }", true),
            new Sample(bare, "@ApplicationModuleTest class Kept { }", false),
            new Sample(bare, "@ModuleTest class A { }", false),
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
            new Sample(unlisted, "@ModuleTest @Import(Object.class) class A { }", true),
            new Sample(unlisted, "@ModuleTest class A { }", false),
            new Sample(dirty, "@DirtiesContext class A { }", true),
            new Sample(dirty, "class A { @DirtiesContext void f() { } }", true),
            new Sample(dirty, "class A { void f() { } }", false),
            new Sample(mock, "class A { @MockitoBean Object v; }", true),
            new Sample(mock, "class A { @MockitoSpyBean Object v; }", true),
            new Sample(mock, "class Kept { @MockitoBean Object v; }", false),
            new Sample(client, "class A { ".concat(context), true),
            new Sample(
                client,
                "class A { Object f(Object c) { return MockMvcBuilders.standaloneSetup(c); } }", true
            ),
            new Sample(
                client, "class A { Object f(MockMvc m) { return MockMvcTester.create(m); } }", true
            ),
            new Sample(
                client,
                "class A { Object f(MockMvc m) throws Exception { return m.perform(null); } }", false
            ),
            new Sample(client, "class Kept { ".concat(context), false),
            new Sample(owned, "class Kept { ".concat(context), false),
            new Sample(owned, "class Kept { }", true)
        );
    }

    private static ArchRule bare(final Set<String> allowed) {
        return TestSetupArchitectureIT.rule(
            "have @SpringBootTest or @ApplicationModuleTest only when listed",
            item -> !allowed.contains(item.getName())
                && (
                    TestSetupArchitectureIT.bare(
                        item, SpringBootTest.class, IntegrationTest.class
                    )
                    || TestSetupArchitectureIT.bare(
                        item, ApplicationModuleTest.class, ModuleTest.class
                    )
                ),
            Set.of()
        );
    }

    private static boolean bare(
        final JavaClass item, final Class<? extends Annotation> context,
        final Class<? extends Annotation> shared
    ) {
        return item.isAnnotatedWith(context) && !shared.getName().equals(item.getName());
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
        return TestSetupArchitectureIT.rule(
            "be listed only while it has its own context",
            item -> names.contains(item.getName())
                && !item.isAnnotatedWith(SpringBootTest.class)
                && !TestSetupArchitectureIT.own(item),
            names
        );
    }

    private static ArchRule unlisted(final Set<String> allowed) {
        return TestSetupArchitectureIT.rule(
            "have its own context settings only when listed",
            item -> TestSetupArchitectureIT.own(item) && !allowed.contains(item.getName()),
            Set.of()
        );
    }

    private static ArchRule clients(final Set<String> allowed) {
        return TestSetupArchitectureIT.rule(
            String.join(
                " ",
                "build HTTP test clients only in MockMvcTestConfiguration:",
                "wstrzyknij wspólne MockMvc, które samo sprawdza kontrakt"
            ),
            item -> TestSetupArchitectureIT.builds(item)
                && !allowed.contains(item.getName())
                && !MockMvcTestConfiguration.class.getName().equals(item.getName()),
            Set.of()
        );
    }

    private static ArchRule ownClients(final Set<String> names) {
        return TestSetupArchitectureIT.rule(
            "be listed only while it builds its own HTTP test client",
            item -> names.contains(item.getName()) && !TestSetupArchitectureIT.builds(item),
            names
        );
    }

    private static boolean builds(final JavaClass item) {
        return item.getMethodCallsFromSelf().stream().anyMatch(TestSetupArchitectureIT::builds);
    }

    private static boolean builds(final JavaMethodCall call) {
        return TestSetupArchitectureIT.BUILDERS
            .getOrDefault(call.getTargetOwner().getName(), name -> false)
            .test(call.getName());
    }

    private static boolean own(final JavaClass item) {
        return TestSetupArchitectureIT.shared(item)
            && (TestSetupArchitectureIT.marked(item) || TestSetupArchitectureIT.dynamic(item));
    }

    private static boolean shared(final JavaClass item) {
        return item.isAnnotatedWith(IntegrationTest.class)
            || item.isAnnotatedWith(ModuleTest.class);
    }

    private static boolean marked(final JavaClass item) {
        return TestSetupArchitectureIT.CONTEXT_MARKS.stream().anyMatch(item::isAnnotatedWith);
    }

    private static boolean dynamic(final JavaClass item) {
        return item.getMethods().stream().anyMatch(TestSetupArchitectureIT::dynamic);
    }

    private static boolean dynamic(final JavaMethod method) {
        return method.isAnnotatedWith(DynamicPropertySource.class);
    }

    private static ArchRule forbidden(
        final Set<String> allowed, final List<Class<? extends Annotation>> marks
    ) {
        return TestSetupArchitectureIT.rule(
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
            final ArchSamples fixtures = new ArchSamples(Files.createTempDirectory("fixtures"));
            for (int idx = 0; idx < samples.size(); ++idx) {
                fixtures.write(
                    String.format("fixture.s%d", idx), String.format("S%d", idx),
                    String.format(
                        "package fixture.s%d;%n%s%n%s%n", idx,
                        String.join(
                            " ", "import io.github.dostepnykampus.IntegrationTest;",
                            "import io.github.dostepnykampus.ModuleTest;",
                            "import org.springframework.modulith.test.*;",
                            "import org.springframework.context.annotation.Import;",
                            "import org.springframework.test.context.*;",
                            "import org.springframework.boot.test.context.*;",
                            "import org.springframework.test.annotation.*;",
                            "import org.springframework.test.context.bean.override.mockito.*;",
                            "import org.springframework.test.web.servlet.*;",
                            "import org.springframework.test.web.servlet.setup.*;",
                            "import org.springframework.test.web.servlet.assertj.*;",
                            "import org.springframework.web.context.*;"
                        ), samples.get(idx).code()
                    )
                );
            }
            return fixtures.compiled(
                "fixture", "-classpath", TestSetupArchitectureIT.classpath()
            );
        } catch (final IOException | URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String classpath() throws URISyntaxException {
        final List<String> parts = new ArrayList<>(9);
        for (final Class<?> type : List.of(
            SpringBootTest.class, Import.class, MockitoBean.class, IntegrationTest.class,
            WebApplicationContext.class, Filter.class, MockMvcTester.class,
            ApplicationModuleTest.class
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
