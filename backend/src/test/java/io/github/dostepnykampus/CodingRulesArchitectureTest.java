/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.logging.Logger;
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
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class CodingRulesArchitectureTest {

    @ArchTest
    void codingForbidsJavaUtilLogging(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.logging()
                .check(classes),
            "Kod produkcyjny używa java.util.logging"
        );
    }

    @ArchTest
    void codingForbidsStandardStreams(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.streams()
                .check(classes),
            "Kod produkcyjny pisze do System.out, System.err albo woła printStackTrace"
        );
    }

    @ArchTest
    void codingForbidsGenericExceptions(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> CodingRulesArchitectureTest.exceptions()
                .check(classes),
            "Kod produkcyjny rzuca Throwable, Exception albo RuntimeException"
        );
    }

    @Test
    void codingLoggingRuleCatchesCallWithoutField() {
        Assertions.assertTrue(
            CodingRulesArchitectureTest.logging()
                .evaluate(new ClassFileImporter().importClasses(JulCallSample.class))
                .hasViolation(),
            "Reguła loggingu nie wykryła wywołania java.util.logging bez pola"
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

    /**
     * Przykład negatywny: wywołanie {@code java.util.logging} bez pola loggera.
     *
     * @since 0.1.0
     */
    static final class JulCallSample {

        /**
         * Pobiera logger tylko na czas wywołania.
         *
         * @return Nazwa loggera
         */
        String name() {
            return Logger.getLogger("przyklad").getName();
        }
    }
}
