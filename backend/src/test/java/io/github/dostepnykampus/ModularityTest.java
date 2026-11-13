/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;
import org.springframework.modulith.docs.Documenter;

/**
 * Moduły serwera według Spring Modulith: podpakiety pakietu bazowego, każdy z klasą produkcyjną
 * i opisem w Javadoc {@code package-info.java}. Bez cykli, bez dostępu do wnętrza cudzego modułu
 * i bez zależności spoza listy {@code allowedDependencies}, którą każdy moduł zapisuje
 * w {@code @ApplicationModule}, także pustą. Spring Modulith czyta {@code package-info}
 * z katalogu testów, więc ta wersja powtarza listę z kodu produkcyjnego.
 *
 * @since 0.1.0
 */
final class ModularityTest {

    /**
     * Pakiet przykładowych modułów w katalogu testów.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.modules";

    /**
     * Jedyne podpakiety otwartego modułu shared.
     */
    private static final Set<String> SHARED = Set.of("error", "json", "geo");

    /**
     * Katalog na przykładowe źródła, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void modulesHaveClassesAndDescription() {
        Assertions.assertEquals(
            List.of(),
            ModularityTest.incomplete(
                ModularityTest.production(), Path.of("src", "main", "java"),
                ModularityTest.packages(ApplicationModules.of(DostepnyKampusApplication.class))
            ),
            "Moduł serwera nie ma klasy produkcyjnej albo opisu w package-info.java"
        );
    }

    @Test
    void rejectsModuleWithoutClassesOrDescription() throws IOException {
        this.description("alpha", "/** Opis. */%n");
        this.description("beta", "");
        this.description("empty", "/** Opis. */%n");
        Assertions.assertEquals(
            List.of(
                String.format("%s.beta", ModularityTest.SAMPLES),
                String.format("%s.empty", ModularityTest.SAMPLES)
            ),
            ModularityTest.incomplete(
                new ClassFileImporter().importPackages(ModularityTest.SAMPLES), this.dir,
                List.of(
                    String.format("%s.alpha", ModularityTest.SAMPLES),
                    String.format("%s.beta", ModularityTest.SAMPLES),
                    String.format("%s.empty", ModularityTest.SAMPLES)
                )
            ),
            "Reguła przepuściła moduł bez klas albo bez opisu w package-info.java"
        );
    }

    @Test
    void sharedHasOnlyErrorJsonAndGeo() {
        Assertions.assertEquals(
            List.of(),
            ModularityTest.foreign(
                ModularityTest.production(), "io.github.dostepnykampus.shared"
            ),
            "Moduł shared ma klasę poza podpakietami error, json i geo"
        );
    }

    @Test
    void basePackageHoldsOnlyApplication() {
        Assertions.assertEquals(
            List.of(DostepnyKampusApplication.class.getName()),
            ModularityTest.production().stream()
                .filter(type -> "io.github.dostepnykampus".equals(type.getPackageName()))
                .filter(type -> !"package-info".equals(type.getSimpleName()))
                .map(JavaClass::getName)
                .toList(),
            "Pakiet główny ma klasę inną niż klasa aplikacji; przenieś ją do modułu albo do shared"
        );
    }

    @Test
    void rejectsSharedSubpackageOutsideList() {
        Assertions.assertEquals(
            List.of("alpha", "beta"),
            ModularityTest.foreign(
                new ClassFileImporter().importPackages(ModularityTest.SAMPLES),
                ModularityTest.SAMPLES
            ),
            "Reguła przepuściła podpakiet shared spoza error, json i geo"
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

    private static List<String> incomplete(
        final JavaClasses classes, final Path sources, final List<String> modules
    ) {
        return modules.stream()
            .filter(name -> ModularityTest.incomplete(classes, sources, name))
            .sorted()
            .toList();
    }

    private static boolean incomplete(
        final JavaClasses classes, final Path sources, final String name
    ) {
        final String prefix = String.format("%s.", name);
        return classes.stream()
            .filter(type -> !"package-info".equals(type.getSimpleName()))
            .noneMatch(type -> String.format("%s.", type.getPackageName()).startsWith(prefix))
            || !ModularityTest.described(sources, name);
    }

    private static boolean described(final Path sources, final String name) {
        final Path info = sources.resolve(name.replace('.', '/')).resolve("package-info.java");
        boolean result = false;
        if (Files.isRegularFile(info)) {
            try {
                result = Files.readString(info).contains("/**");
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return result;
    }

    private void description(final String module, final String javadoc) throws IOException {
        final Path folder = this.dir.resolve(
            String.format("%s.%s", ModularityTest.SAMPLES, module).replace('.', '/')
        );
        Files.createDirectories(folder);
        Files.writeString(
            folder.resolve("package-info.java"),
            String.format(
                String.format("%s%s", javadoc, "package %s.%s;%n"), ModularityTest.SAMPLES, module
            )
        );
    }

    private static List<String> foreign(final JavaClasses classes, final String shared) {
        final String prefix = String.format("%s.", shared);
        return classes.stream()
            .filter(type -> !"package-info".equals(type.getSimpleName()))
            .map(JavaClass::getPackageName)
            .filter(name -> String.format("%s.", name).startsWith(prefix))
            .map(name -> String.format("%s.", name).substring(prefix.length()).split("\\.", 2)[0])
            .filter(sub -> !ModularityTest.SHARED.contains(sub))
            .distinct()
            .sorted()
            .toList();
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
