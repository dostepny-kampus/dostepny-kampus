/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Strażnik niezmienności dziedziny i API serwera: każde pole klasy z warstwy {@code domain}
 * albo {@code api} (w obu układach modułu) jest {@code final}. Pola adapterów i usług nie
 * podlegają regule.
 *
 * @since 0.1.0
 */
final class ImmutableDomainArchitectureTest {

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.immutable";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void domainAndApiFieldsAreFinal() {
        ImmutableDomainArchitectureTest.rule(ModuleLayers.modules()).check(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackagesOf(DostepnyKampusApplication.class)
        );
    }

    @Test
    void rejectsMutableDomainAndApiFieldsOnly() throws IOException {
        final ArchSamples samples = new ArchSamples(this.dir);
        for (final String[] sample : new String[][] {
            {"submission", "Model", "int count;"},
            {"api", "Event", "String name;"},
            {"submission.content", "Place", "private final double lat = 0;"},
            {"submission.web", "Controller", "int calls;"},
        }) {
            ImmutableDomainArchitectureTest.type(samples, sample[0], sample[1], sample[2]);
        }
        Assertions.assertEquals(
            2,
            ImmutableDomainArchitectureTest.rule(List.of(ImmutableDomainArchitectureTest.SAMPLES))
                .evaluate(samples.compiled(ImmutableDomainArchitectureTest.SAMPLES))
                .getFailureReport().getDetails().size(),
            "Reguła przepuściła zmienne pole dziedziny albo API albo odrzuciła pole adaptera"
        );
    }

    private static ArchRule rule(final Collection<String> roots) {
        return ArchRuleDefinition.fields()
            .that().areDeclaredInClassesThat(
                DescribedPredicate.or(
                    ModuleLayers.in(roots, "domain"), ModuleLayers.in(roots, "api")
                )
            )
            .should().beFinal()
            .because("obiekty dziedziny i API są niezmienne");
    }

    private static void type(
        final ArchSamples samples, final String pack, final String name, final String field
    ) throws IOException {
        final String full = String.format("%s.%s", ImmutableDomainArchitectureTest.SAMPLES, pack);
        samples.write(
            full, name, String.format("package %s;%nfinal class %s { %s }%n", full, name, field)
        );
    }
}
