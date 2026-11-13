/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;
import org.springframework.modulith.docs.Documenter;

/**
 * Testy zadania INFRA: moduły serwera według Spring Modulith.
 *
 * <p>Moduły z propozycji projektu to podpakiety pakietu bazowego.
 * Weryfikacja odrzuca cykle, dostęp do wnętrza cudzego modułu i zależność
 * od modułu spoza listy {@code allowedDependencies}. Każdy moduł zapisuje
 * tę listę w {@code @ApplicationModule}, także pustą. Spring Modulith czyta
 * {@code package-info} przez class loader, więc w testach widzi wersję z
 * katalogu testów; ta wersja powtarza listę z kodu produkcyjnego.</p>
 *
 * @since 0.1.0
 */
final class ModularityTest {

    /**
     * Pakiet przykładowych modułów w katalogu testów.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.modules";

    @Test
    void infraDetectsSixServerModules() {
        MatcherAssert.assertThat(
            "Spring Modulith nie wykrył dokładnie sześciu modułów serwera",
            ApplicationModules.of(DostepnyKampusApplication.class).stream()
                .map(ApplicationModule::getIdentifier)
                .map(Object::toString)
                .toList(),
            Matchers.containsInAnyOrder(
                "identity", "building", "barrier", "review", "sync", "export"
            )
        );
    }

    @Test
    void infraVerifiesModuleBoundaries() {
        Assertions.assertDoesNotThrow(
            (Executable) ApplicationModules.of(DostepnyKampusApplication.class)
                ::verify,
            "Moduły serwera mają cykl albo sięgają do wnętrza cudzego modułu"
        );
    }

    @Test
    void modulesDeclareAllowedDependencies() {
        Assertions.assertEquals(
            List.of(),
            ModularityTest.undeclared(
                ModularityTest.production(),
                ModularityTest.packages(ApplicationModules.of(DostepnyKampusApplication.class))
            ),
            "Moduł serwera nie ma @ApplicationModule z jawną listą allowedDependencies"
        );
    }

    @Test
    void rejectsModuleWithoutDeclaredDependencies() {
        Assertions.assertEquals(
            List.of(String.format("%s.beta", ModularityTest.SAMPLES)),
            ModularityTest.undeclared(
                new ClassFileImporter().importPackages(ModularityTest.SAMPLES),
                ModularityTest.packages(ModularityTest.samples())
            ),
            "Reguła przepuściła moduł bez @ApplicationModule"
        );
    }

    @Test
    void rejectsDependencyOutsideDeclaredList() {
        Assertions.assertTrue(
            ModularityTest.samples().detectViolations().hasViolations(),
            "Weryfikacja przepuściła zależność od modułu spoza allowedDependencies"
        );
    }

    @Test
    void packageInfoOfTestsRepeatsDeclaredDependencies() {
        final JavaClasses main = ModularityTest.production();
        Assertions.assertEquals(
            List.of(),
            ApplicationModules.of(DostepnyKampusApplication.class).stream()
                .map(ApplicationModule::getBasePackage)
                .filter(pack -> ModularityTest.shadowed(main, pack))
                .map(JavaPackage::getName)
                .toList(),
            "package-info testów zasłania listę modułu; powtórz w nim to samo @ApplicationModule"
        );
    }

    @Test
    void infraWritesModuleDocumentation() {
        final String folder = "target/spring-modulith-docs";
        new Documenter(
            ApplicationModules.of(DostepnyKampusApplication.class),
            Documenter.Options.defaults().withOutputFolder(folder)
        ).writeDocumentation();
        MatcherAssert.assertThat(
            "Documenter nie zapisał diagramu modułów",
            Files.exists(Path.of(folder, "components.puml")),
            Matchers.is(true)
        );
    }

    private static List<String> undeclared(
        final JavaClasses classes, final List<String> packages
    ) {
        return packages.stream()
            .filter(name -> ModularityTest.declared(classes, name).isEmpty())
            .sorted()
            .toList();
    }

    private static Optional<List<String>> declared(final JavaClasses classes, final String name) {
        final String info = String.format("%s.package-info", name);
        Optional<List<String>> result = Optional.empty();
        if (classes.contain(info)) {
            result = classes.get(info)
                .tryGetAnnotationOfType(org.springframework.modulith.ApplicationModule.class)
                .map(annotation -> List.of(annotation.allowedDependencies()));
        }
        return result;
    }

    private static boolean shadowed(final JavaClasses main, final JavaPackage pack) {
        return !ModularityTest.declared(main, pack.getName()).equals(
            pack.getAnnotation(org.springframework.modulith.ApplicationModule.class)
                .map(annotation -> List.of(annotation.allowedDependencies()))
        );
    }

    private static List<String> packages(final ApplicationModules modules) {
        return modules.stream()
            .map(ApplicationModule::getBasePackage)
            .map(JavaPackage::getName)
            .toList();
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private static ApplicationModules samples() {
        return ApplicationModules.of(ModularityTest.SAMPLES, location -> true);
    }
}
