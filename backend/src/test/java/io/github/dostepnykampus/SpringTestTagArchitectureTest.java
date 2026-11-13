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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Strażnik: każdy test z kontekstem Springa i każdy zestaw scenariuszy ma znacznik JUnit
 * {@code kontekst-springa}, który wyłącza go z analizy PIT.
 *
 * @since 0.1.0
 */
final class SpringTestTagArchitectureTest {

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
    void springAndSuiteTestsCarryTag() {
        ArchRuleDefinition.classes()
            .that().areMetaAnnotatedWith(SpringBootTest.class)
            .or().areAnnotatedWith(Suite.class)
            .should().beMetaAnnotatedWith(SpringTestTagArchitectureTest.tag())
            .check(SpringTestTagArchitectureTest.TESTS);
    }

    private static DescribedPredicate<JavaAnnotation<?>> tag() {
        return DescribedPredicate.describe(
            String.format("@Tag(\"%s\")", SpringTestTagArchitectureTest.TAG),
            annotation -> annotation.getRawType().isEquivalentTo(Tag.class)
                && SpringTestTagArchitectureTest.TAG.equals(annotation.get("value").orElse(null))
        );
    }
}
