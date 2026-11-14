/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testy serwera nie wywołują API Mockito: dziedzinę i moduły testuje się przez fejki. Reguła
 * patrzy na wywołania i dostęp, nie na adnotacje: {@code @MockitoBean} i {@code @MockitoSpyBean}
 * sąsiednich modułów pilnuje {@link TestSetupArchitectureTest}, a ich składowa {@code answers}
 * ma typ z pakietu Mockito.
 *
 * @since 0.1.0
 */
final class MockFreeArchitectureTest {

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.mocks";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void testsSkipMockitoApi() {
        MockFreeArchitectureTest.rule().check(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("io.github.dostepnykampus")
        );
    }

    @Test
    void rejectsMockCreatedInTest() throws IOException {
        Assertions.assertTrue(
            MockFreeArchitectureTest.rule().evaluate(
                this.compiled("Object field = org.mockito.Mockito.mock(Object.class);")
            ).hasViolation(),
            "Reguła przepuściła test tworzący mock przez Mockito"
        );
    }

    @Test
    void acceptsMockitoBeanAnnotation() throws IOException {
        Assertions.assertFalse(
            MockFreeArchitectureTest.rule().evaluate(
                this.compiled(
                    String.join(
                        ".", "@org.springframework.test.context.bean.override.mockito",
                        "MockitoBean Object field;"
                    )
                )
            ).hasViolation(),
            "Reguła odrzuciła adnotację @MockitoBean pilnowaną przez TestSetupArchitectureTest"
        );
    }

    private static ArchRule rule() {
        return ArchRuleDefinition.noClasses()
            .should().accessClassesThat().resideInAPackage("org.mockito..")
            .allowEmptyShould(false);
    }

    private JavaClasses compiled(final String member) throws IOException {
        final ArchSamples samples = new ArchSamples(this.dir);
        samples.write(
            MockFreeArchitectureTest.SAMPLES, "Sample",
            String.format(
                "package %s;%nfinal class Sample { %s }%n", MockFreeArchitectureTest.SAMPLES, member
            )
        );
        return samples.compiled(MockFreeArchitectureTest.SAMPLES);
    }
}
