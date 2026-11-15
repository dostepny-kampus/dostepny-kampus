/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Strażnik: każdy test z kontekstem Springa i każdy zestaw scenariuszy ma znacznik JUnit
 * {@code kontekst-springa}, który wyłącza go z analizy PIT, a nazwa klasy testów
 * z kontekstem Springa kończy się na {@code IT}, więc uruchamia ją failsafe. Klasa konfiguracji
 * kontekstu scenariuszy Cucumbera nie jest klasą testów i nie podlega tej nazwie.
 *
 * @since 0.1.0
 */
final class SpringTestTagArchitectureIT {

    /**
     * Znacznik testów z kontekstem Springa.
     */
    private static final String TAG = "kontekst-springa";

    /**
     * Skompilowane klasy testów.
     */
    private static final JavaClasses TESTS = new ClassFileImporter()
        .withImportOption(location -> location.contains("/test-classes/"))
        .importPackagesOf(IntegrationTest.class);

    @Test
    void tagsSpringAndSuiteClasses() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                SpringTestTagArchitectureIT.TESTS,
                ArchRuleDefinition.classes()
                    .that().areMetaAnnotatedWith(SpringBootTest.class)
                    .or().areAnnotatedWith(Suite.class)
                    .should().beMetaAnnotatedWith(SpringTestTagArchitectureIT.tag())
            ),
            "Klasa z kontekstem Springa albo zestaw bez znacznika kontekst-springa"
        );
    }

    @Test
    void namesSpringClassesWithIt() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                SpringTestTagArchitectureIT.TESTS,
                ArchRuleDefinition.classes()
                    .that().areMetaAnnotatedWith(SpringBootTest.class)
                    .and().areNotAnnotations()
                    .and().areNotAnnotatedWith(CucumberContextConfiguration.class)
                    .should().haveSimpleNameEndingWith("IT")
            ),
            "Klasa testów z kontekstem Springa nie kończy się na IT"
        );
    }

    private static DescribedPredicate<JavaAnnotation<?>> tag() {
        return DescribedPredicate.describe(
            String.format("@Tag(\"%s\")", SpringTestTagArchitectureIT.TAG),
            annotation -> annotation.getRawType().isEquivalentTo(Tag.class)
                && SpringTestTagArchitectureIT.TAG.equals(annotation.get("value").orElse(null))
        );
    }
}
