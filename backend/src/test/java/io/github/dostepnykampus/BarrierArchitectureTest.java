/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Strażnik architektury modułu zgłoszeń (reguły A-1 do A-9). {@code JdbcSubmissions} jest
 * komponentem, więc granice adaptera pilnują jawne reguły pakietów. Te same reguły sprawdzają
 * kod produkcyjny i przykłady negatywne, kompilowane do katalogu tymczasowego pod pakietem
 * {@code archsamples}. Reguła na pustym zbiorze klas nie przechodzi
 * ({@code allowEmptyShould(false)}).
 *
 * @since 0.1.0
 */
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class BarrierArchitectureTest {

    /**
     * Korzeń pakietów modułu zgłoszeń.
     */
    private static final String MODULE = "io.github.dostepnykampus.barrier";

    /**
     * Korzeń pakietów przykładów negatywnych.
     */
    private static final String SAMPLES =
        "io.github.dostepnykampus.archsamples";

    /**
     * Port repozytorium zgłoszeń.
     */
    private static final String REPO =
        "io.github.dostepnykampus.barrier.submission.SubmissionRepository";

    /**
     * Wejście do kodu budynków.
     */
    private static final String OUTLINES = "io.github.dostepnykampus.building.Outlines";

    /**
     * Adapter bazy, zawsze pod modułem zgłoszeń.
     */
    private static final String JDBC =
        "io.github.dostepnykampus.barrier.persistence.JdbcSubmissions";

    /**
     * Wyjątek HTTP Springa.
     */
    private static final String HTTP = "org.springframework.web.server.ResponseStatusException";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @ArchTest
    void barrierPassesOnCurrentCode(final JavaClasses classes) {
        Assertions.assertAll(
            Stream.of("A-1", "A-2", "A-3", "A-4", "A-5", "A-6", "A-7", "A-8", "A-9").map(
                name -> () -> Assertions.assertDoesNotThrow(
                    () -> BarrierArchitectureTest.rule(name, BarrierArchitectureTest.MODULE)
                        .check(classes),
                    String.format("Reguła %s jest złamana w kodzie produkcyjnym", name)
                )
            )
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
                .evaluate(this.compiled(pack, body)).hasViolation(),
            Matchers.is(true)
        );
    }

    private static Stream<Arguments> violations() {
        final String jackson = "tools.jackson.databind.JsonNode";
        final String sql = "java.sql.Connection";
        final String subtype =
            "extends io.github.dostepnykampus.%s { %s s; Sample() { super(%s); } }";
        return Stream.of(
            BarrierArchitectureTest.sample("A-1", "submission", BarrierArchitectureTest.HTTP),
            BarrierArchitectureTest.sample("A-1", "submission", jackson),
            BarrierArchitectureTest.sample("A-1", "submission", sql),
            BarrierArchitectureTest.sample("A-1", "submission", BarrierArchitectureTest.OUTLINES),
            BarrierArchitectureTest.sample(
                "A-2", "web", "org.springframework.jdbc.core.simple.JdbcClient"
            ),
            BarrierArchitectureTest.sample("A-2", "json", sql),
            BarrierArchitectureTest.sample("A-3", "web", jackson),
            BarrierArchitectureTest.sample("A-3", "web", "tools.jackson.databind.node.ObjectNode"),
            BarrierArchitectureTest.sample("A-4", "web", BarrierArchitectureTest.JDBC),
            BarrierArchitectureTest.sample("A-4", "web", BarrierArchitectureTest.REPO),
            BarrierArchitectureTest.sample("A-5", "json", BarrierArchitectureTest.REPO),
            BarrierArchitectureTest.sample("A-5", "photo", BarrierArchitectureTest.REPO),
            BarrierArchitectureTest.sample("A-6", "web", BarrierArchitectureTest.OUTLINES),
            BarrierArchitectureTest.sample("A-6", "photo", BarrierArchitectureTest.OUTLINES),
            BarrierArchitectureTest.sample("A-7", "submission", BarrierArchitectureTest.HTTP),
            BarrierArchitectureTest.sample(
                "A-7", "web", "org.springframework.web.server.ServerWebInputException"
            ),
            BarrierArchitectureTest.sample("A-8", "json", BarrierArchitectureTest.JDBC),
            Arguments.of(
                "A-9", "errors",
                String.format(
                    subtype, "shared.error.ForbiddenChange",
                    "org.springframework.http.HttpStatus", "\"x\""
                )
            ),
            Arguments.of(
                "A-9", "errors",
                String.format(
                    subtype, "shared.error.ConflictingState", jackson, "\"x\", \"y\", null"
                )
            )
        );
    }

    private static Arguments sample(final String name, final String pack, final String type) {
        return Arguments.of(name, pack, String.format("{ %s field; }", type));
    }

    private static ArchRule rule(final String name, final String root) {
        return Objects.requireNonNull(BarrierArchitectureTest.all(root).get(name))
            .allowEmptyShould(false);
    }

    private static Map<String, ArchRule> all(final String root) {
        final String pkg = String.format("%s..", root);
        final String out = String.format("%s.persistence..", root);
        final String service = String.format("%s.application..", root);
        final String controller = String.format(
            "%s\\.web\\.SubmissionsController(\\$.*)?", Pattern.quote(BarrierArchitectureTest.MODULE)
        );
        final String adapter = "..barrier.persistence..";
        return Map.of(
            "A-1", ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage(
                    String.format("%s.submission", root),
                    String.format("%s.submission.content", root)
                )
                .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "tools.jackson..", "java.sql..",
                    "io.github.dostepnykampus.building.."
                ),
            "A-2", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(pkg).and().resideOutsideOfPackage(out)
                .should().dependOnClassesThat().resideInAnyPackage(
                    "java.sql..", "org.springframework.jdbc.."
                ),
            "A-3", ArchRuleDefinition.noClasses().that().resideInAPackage(pkg)
                .should().dependOnClassesThat().areAssignableTo("tools.jackson.databind.JsonNode"),
            "A-4", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(String.format("%s.web..", root))
                .should().dependOnClassesThat().resideInAPackage(adapter)
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName(BarrierArchitectureTest.REPO),
            "A-5", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(pkg).and().resideOutsideOfPackage(out)
                .and().resideOutsideOfPackage(service)
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(BarrierArchitectureTest.REPO),
            "A-6", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(pkg).and().resideOutsideOfPackage(service)
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(BarrierArchitectureTest.OUTLINES),
            "A-7", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(pkg).and().haveNameNotMatching(controller)
                .should().dependOnClassesThat()
                .areAssignableTo(BarrierArchitectureTest.HTTP),
            "A-8", ArchRuleDefinition.noClasses()
                .that().resideInAPackage(pkg).and().resideOutsideOfPackage(out)
                .should().dependOnClassesThat().resideInAPackage(adapter),
            "A-9", ArchitectureTest.errorTypes()
        );
    }

    private JavaClasses compiled(final String pack, final String body) throws IOException {
        final String full = String.format("%s.%s", BarrierArchitectureTest.SAMPLES, pack);
        final ArchSamples samples = new ArchSamples(this.dir);
        samples.write(
            full, "Sample", String.format("package %s;%nfinal class Sample %s%n", full, body)
        );
        return samples.compiled(full);
    }
}
