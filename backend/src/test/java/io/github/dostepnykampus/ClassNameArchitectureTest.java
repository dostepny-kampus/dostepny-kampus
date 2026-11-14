/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

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
 * Strażnik nazw klas serwera: nazwa klasy nie kończy się na {@code Util}, {@code Utils},
 * {@code Helper}, {@code Helpers} ani {@code Manager}. Taka klasa zbiera niepowiązane funkcje;
 * kod trafia do klasy nazwanej po swojej odpowiedzialności. Role frameworka, np.
 * {@code Controller} czy {@code Repository}, są dozwolone.
 *
 * @since 0.1.0
 */
final class ClassNameArchitectureTest {

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.name";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void productionClassesAvoidCatchAllNames() {
        ClassNameArchitectureTest.names().check(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackagesOf(DostepnyKampusApplication.class)
        );
    }

    @Test
    void rejectsCatchAllNamesAndAcceptsRoles() throws IOException {
        final ArchSamples samples = new ArchSamples(this.dir);
        for (final String name : new String[] {"DateUtils", "JsonHelper", "PhotoManager"}) {
            samples.write(
                ClassNameArchitectureTest.SAMPLES, name,
                String.format(
                    "package %s;%nfinal class %s { }%n", ClassNameArchitectureTest.SAMPLES, name
                )
            );
        }
        samples.write(
            ClassNameArchitectureTest.SAMPLES, "PhotoController",
            String.format(
                "package %s;%nfinal class PhotoController { }%n", ClassNameArchitectureTest.SAMPLES
            )
        );
        Assertions.assertEquals(
            3,
            ClassNameArchitectureTest.names()
                .evaluate(samples.compiled(ClassNameArchitectureTest.SAMPLES))
                .getFailureReport().getDetails().size(),
            "Reguła przepuściła nazwę Utils, Helper albo Manager albo odrzuciła rolę frameworka"
        );
    }

    private static ArchRule names() {
        return ArchRuleDefinition.noClasses()
            .should().haveNameMatching(".*(Util|Utils|Helper|Helpers|Manager)(\\$.*)?")
            .because("klasa nazwana po odpowiedzialności zamiast zbiorczej klasy pomocniczej");
    }
}
