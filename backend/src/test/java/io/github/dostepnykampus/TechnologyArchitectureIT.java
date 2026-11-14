/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reguły technologii w każdym zamkniętym module serwera, wyrażone warstwami z {@link ModuleLayers}:
 * A-2 JDBC tylko w persistence, A-3 web bez {@code JsonNode}, A-4 port dziedziny z implementacją
 * w adapterze tylko w application i adapterach, A-6 API innego modułu tylko w application, A-7
 * wyjątki HTTP tylko w web. A-9 (typy błędów bez HTTP i Jacksona) sprawdza na kodzie
 * {@link ArchitectureTest}, a tu są jej przykłady negatywne. Dziedzinę bez frameworków i adapter
 * bez adaptera pilnuje {@link LayerDirectionArchitectureTest}. Lista wyjątków tylko maleje.
 *
 * @since 0.1.0
 */
final class TechnologyArchitectureIT {

    /**
     * Reguły sprawdzane w modułach.
     */
    private static final List<String> RULES = List.of("A-2", "A-3", "A-4", "A-6", "A-7");

    /**
     * Pakiety, które dziś łamią regułę, i issue, które to usuwa; klucz to reguła i pakiet.
     */
    private static final Map<String, Integer> KNOWN = Map.of();

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.technology";

    /**
     * Wyjątek HTTP Springa.
     */
    private static final String HTTP = "org.springframework.web.server.ResponseStatusException";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void modulesKeepTechnologyRules() {
        final JavaClasses classes = TechnologyArchitectureIT.production();
        Assertions.assertAll(
            TechnologyArchitectureIT.RULES.stream().map(
                name -> () -> Assertions.assertDoesNotThrow(
                    () -> TechnologyArchitectureIT.rule(
                        name, ModuleLayers.modules(), TechnologyArchitectureIT.skipped(name)
                    ).check(classes),
                    String.format("Reguła %s jest złamana w kodzie produkcyjnym", name)
                )
            )
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final JavaClasses classes = TechnologyArchitectureIT.production();
        Assertions.assertEquals(
            List.of(),
            TechnologyArchitectureIT.KNOWN.keySet().stream().filter(
                key -> !TechnologyArchitectureIT.rule(
                    key.split(" ", 2)[0], ModuleLayers.modules(), List.of()
                ).evaluate(
                    classes.that(TechnologyArchitectureIT.within(List.of(key.split(" ", 2)[1])))
                ).hasViolation()
            ).toList(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @Test
    void acceptsPortAndOtherModuleInApplication() throws IOException {
        this.sample("own.application", "own.submission.Port", "other.api.Outlines");
        final JavaClasses classes = this.compiled();
        Assertions.assertAll(
            TechnologyArchitectureIT.RULES.stream().map(
                name -> () -> Assertions.assertFalse(
                    TechnologyArchitectureIT.rule(name, this.roots(), List.of())
                        .evaluate(classes).hasViolation(),
                    String.format("Reguła %s odrzuciła zależność dozwoloną", name)
                )
            )
        );
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("violations")
    void rejectsViolatingSample(final String name, final String pack, final String type)
        throws IOException {
        this.sample(pack, type);
        Assertions.assertTrue(
            TechnologyArchitectureIT.rule(name, this.roots(), List.of())
                .evaluate(this.compiled()).hasViolation(),
            String.format("Reguła %s przepuściła przykład negatywny", name)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void rejectsErrorTypeWithHttpOrJackson(final String body) throws IOException {
        final String full = String.format("%s.errors", TechnologyArchitectureIT.SAMPLES);
        final ArchSamples samples = new ArchSamples(this.dir);
        samples.write(
            full, "Sample", String.format("package %s;%nfinal class Sample %s%n", full, body)
        );
        Assertions.assertTrue(
            ArchitectureTest.errorTypes().evaluate(samples.compiled(full)).hasViolation(),
            "Reguła A-9 przepuściła typ błędu zależny od HTTP albo Jacksona"
        );
    }

    private static Stream<String> errors() {
        final String subtype =
            "extends io.github.dostepnykampus.%s { %s s; Sample() { super(%s); } }";
        return Stream.of(
            String.format(
                subtype, "shared.error.ForbiddenChange", "org.springframework.http.HttpStatus",
                "\"x\""
            ),
            String.format(
                subtype, "shared.error.ConflictingState", "tools.jackson.databind.JsonNode",
                "\"x\", \"y\", null"
            )
        );
    }

    private static Stream<Arguments> violations() {
        final String port = "own.submission.Port";
        final String api = "other.api.Outlines";
        return Stream.of(
            Arguments.of("A-2", "own.web", "java.sql.Connection"),
            Arguments.of("A-2", "own.json", "org.springframework.jdbc.core.simple.JdbcClient"),
            Arguments.of("A-3", "own.web", "tools.jackson.databind.JsonNode"),
            Arguments.of("A-3", "own.web", "tools.jackson.databind.node.ObjectNode"),
            Arguments.of("A-4", "own.web", port),
            Arguments.of("A-4", "own.json", port),
            Arguments.of("A-4", "own.photo", port),
            Arguments.of("A-6", "own.web", api),
            Arguments.of("A-6", "own.submission", api),
            Arguments.of("A-7", "own.submission", TechnologyArchitectureIT.HTTP),
            Arguments.of(
                "A-7", "own.application", "org.springframework.web.server.ServerWebInputException"
            )
        );
    }

    private static ArchRule rule(
        final String name, final Collection<String> roots, final Collection<String> skipped
    ) {
        final DescribedPredicate<JavaClass> checked = TechnologyArchitectureIT.within(roots)
            .and(DescribedPredicate.not(TechnologyArchitectureIT.within(skipped)));
        return switch (name) {
            case "A-2" -> TechnologyArchitectureIT.local(
                checked, TechnologyArchitectureIT.except(roots, "persistence"),
                JavaClass.Predicates.resideInAnyPackage("java.sql..", "org.springframework.jdbc..")
            );
            case "A-3" -> TechnologyArchitectureIT.local(
                checked, ModuleLayers.in(roots, "web"),
                JavaClass.Predicates.assignableTo("tools.jackson.databind.JsonNode")
            );
            case "A-4" -> TechnologyArchitectureIT.local(
                checked,
                TechnologyArchitectureIT.except(roots, "application", "persistence", "storage"),
                TechnologyArchitectureIT.port(roots)
            );
            case "A-6" -> CompositeArchRule.of(
                roots.stream().map(
                    root -> TechnologyArchitectureIT.foreign(root, roots, checked)
                ).toList()
            ).allowEmptyShould(true);
            case null, default -> TechnologyArchitectureIT.local(
                checked, TechnologyArchitectureIT.except(roots, "web"),
                JavaClass.Predicates.assignableTo(TechnologyArchitectureIT.HTTP)
            );
        };
    }

    private static ArchRule local(
        final DescribedPredicate<JavaClass> checked,
        final DescribedPredicate<JavaClass> origin,
        final DescribedPredicate<JavaClass> forbidden
    ) {
        return ArchRuleDefinition.noClasses().that(checked).and(origin)
            .should().dependOnClassesThat(forbidden).allowEmptyShould(true);
    }

    private static ArchRule foreign(
        final String root, final Collection<String> roots,
        final DescribedPredicate<JavaClass> checked
    ) {
        return TechnologyArchitectureIT.local(
            checked.and(TechnologyArchitectureIT.within(List.of(root))),
            TechnologyArchitectureIT.except(List.of(root), "application"),
            ModuleLayers.in(roots.stream().filter(other -> !other.equals(root)).toList(), "api")
        );
    }

    private static DescribedPredicate<JavaClass> port(final Collection<String> roots) {
        final DescribedPredicate<JavaClass> adapter = ModuleLayers.in(roots, "persistence")
            .or(ModuleLayers.in(roots, "storage"));
        return DescribedPredicate.describe(
            "port dziedziny z implementacją w adapterze",
            (JavaClass type) -> type.isInterface()
                && ModuleLayers.in(roots, "domain").test(type)
                && type.getAllSubclasses().stream().anyMatch(adapter)
        );
    }

    private static DescribedPredicate<JavaClass> except(
        final Collection<String> roots, final String... layers
    ) {
        return DescribedPredicate.describe(
            String.format("poza warstwami %s", String.join(", ", layers)),
            (JavaClass type) -> Stream.of(layers)
                .noneMatch(layer -> ModuleLayers.in(roots, layer).test(type))
        );
    }

    private static DescribedPredicate<JavaClass> within(final Collection<String> packs) {
        return DescribedPredicate.describe(
            "w pakiecie z podpakietami",
            (JavaClass type) -> packs.stream().anyMatch(
                pack -> String.format("%s.", type.getPackageName())
                    .startsWith(String.format("%s.", pack))
            )
        );
    }

    private static List<String> skipped(final String name) {
        return TechnologyArchitectureIT.KNOWN.keySet().stream()
            .filter(key -> key.startsWith(String.format("%s ", name)))
            .map(key -> key.split(" ", 2)[1])
            .toList();
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.dostepnykampus");
    }

    private List<String> roots() {
        return Stream.of("own", "other")
            .map(root -> String.format("%s.%s", TechnologyArchitectureIT.SAMPLES, root))
            .toList();
    }

    private void sample(final String pack, final String... types) throws IOException {
        this.type("own.submission", "Port", "public interface Port { }");
        this.type(
            "own.persistence", "JdbcPort",
            String.format(
                "public final class JdbcPort implements %s.own.submission.Port { }",
                TechnologyArchitectureIT.SAMPLES
            )
        );
        this.type("other.api", "Outlines", "public interface Outlines { }");
        final StringBuilder fields = new StringBuilder();
        for (final String type : types) {
            String full = type;
            if (type.startsWith("own.") || type.startsWith("other.")) {
                full = String.format("%s.%s", TechnologyArchitectureIT.SAMPLES, type);
            }
            fields.append(String.format("%s f%d; ", full, fields.length()));
        }
        this.type(pack, "Sample", String.format("public final class Sample { %s}", fields));
    }

    private void type(final String pack, final String name, final String body)
        throws IOException {
        final String full = String.format("%s.%s", TechnologyArchitectureIT.SAMPLES, pack);
        new ArchSamples(this.dir).write(full, name, String.format("package %s;%n%s%n", full, body));
    }

    private JavaClasses compiled() throws IOException {
        return new ArchSamples(this.dir).compiled(TechnologyArchitectureIT.SAMPLES);
    }
}
