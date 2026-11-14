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
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Strażnik korzenia i API modułów serwera. Korzeń zamkniętego modułu zawiera tylko
 * {@code package-info.java}; API modułu leży w pakiecie {@code api}, a jego typy zależą tylko od
 * typów {@code api}, modułu {@code shared}, JDK i adnotacji JSpecify. Moduł, który zależy od
 * modułu z pakietem {@code api}, zapisuje tę zależność jako {@code moduł :: api}.
 *
 * @since 0.1.0
 */
final class ModuleRootArchitectureTest {

    /**
     * Pakiet bazowy aplikacji.
     */
    private static final String BASE = "io.github.dostepnykampus";

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.root";

    /**
     * Nazwa klasy opisu pakietu.
     */
    private static final String INFO = "package-info";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void moduleRootsHoldOnlyPackageInfo() {
        Assertions.assertEquals(
            Set.of(),
            ModuleRootArchitectureTest.rooted(
                ModuleRootArchitectureTest.production(), ModuleRootArchitectureTest.closed()
            ),
            "Klasa leży w korzeniu modułu; przenieś ją do api, tematu albo warstwy"
        );
    }

    @Test
    void apiTypesDependOnlyOnApiSharedAndJdk() {
        ModuleRootArchitectureTest.api(ModuleRootArchitectureTest.BASE)
            .check(ModuleRootArchitectureTest.production());
    }

    @Test
    void dependenciesOnModulesWithApiNameTheApi() {
        Assertions.assertEquals(
            List.of(),
            ModuleRootArchitectureTest.unnamed(
                ModuleRootArchitectureTest.closed().stream().collect(
                    Collectors.toMap(
                        ModuleRootArchitectureTest::name, ModuleRootArchitectureTest::declared
                    )
                ),
                ModuleRootArchitectureTest.withApi(ModuleRootArchitectureTest.production())
            ),
            "Zależność od modułu z pakietem api ma postać \"moduł :: api\""
        );
    }

    @Test
    void rejectsClassInModuleRoot() throws IOException {
        final String alpha = ModuleRootArchitectureTest.sample("alpha");
        this.source(alpha, "Hidden", "public final class Hidden { }");
        this.source(String.format("%s.api", alpha), "Event", "public record Event(String id) { }");
        Assertions.assertEquals(
            Set.of(String.format("%s.Hidden", alpha)),
            ModuleRootArchitectureTest.rooted(this.compiled(), List.of(alpha)),
            "Reguła przepuściła klasę w korzeniu modułu"
        );
    }

    @Test
    void rejectsApiTypeUsingModuleInterior() throws IOException {
        final String alpha = ModuleRootArchitectureTest.sample("alpha");
        final String beta = ModuleRootArchitectureTest.sample("beta");
        this.source(String.format("%s.domain", alpha), "Model", "public final class Model { }");
        this.source(String.format("%s.api", beta), "Area", "public record Area(String id) { }");
        this.source(
            String.format("%s.api", alpha), "Good",
            String.format("public record Good(java.util.List<%s.api.Area> areas) { }", beta)
        );
        this.source(
            String.format("%s.api", alpha), "Bad",
            String.format("public record Bad(%s.domain.Model model) { }", alpha)
        );
        final List<String> details = ModuleRootArchitectureTest.api(
            ModuleRootArchitectureTest.SAMPLES
        ).evaluate(this.compiled()).getFailureReport().getDetails();
        Assertions.assertTrue(
            !details.isEmpty() && details.stream().allMatch(line -> line.contains("api.Bad")),
            "Reguła przepuściła typ api zależny od wnętrza modułu albo odrzuciła typ api sąsiada"
        );
    }

    @Test
    void rejectsDependencyWithoutApiName() {
        Assertions.assertEquals(
            List.of("barrier -> building"),
            ModuleRootArchitectureTest.unnamed(
                Map.of(
                    "barrier", List.of("building", "identity :: api", "shared"),
                    "identity", List.of()
                ),
                Set.of("building", "identity")
            ),
            "Reguła przepuściła zależność od modułu z api bez nazwy interfejsu"
        );
    }

    private static Set<String> rooted(final JavaClasses classes, final Collection<String> roots) {
        return classes.stream()
            .filter(type -> !ModuleRootArchitectureTest.INFO.equals(type.getSimpleName()))
            .filter(type -> roots.contains(type.getPackageName()))
            .map(JavaClass::getName)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ArchRule api(final String base) {
        return ArchRuleDefinition.classes()
            .that().resideInAPackage(String.format("%s.*.api..", base))
            .and().doNotHaveSimpleName(ModuleRootArchitectureTest.INFO)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                String.format("%s.*.api..", base),
                String.format("%s.shared..", ModuleRootArchitectureTest.BASE),
                "java..", "org.jspecify.."
            )
            .because("API modułu nie wydaje typów jego wnętrza ani frameworków");
    }

    private static List<String> unnamed(
        final Map<String, List<String>> declared, final Set<String> apis
    ) {
        return declared.entrySet().stream().flatMap(
            entry -> entry.getValue().stream()
                .filter(apis::contains)
                .map(target -> String.format("%s -> %s", entry.getKey(), target))
        ).sorted().toList();
    }

    private static List<String> declared(final String root) {
        try {
            return Arrays.asList(
                Class.forName(String.format("%s.%s", root, ModuleRootArchitectureTest.INFO))
                    .getAnnotation(org.springframework.modulith.ApplicationModule.class)
                    .allowedDependencies()
            );
        } catch (final ClassNotFoundException ex) {
            throw new IllegalStateException(String.format("Brak opisu modułu %s", root), ex);
        }
    }

    private static Set<String> withApi(final JavaClasses classes) {
        return ModuleRootArchitectureTest.closed().stream().filter(
            root -> classes.stream().anyMatch(
                type -> type.getPackageName().startsWith(String.format("%s.api", root))
            )
        ).map(ModuleRootArchitectureTest::name).collect(Collectors.toSet());
    }

    private static String name(final String root) {
        return root.substring(ModuleRootArchitectureTest.BASE.length() + 1);
    }

    private static List<String> closed() {
        return ApplicationModules.of(DostepnyKampusApplication.class).stream()
            .filter(module -> !module.isOpen())
            .map(ApplicationModule::getBasePackage)
            .map(JavaPackage::getName)
            .toList();
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(DostepnyKampusApplication.class);
    }

    private static String sample(final String name) {
        return String.format("%s.%s", ModuleRootArchitectureTest.SAMPLES, name);
    }

    private JavaClasses compiled() throws IOException {
        return new ArchSamples(this.dir).compiled(ModuleRootArchitectureTest.SAMPLES);
    }

    private void source(final String pack, final String name, final String body)
        throws IOException {
        new ArchSamples(this.dir).write(pack, name, String.format("package %s;%n%s%n", pack, body));
    }
}
