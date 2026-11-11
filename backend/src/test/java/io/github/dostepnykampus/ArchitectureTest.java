/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
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
 * {@code ResponseStatusException}, więc każdy błąd ma opis w {@code detail}
 * (#851).</p>
 *
 * @since 0.1.0
 */
final class ArchitectureTest {

    @Test
    void controllersStayOutsidePersistence() {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.controllerBoundary().check(ArchitectureTest.classes()),
            "Kontroler sięga bezpośrednio do warstwy dostępu do danych"
        );
    }

    @Test
    void jdbcStaysInPersistencePackages() {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.jdbcPlacement().check(ArchitectureTest.classes()),
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

    @Test
    void infraForbidsFieldInjection() {
        Assertions.assertDoesNotThrow(
            () -> GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
                .allowEmptyShould(true)
                .check(ArchitectureTest.classes()),
            "Klasa wstrzykuje zależność przez pole zamiast konstruktora"
        );
    }

    @Test
    void errorFiveKeepsErrorTypesFreeOfWebAndJson() {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.errorTypes().check(ArchitectureTest.classes()),
            "Typ błędu zależy od Spring Web albo Jacksona"
        );
    }

    @Test
    void errorsGoThroughSharedTypesInEveryModule() {
        Assertions.assertDoesNotThrow(
            () -> ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat().areAssignableTo(ResponseStatusException.class)
                .because("błędy idą wspólnymi typami z opisem w detail (#851)")
                .check(ArchitectureTest.classes()),
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

    private static JavaClasses classes() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }
}
