/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Strażnik: korzeń pakietu modułu serwera zawiera tylko API modułu.
 *
 * <p>Klasa najwyższego poziomu leżąca bezpośrednio w pakiecie bazowym modułu
 * musi być używana przez kod spoza tego modułu. Moduły otwarte nie podlegają
 * regule. Klasy, które dziś łamią regułę, są na liście wyjątków z numerem
 * issue; lista może tylko maleć.</p>
 *
 * @since 0.1.0
 */
final class ModuleRootArchitectureTest {

    /**
     * Klasy w korzeniu modułu, których nie woła inny moduł, i issue, które je przenosi.
     */
    private static final Map<String, Integer> INTERNAL = Map.of();

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.root";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void moduleRootsHoldOnlyApiOutsideExceptionList() {
        final Set<String> found = ModuleRootArchitectureTest.production();
        found.removeAll(ModuleRootArchitectureTest.INTERNAL.keySet());
        Assertions.assertEquals(
            Set.of(), found,
            "Klasa w korzeniu modułu nie jest używana przez inny moduł; przenieś ją do podpakietu"
        );
    }

    @Test
    void exceptionListHoldsOnlyRootInternals() {
        final List<String> stale = new ArrayList<>(ModuleRootArchitectureTest.INTERNAL.keySet());
        stale.removeAll(ModuleRootArchitectureTest.production());
        Assertions.assertEquals(
            List.of(), stale,
            "Klasa z listy wyjątków jest już API albo nie leży w korzeniu; usuń wpis"
        );
    }

    @Test
    void rejectsRootClassUsedOnlyInsideModule() throws IOException {
        final String alpha = String.format("%s.alpha", ModuleRootArchitectureTest.SAMPLES);
        final String beta = String.format("%s.beta", ModuleRootArchitectureTest.SAMPLES);
        this.source(alpha, "Api", "public final class Api { }");
        this.source(alpha, "Hidden", "public final class Hidden { }");
        this.source(
            String.format("%s.inner", alpha), "Worker",
            String.format("final class Worker { %s.Hidden hidden; }", alpha)
        );
        this.source(beta, "Client", String.format("final class Client { %s.Api api; }", alpha));
        Assertions.assertEquals(
            Set.of(String.format("%s.Hidden", alpha)),
            ModuleRootArchitectureTest.internal(this.compiled(), List.of(alpha)),
            "Reguła przepuściła klasę korzenia używaną tylko wewnątrz modułu"
        );
    }

    private static Set<String> production() {
        return ModuleRootArchitectureTest.internal(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackagesOf(DostepnyKampusApplication.class),
            ApplicationModules.of(DostepnyKampusApplication.class).stream()
                .filter(module -> !module.isOpen())
                .map(ApplicationModule::getBasePackage)
                .map(JavaPackage::getName)
                .toList()
        );
    }

    private static Set<String> internal(
        final JavaClasses classes, final Collection<String> roots
    ) {
        return classes.stream()
            .filter(JavaClass::isTopLevelClass)
            .filter(type -> !"package-info".equals(type.getSimpleName()))
            .filter(type -> roots.contains(type.getPackageName()))
            .filter(type -> !ModuleRootArchitectureTest.usedOutside(type))
            .map(JavaClass::getName)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean usedOutside(final JavaClass type) {
        final String root = String.format("%s.", type.getPackageName());
        return type.getDirectDependenciesToSelf().stream()
            .map(Dependency::getOriginClass)
            .anyMatch(origin -> !String.format("%s.", origin.getPackageName()).startsWith(root));
    }

    private JavaClasses compiled() throws IOException {
        final List<String> args = new ArrayList<>(List.of("-d", this.dir.toString()));
        try (Stream<Path> files = Files.walk(this.dir)) {
            files.filter(file -> file.toString().endsWith(".java"))
                .map(Path::toString)
                .forEach(args::add);
        }
        Assertions.assertEquals(
            0,
            ToolProvider.getSystemJavaCompiler().run(
                null, null, null, args.toArray(String[]::new)
            ),
            "Przykład się nie skompilował"
        );
        return new ClassFileImporter().importPath(
            this.dir.resolve(ModuleRootArchitectureTest.SAMPLES.replace('.', '/'))
        );
    }

    private void source(final String pack, final String name, final String body)
        throws IOException {
        final Path folder = this.dir.resolve("src").resolve(pack.replace('.', '/'));
        Files.createDirectories(folder);
        Files.writeString(
            folder.resolve(String.format("%s.java", name)),
            String.format("package %s;%n%s%n", pack, body)
        );
    }
}
