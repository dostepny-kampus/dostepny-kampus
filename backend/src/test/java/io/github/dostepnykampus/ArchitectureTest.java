/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testy zadania INFRA: reguły architektury, których Spring Modulith
 * nie sprawdza.
 *
 * <p>Źródło: propozycja projektu, zakazy zależności serwera. „Kontroler,
 * usługa, repozytorium; kontroler nie widzi repozytorium”, „encja nie
 * wycieka poza moduł” i „pole {@code @Autowired} zabronione” (gotowa
 * reguła ArchUnit). Reguły dopuszczają brak klas, bo moduły są puste.</p>
 *
 * @since 0.1.0
 */
final class ArchitectureTest {

    @Test
    void infraKeepsControllerServiceRepositoryChain() {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.layers().check(ArchitectureTest.classes()),
            "Kontroler sięga do repozytorium albo warstwy idą pod prąd"
        );
    }

    @Test
    void infraKeepsEntitiesInsideTheirModule() {
        Assertions.assertDoesNotThrow(
            () -> ArchitectureTest.entities().check(ArchitectureTest.classes()),
            "Encja JPA jest używana poza swoim modułem"
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

    private static ArchRule layers() {
        return Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Kontrolery")
            .definedBy(ArchitectureTest.stereotype("Controller"))
            .layer("Usługi")
            .definedBy(ArchitectureTest.stereotype("Service"))
            .layer("Repozytoria").definedBy(
                JavaClass.Predicates.assignableTo(
                    "org.springframework.data.repository.Repository"
                ).or(ArchitectureTest.stereotype("Repository"))
            )
            .whereLayer("Kontrolery").mayNotBeAccessedByAnyLayer()
            .whereLayer("Usługi").mayOnlyBeAccessedByLayers("Kontrolery")
            .whereLayer("Repozytoria").mayOnlyBeAccessedByLayers("Usługi");
    }

    private static ArchRule entities() {
        return SlicesRuleDefinition.slices()
            .matching("io.github.dostepnykampus.(*)..")
            .should().notDependOnEachOther().ignoreDependency(
                DescribedPredicate.alwaysTrue(),
                DescribedPredicate.not(
                    CanBeAnnotated.Predicates.annotatedWith(
                        "jakarta.persistence.Entity"
                    )
                )
            )
            .allowEmptyShould(true);
    }

    private static DescribedPredicate<CanBeAnnotated> stereotype(
        final String name
    ) {
        return CanBeAnnotated.Predicates.metaAnnotatedWith(
            String.format("org.springframework.stereotype.%s", name)
        );
    }

    private static JavaClasses classes() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }
}
