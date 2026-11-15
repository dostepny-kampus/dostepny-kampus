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
final class ModuleRootArchitectureIT {

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

    @Test
    void holdsOnlyPackageInfoInModuleRoots() {
        Assertions.assertEquals(
            Set.of(),
            ModuleRootArchitectureIT.rooted(
                ModuleRootArchitectureIT.production(), ModuleRootArchitectureIT.closed()
            ),
            "Klasa leży w korzeniu modułu; przenieś ją do api, tematu albo warstwy"
        );
    }

    @Test
    void limitsApiTypesToApiSharedAndJdk() {
        Assertions.assertEquals(
            "",
            ArchViolations.of(
                ModuleRootArchitectureIT.production(),
                ModuleRootArchitectureIT.api(ModuleRootArchitectureIT.BASE)
            ),
            "Typy API modułu zależą od czegoś poza API, shared i JDK"
        );
    }

    @Test
    void dependenciesOnModulesWithApiNameTheApi() {
        Assertions.assertEquals(
            List.of(),
            ModuleRootArchitectureIT.unnamed(
                ModuleRootArchitectureIT.closed().stream().collect(
                    Collectors.toMap(
                        ModuleRootArchitectureIT::name, ModuleRootArchitectureIT::declared
                    )
                ),
                ModuleRootArchitectureIT.withApi(ModuleRootArchitectureIT.production())
            ),
            "Zależność od modułu z pakietem api ma postać \"moduł :: api\""
        );
    }

    @Test
    void rejectsClassInModuleRoot(@TempDir final Path dir) throws IOException {
        final String alpha = ModuleRootArchitectureIT.sample("alpha");
        ModuleRootArchitectureIT.source(
            dir, alpha, "Hidden", "public final class Hidden { }"
        );
        ModuleRootArchitectureIT.source(
            dir, String.format("%s.api", alpha), "Event", "public record Event(String id) { }"
        );
        Assertions.assertEquals(
            Set.of(String.format("%s.Hidden", alpha)),
            ModuleRootArchitectureIT.rooted(
                ModuleRootArchitectureIT.compiled(dir), List.of(alpha)
            ),
            "Reguła przepuściła klasę w korzeniu modułu"
        );
    }

    @Test
    void rejectsApiTypeUsingModuleInterior(@TempDir final Path dir)
        throws IOException {
        final String alpha = ModuleRootArchitectureIT.sample("alpha");
        final String beta = ModuleRootArchitectureIT.sample("beta");
        ModuleRootArchitectureIT.source(
            dir, String.format("%s.domain", alpha), "Model", "public final class Model { }"
        );
        ModuleRootArchitectureIT.source(
            dir, String.format("%s.api", beta), "Area", "public record Area(String id) { }"
        );
        ModuleRootArchitectureIT.source(
            dir, String.format("%s.api", alpha), "Good",
            String.format("public record Good(java.util.List<%s.api.Area> areas) { }", beta)
        );
        ModuleRootArchitectureIT.source(
            dir, String.format("%s.api", alpha), "Bad",
            String.format("public record Bad(%s.domain.Model model) { }", alpha)
        );
        final List<String> details = ModuleRootArchitectureIT.api(
            ModuleRootArchitectureIT.SAMPLES
        ).evaluate(ModuleRootArchitectureIT.compiled(dir)).getFailureReport().getDetails();
        Assertions.assertTrue(
            !details.isEmpty() && details.stream().allMatch(line -> line.contains("api.Bad")),
            "Reguła przepuściła typ api zależny od wnętrza modułu albo odrzuciła typ api sąsiada"
        );
    }

    @Test
    void rejectsDependencyWithoutApiName() {
        Assertions.assertEquals(
            List.of("barrier -> building"),
            ModuleRootArchitectureIT.unnamed(
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
            .filter(type -> !ModuleRootArchitectureIT.INFO.equals(type.getSimpleName()))
            .filter(type -> roots.contains(type.getPackageName()))
            .map(JavaClass::getName)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static ArchRule api(final String base) {
        return ArchRuleDefinition.classes()
            .that().resideInAPackage(String.format("%s.*.api..", base))
            .and().doNotHaveSimpleName(ModuleRootArchitectureIT.INFO)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                String.format("%s.*.api..", base),
                String.format("%s.shared..", ModuleRootArchitectureIT.BASE),
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
                Class.forName(String.format("%s.%s", root, ModuleRootArchitectureIT.INFO))
                    .getAnnotation(org.springframework.modulith.ApplicationModule.class)
                    .allowedDependencies()
            );
        } catch (final ClassNotFoundException ex) {
            throw new IllegalStateException(String.format("Brak opisu modułu %s", root), ex);
        }
    }

    private static Set<String> withApi(final JavaClasses classes) {
        return ModuleRootArchitectureIT.closed().stream().filter(
            root -> classes.stream().anyMatch(
                type -> type.getPackageName().startsWith(String.format("%s.api", root))
            )
        ).map(ModuleRootArchitectureIT::name).collect(Collectors.toSet());
    }

    private static String name(final String root) {
        return root.substring(ModuleRootArchitectureIT.BASE.length() + 1);
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
        return String.format("%s.%s", ModuleRootArchitectureIT.SAMPLES, name);
    }

    private static JavaClasses compiled(final Path dir) throws IOException {
        return new ArchSamples(dir).compiled(ModuleRootArchitectureIT.SAMPLES);
    }

    private static void source(
        final Path dir, final String pack, final String name, final String body
    ) throws IOException {
        new ArchSamples(dir).write(pack, name, String.format("package %s;%n%s%n", pack, body));
    }
}
