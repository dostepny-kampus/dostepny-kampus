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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Ogólne reguły kodu serwera z gotowego zestawu ArchUnit.
 *
 * <p>Kod produkcyjny nie zależy od {@code java.util.logging}, nie pisze do
 * strumieni standardowych i nie rzuca ogólnych wyjątków. Reguły pochodzą z
 * gotowego zestawu ArchUnit, który sam zgłasza błąd na pustym imporcie.</p>
 *
 * <p>Gotowa reguła ArchUnit o {@code java.util.logging} łapie tylko pole
 * typu loggera, więc reguła loggingu dokłada zakaz każdej zależności od
 * tego pakietu, także wywołania bez pola.</p>
 *
 * @since 0.1.0
 */
final class CodingRulesArchitectureTest {

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

    private static ArchRule logging() {
        return CompositeArchRule
            .of(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING).and(
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
}
