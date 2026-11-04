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
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Ogólne reguły kodu serwera z gotowego zestawu ArchUnit.
 *
 * <p>Kod produkcyjny nie zależy od {@code java.util.logging}, nie pisze do
 * strumieni standardowych i nie rzuca ogólnych wyjątków. Każda reguła ma
 * przykład negatywny: klasę, która ją łamie. Przykłady kompiluje test do
 * katalogu tymczasowego, więc nie trafiają do importu kodu produkcyjnego
 * ani do kontroli stylu.</p>
 *
 * <p>Gotowa reguła ArchUnit o {@code java.util.logging} łapie tylko pole
 * typu loggera, więc reguła loggingu dokłada zakaz każdej zależności od
 * tego pakietu, także wywołania bez pola.</p>
 *
 * @since 0.1.0
 */
final class CodingRulesArchitectureTest {

    /**
     * Pakiet klas łamiących reguły, poniżej pakietu bazowego.
     */
    private static final String FIXTURES = "io.github.dostepnykampus.fixture.deep";

    @Test
    void codingForbidsJavaUtilLogging() {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.logging()
                .check(CodingRulesArchitectureTest.production()),
            "Kod produkcyjny używa java.util.logging"
        );
    }

    @Test
    void codingForbidsStandardStreams() {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.streams()
                .check(CodingRulesArchitectureTest.production()),
            "Kod produkcyjny pisze do System.out, System.err albo woła printStackTrace"
        );
    }

    @Test
    void codingForbidsGenericExceptions() {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.exceptions()
                .check(CodingRulesArchitectureTest.production()),
            "Kod produkcyjny rzuca Throwable, Exception albo RuntimeException"
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("violations")
    void codingRejectsViolatingClass(
        final ArchRule rule, final String name, final String body,
        @TempDir final Path dir
    ) throws IOException {
        MatcherAssert.assertThat(
            String.format("Reguła przepuściła klasę %s", name),
            rule.evaluate(
                CodingRulesArchitectureTest.fixture(dir, name, body)
            ).hasViolation(),
            Matchers.is(true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void codingFailsOnEmptyImport(final ArchRule rule) {
        Assertions.assertThrows(
            AssertionError.class,
            () -> rule.check(new ClassFileImporter().importClasses()),
            "Reguła przechodzi, choć nie sprawdziła żadnej klasy"
        );
    }

    @Test
    void codingImportsProductionSubpackages() {
        MatcherAssert.assertThat(
            "Import kodu produkcyjnego pomija podpakiety modułów",
            CodingRulesArchitectureTest.production().contain(
                "io.github.dostepnykampus.barrier.domain.SubmissionPoint"
            ),
            Matchers.is(true)
        );
    }

    @Test
    void codingImportsNoTestClasses() {
        MatcherAssert.assertThat(
            "Import kodu produkcyjnego obejmuje klasy testowe",
            CodingRulesArchitectureTest.production()
                .contain(CodingRulesArchitectureTest.class),
            Matchers.is(false)
        );
    }

    private static Stream<Arguments> violations() {
        return Stream.of(
            Arguments.of(
                CodingRulesArchitectureTest.logging(), "JulLogger",
                "final java.util.logging.Logger log = java.util.logging.Logger.getLogger(\"x\");"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.logging(), "JulCall",
                "void log() { java.util.logging.Logger.getLogger(\"x\").info(\"x\"); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.streams(), "StandardOut",
                "void print() { System.out.println(\"x\"); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.streams(), "StandardErr",
                "void print() { System.err.println(\"x\"); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.streams(), "StackTrace",
                "void print(final Exception error) { error.printStackTrace(); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.exceptions(), "RawThrowable",
                "void fail() throws Throwable { throw new Throwable(\"x\"); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.exceptions(), "RawException",
                "void fail() throws Exception { throw new Exception(\"x\"); }"
            ),
            Arguments.of(
                CodingRulesArchitectureTest.exceptions(), "RawRuntime",
                "void fail() { throw new RuntimeException(\"x\"); }"
            )
        );
    }

    private static Stream<ArchRule> rules() {
        return Stream.of(
            CodingRulesArchitectureTest.logging(),
            CodingRulesArchitectureTest.streams(),
            CodingRulesArchitectureTest.exceptions()
        );
    }

    private static ArchRule logging() {
        return CompositeArchRule
            .of(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING)
            .and(
                ArchRuleDefinition.noClasses().should().dependOnClassesThat()
                    .resideInAPackage("java.util.logging..")
            );
    }

    private static ArchRule streams() {
        return GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
    }

    private static ArchRule exceptions() {
        return GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private static JavaClasses fixture(
        final Path dir, final String name, final String body
    ) throws IOException {
        final Path source = dir.resolve(String.format("%s.java", name));
        Files.writeString(
            source,
            String.format(
                "package %s;%nfinal class %s {%n    %s%n}%n",
                CodingRulesArchitectureTest.FIXTURES, name, body
            )
        );
        final int code = ToolProvider.getSystemJavaCompiler().run(
            null, null, null, "-d", dir.toString(), source.toString()
        );
        if (code != 0) {
            throw new IllegalStateException(
                String.format("Klasa %s się nie skompilowała", name)
            );
        }
        return new ClassFileImporter().importPath(
            dir.resolve(CodingRulesArchitectureTest.FIXTURES.replace('.', '/'))
        );
    }
}
