/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Strażnik architektury modułu zgłoszeń (reguły A-1 do A-9).
 *
 * <p>Źródło: kontrakt modułu zgłoszeń (reguły ArchUnit) oraz decyzja 136:
 * {@code JdbcSubmissions} jest komponentem, więc granice adaptera pilnują
 * jawne reguły pakietów. Reguły dostają korzeń pakietów, więc te same reguły
 * sprawdzają kod produkcyjny i przykłady negatywne. Przykłady kompiluje test
 * do katalogu tymczasowego pod pakietem {@code archsamples}, poza modułem
 * zgłoszeń. Reguła na pustym zbiorze klas, także przez literówkę w pakiecie,
 * nie przechodzi.</p>
 *
 * @since 0.1.0
 */
final class BarrierArchitectureTest {

    /**
     * Korzeń pakietów modułu zgłoszeń.
     */
    private static final String MODULE = "io.github.dostepnykampus.barrier";

    /**
     * Korzeń pakietów przykładów negatywnych.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples";

    /**
     * Port repozytorium zgłoszeń.
     */
    private static final String REPO = "io.github.dostepnykampus.barrier.domain.SubmissionRepository";

    /**
     * Wejście do kodu budynków.
     */
    private static final String OUTLINES = "io.github.dostepnykampus.building.Outlines";

    /**
     * Adapter bazy, zawsze pod modułem zgłoszeń.
     */
    private static final String JDBC = "io.github.dostepnykampus.barrier.persistence.JdbcSubmissions";

    /**
     * Wyjątek HTTP Springa.
     */
    private static final String HTTP = "org.springframework.web.server.ResponseStatusException";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void barrierPassesOnCurrentCode(final String name, final ArchRule rule) {
        Assertions.assertDoesNotThrow(
            () -> rule.check(BarrierArchitectureTest.production()),
            String.format("Reguła %s jest złamana w kodzie produkcyjnym", name)
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("violations")
    void barrierRejectsViolatingSample(
        final String name, final String pack, final String body
    ) throws IOException {
        MatcherAssert.assertThat(
            String.format("Reguła %s przepuściła przykład negatywny", name),
            BarrierArchitectureTest.rule(name, BarrierArchitectureTest.SAMPLES)
                .evaluate(this.sample(pack, body)).hasViolation(),
            Matchers.is(true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void barrierFailsOnEmptyImport(final String name, final ArchRule rule) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> rule.check(new ClassFileImporter().importClasses()),
            String.format("Reguła %s przechodzi, choć nie sprawdziła żadnej klasy", name)
        );
    }

    private static Stream<Arguments> rules() {
        return Stream.of("A-1", "A-2", "A-3", "A-4", "A-5", "A-6", "A-7", "A-8", "A-9").map(
            name -> Arguments.of(
                name, BarrierArchitectureTest.rule(name, BarrierArchitectureTest.MODULE)
            )
        );
    }

    private static Stream<Arguments> violations() {
        final String subtype = "extends io.github.dostepnykampus.%s { %s s; Sample() { super(%s); } }";
        return Stream.of(
            Arguments.of("A-1", "domain", BarrierArchitectureTest.field(BarrierArchitectureTest.HTTP)),
            Arguments.of("A-1", "domain", BarrierArchitectureTest.field("tools.jackson.databind.JsonNode")),
            Arguments.of("A-1", "domain", BarrierArchitectureTest.field("java.sql.Connection")),
            Arguments.of("A-1", "domain", BarrierArchitectureTest.field(BarrierArchitectureTest.OUTLINES)),
            Arguments.of("A-2", "web", BarrierArchitectureTest.field("org.springframework.jdbc.core.simple.JdbcClient")),
            Arguments.of("A-2", "json", BarrierArchitectureTest.field("java.sql.Connection")),
            Arguments.of("A-3", "web", BarrierArchitectureTest.field("tools.jackson.databind.JsonNode")),
            Arguments.of("A-4", "web", BarrierArchitectureTest.field(BarrierArchitectureTest.JDBC)),
            Arguments.of("A-4", "web", BarrierArchitectureTest.field(BarrierArchitectureTest.REPO)),
            Arguments.of("A-5", "json", BarrierArchitectureTest.field(BarrierArchitectureTest.REPO)),
            Arguments.of("A-6", "web", BarrierArchitectureTest.field(BarrierArchitectureTest.OUTLINES)),
            Arguments.of("A-7", "domain", BarrierArchitectureTest.field(BarrierArchitectureTest.HTTP)),
            Arguments.of("A-8", "json", BarrierArchitectureTest.field(BarrierArchitectureTest.JDBC)),
            Arguments.of(
                "A-9", "errors",
                String.format(subtype, "ForbiddenChange", "org.springframework.http.HttpStatus", "\"x\"")
            ),
            Arguments.of(
                "A-9", "errors",
                String.format(subtype, "ConflictingState", "tools.jackson.databind.JsonNode", "\"x\", \"y\", null")
            )
        );
    }

    private static String field(final String type) {
        return String.format("{ %s field; }", type);
    }

    private static ArchRule rule(final String name, final String root) {
        final String all = String.format("%s..", root);
        final String persistence = String.format("%s.persistence..", root);
        final String service = String.format("%s.SubmissionService", MODULE);
        final String controller = String.format("%s.web.SubmissionsController(\\$.*)?", MODULE);
        final ArchRule result;
        switch (name) {
            case "A-1":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(String.format("%s.domain..", root))
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "tools.jackson..", "java.sql..",
                        "io.github.dostepnykampus.building.."
                    );
                break;
            case "A-2":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(all).and().resideOutsideOfPackage(persistence)
                    .should().dependOnClassesThat().resideInAnyPackage(
                        "java.sql..", "org.springframework.jdbc.."
                    );
                break;
            case "A-3":
                result = ArchRuleDefinition.noClasses().that().resideInAPackage(all)
                    .should().dependOnClassesThat().haveSimpleName("JsonNode");
                break;
            case "A-4":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(String.format("%s.web..", root))
                    .should().dependOnClassesThat().resideInAPackage("..barrier.persistence..")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName(REPO);
                break;
            case "A-5":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(all).and().resideOutsideOfPackage(persistence)
                    .and().doNotHaveFullyQualifiedName(service)
                    .should().dependOnClassesThat().haveFullyQualifiedName(REPO);
                break;
            case "A-6":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(all).and().doNotHaveFullyQualifiedName(service)
                    .should().dependOnClassesThat().haveFullyQualifiedName(OUTLINES);
                break;
            case "A-7":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(all).and().haveNameNotMatching(controller)
                    .should().dependOnClassesThat().haveSimpleName("ResponseStatusException");
                break;
            case "A-8":
                result = ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(all).and().resideOutsideOfPackage(persistence)
                    .should().dependOnClassesThat().resideInAPackage("..barrier.persistence..");
                break;
            default:
                result = ArchitectureTest.errorTypes();
                break;
        }
        return result.allowEmptyShould(false);
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private JavaClasses sample(final String pack, final String body) throws IOException {
        final String full = String.format("%s.%s", BarrierArchitectureTest.SAMPLES, pack);
        final Path source = this.dir.resolve("Sample.java");
        Files.writeString(
            source, String.format("package %s;%nfinal class Sample %s%n", full, body)
        );
        final int code = ToolProvider.getSystemJavaCompiler().run(
            null, null, null, "-d", this.dir.toString(), source.toString()
        );
        Assertions.assertEquals(0, code, "Przykład negatywny się nie skompilował");
        return new ClassFileImporter().importPath(this.dir.resolve(full.replace('.', '/')));
    }
}
