/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
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
 * sąsiednich modułów pilnuje {@link TestSetupArchitectureIT}, a ich składowa {@code answers}
 * ma typ z pakietu Mockito.
 *
 * @since 0.1.0
 */
final class MockFreeArchitectureIT {

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.mocks";

    @Test
    void forbidsMockitoApi() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                    .importPackages("io.github.dostepnykampus"),
                MockFreeArchitectureIT.rule()
            ),
            "Testy używają API Mockito"
        );
    }

    @Test
    void rejectsMockCreatedByMockito(@TempDir final Path dir) throws IOException {
        Assertions.assertTrue(
            MockFreeArchitectureIT.rule().evaluate(
                MockFreeArchitectureIT.compiled(
                    dir, "Object field = org.mockito.Mockito.mock(Object.class);"
                )
            ).hasViolation(),
            "Reguła przepuściła test tworzący mock przez Mockito"
        );
    }

    @Test
    void acceptsMockitoBeanAnnotation(@TempDir final Path dir) throws IOException {
        Assertions.assertFalse(
            MockFreeArchitectureIT.rule().evaluate(
                MockFreeArchitectureIT.compiled(
                    dir,
                    String.join(
                        ".", "@org.springframework.test.context.bean.override.mockito",
                        "MockitoBean Object field;"
                    )
                )
            ).hasViolation(),
            "Reguła odrzuciła adnotację @MockitoBean pilnowaną przez TestSetupArchitectureIT"
        );
    }

    private static ArchRule rule() {
        return ArchRuleDefinition.noClasses()
            .should().accessClassesThat().resideInAPackage("org.mockito..")
            .allowEmptyShould(false);
    }

    private static JavaClasses compiled(final Path dir, final String member) throws IOException {
        final ArchSamples samples = new ArchSamples(dir);
        samples.write(
            MockFreeArchitectureIT.SAMPLES, "Sample",
            String.format(
                "package %s;%nfinal class Sample { %s }%n", MockFreeArchitectureIT.SAMPLES, member
            )
        );
        return samples.compiled(MockFreeArchitectureIT.SAMPLES);
    }
}
