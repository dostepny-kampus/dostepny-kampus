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
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testy zadania INFRA: reguły architektury, których Spring Modulith
 * nie sprawdza.
 *
 * <p>Kontrolery korzystają z usług i portów. Implementacje JDBC należą
 * do pakietów persistence lub postgis. ArchUnit zabrania też wstrzykiwania pól.</p>
 *
 * <p>E5: wspólne typy błędów nie zależą od Spring Web ani Jacksona, żeby
 * wyjątki domenowe nie wciągały warstwy HTTP. Żaden moduł nie rzuca
 * {@code ResponseStatusException}, więc każdy błąd ma opis w {@code detail}.</p>
 *
 * @since 0.1.0
 */
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class ArchitectureTest {

    @ArchTest
    void controllersStayOutsidePersistence(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.controllerBoundary().check(classes),
            "Kontroler sięga bezpośrednio do warstwy dostępu do danych"
        );
    }

    @ArchTest
    void jdbcStaysInPersistencePackages(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.jdbcPlacement().check(classes),
            "Spring JDBC jest używane poza warstwą dostępu do danych"
        );
    }

    @Test
    void controllerUsingJdbcIsRejected() {
        Assertions.assertTrue(
            ArchitectureTest.controllerBoundary()
                .evaluate(new ClassFileImporter().importClasses(JdbcControllerSample.class))
                .getFailureReport().getDetails().stream()
                .anyMatch(detail -> detail.contains("JdbcClient")),
            "Reguła kontrolerów nie wykryła zależności od JdbcClient"
        );
    }

    @Test
    void controllerBoundaryRejectsEmptySelection() {
        Assertions.assertThrows(
            AssertionError.class,
            () -> ArchitectureTest.controllerBoundary().check(
                new ClassFileImporter().importClasses(InvalidInput.class)
            ),
            "Reguła bez żadnego kontrolera powinna zawieść"
        );
    }

    @ArchTest
    void infraForbidsFieldInjection(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
                .allowEmptyShould(true)
                .check(classes),
            "Klasa wstrzykuje zależność przez pole zamiast konstruktora"
        );
    }

    @ArchTest
    void errorFiveKeepsErrorTypesFreeOfWebAndJson(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.errorTypes().check(classes),
            "Typ błędu zależy od Spring Web albo Jacksona"
        );
    }

    @ArchTest
    void errorsGoThroughSharedTypesInEveryModule(final JavaClasses classes) {
        Assertions.assertDoesNotThrow(
            () -> ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat().areAssignableTo(ResponseStatusException.class)
                .because("błędy idą wspólnymi typami z opisem w detail (#851)")
                .check(classes),
            "Kod produkcyjny rzuca ResponseStatusException zamiast wspólnego typu błędu"
        );
    }

    /**
     * Reguła E5: typy błędów bez Spring Web i Jacksona.
     *
     * @return Reguła, także dla przykładu negatywnego strażnika zgłoszeń
     */
    static ArchRule errorTypes() {
        return ArchRuleDefinition.noClasses()
            .that().areAssignableTo(InvalidInput.class)
            .or().areAssignableTo(MissingResource.class)
            .or().areAssignableTo(ConflictingState.class)
            .or().areAssignableTo(ForbiddenChange.class)
            .or().areAssignableTo(MissingLogin.class)
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework.web..", "org.springframework.http..",
                "tools.jackson..", "com.fasterxml.jackson.."
            )
            .allowEmptyShould(false);
    }

    private static ArchRule controllerBoundary() {
        return ArchRuleDefinition.noClasses().that().areMetaAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework.jdbc..", "java.sql..", "javax.sql..",
                "..persistence..", "..postgis.."
            ).allowEmptyShould(false);
    }

    private static ArchRule jdbcPlacement() {
        return ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages("..persistence..", "..postgis..")
            .and().doNotHaveFullyQualifiedName("io.github.dostepnykampus.building.BuildingQuery")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..")
            .because("BuildingQuery pozostaje wyjątkiem do uporządkowania zapytań w #849")
            .allowEmptyShould(false);
    }
}
