/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Strażnik zależności między pakietami serwera: każdy pakiet jest osobnym wycinkiem i wycinki
 * nie tworzą cykli, także między tematem a jego warstwą. Klasa z podpakietu modułu nie zależy od
 * typów w korzeniu własnego modułu, bo korzeń zawiera tylko opis modułu, API i tematy albo
 * warstwy. Wyjątki mają numer issue, a ich lista tylko maleje.
 *
 * @since 0.1.0
 */
final class PackageCycleArchitectureTest {

    /**
     * Pakiet aplikacji; jego podpakiety są wycinkami.
     */
    private static final String BASE = "io.github.dostepnykampus";

    /**
     * Korzenie modułów, których typy są dziś używane w podpakietach, i issue, które to usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of(
        "io.github.dostepnykampus.building", 1679
    );

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.cycle";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void packagesHaveNoCycles() {
        PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.BASE)
            .check(PackageCycleArchitectureTest.production());
    }

    @Test
    void subpackagesSkipModuleRootOutsideExceptionList() {
        PackageCycleArchitectureTest.root(
            PackageCycleArchitectureTest.modules(), PackageCycleArchitectureTest.KNOWN.keySet()
        ).check(PackageCycleArchitectureTest.production());
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final JavaClasses classes = PackageCycleArchitectureTest.production();
        Assertions.assertEquals(
            List.of(),
            PackageCycleArchitectureTest.KNOWN.keySet().stream()
                .filter(pack -> !PackageCycleArchitectureTest.usedInside(classes, pack))
                .toList(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @Test
    void acceptsOneWayDependencies() throws IOException {
        this.type("way.api", "Event", "");
        this.type("way.submission", "Model", "way.api.Event");
        this.type("way.submission.web", "Controller", "way.submission.Model");
        this.type("way.photo", "Model", "way.submission.Model");
        final JavaClasses classes = this.compiled();
        Assertions.assertFalse(
            PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.SAMPLES)
                .evaluate(classes).hasViolation()
                || PackageCycleArchitectureTest.root(
                    List.of(PackageCycleArchitectureTest.at("way")), Set.of()
                ).evaluate(classes).hasViolation(),
            "Reguła odrzuciła zależności bez cyklu i bez korzenia modułu"
        );
    }

    @Test
    void rejectsCycleBetweenTopics() throws IOException {
        this.type("loop.photo", "Model", "loop.submission.Model");
        this.type("loop.submission", "Model", "loop.photo.Model");
        Assertions.assertTrue(
            PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.SAMPLES)
                .evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła cykl między tematami"
        );
    }

    @Test
    void rejectsCycleBetweenTopicAndLayer() throws IOException {
        this.type("nest.submission.web", "Controller", "");
        this.type("nest.submission", "Model", "nest.submission.web.Controller");
        this.type("nest.submission.web", "Request", "nest.submission.Model");
        Assertions.assertTrue(
            PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.SAMPLES)
                .evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła cykl między tematem a jego warstwą"
        );
    }

    @Test
    void rejectsSubpackageUsingModuleRoot() throws IOException {
        this.type("top", "Area", "");
        this.type("top.persistence", "Rows", "top.Area");
        Assertions.assertTrue(
            PackageCycleArchitectureTest.root(
                List.of(PackageCycleArchitectureTest.at("top")), Set.of()
            ).evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła podpakiet zależny od korzenia modułu"
        );
    }

    private static ArchRule cycles(final String base) {
        return SlicesRuleDefinition.slices()
            .matching(String.format("%s.(**)", base))
            .should().beFreeOfCycles();
    }

    private static ArchRule root(final Collection<String> roots, final Set<String> skipped) {
        return ArchRuleDefinition.classes().should(
            ArchConditions.onlyHaveDependenciesWhere(
                DescribedPredicate.describe(
                    "klasa z podpakietu nie zależy od korzenia własnego modułu",
                    (Dependency dependency) -> roots.stream()
                        .filter(pack -> !skipped.contains(pack))
                        .noneMatch(pack -> PackageCycleArchitectureTest.usesRoot(dependency, pack))
                )
            )
        );
    }

    private static boolean usedInside(final JavaClasses classes, final String root) {
        return PackageCycleArchitectureTest.root(List.of(root), Set.of())
            .evaluate(classes).hasViolation();
    }

    private static boolean usesRoot(final Dependency dependency, final String root) {
        return root.equals(dependency.getTargetClass().getPackageName())
            && dependency.getOriginClass().getPackageName()
                .startsWith(String.format("%s.", root));
    }

    private static List<String> modules() {
        return ApplicationModules.of(DostepnyKampusApplication.class).stream()
            .map(ApplicationModule::getBasePackage)
            .map(JavaPackage::getName)
            .toList();
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PackageCycleArchitectureTest.BASE);
    }

    private static String at(final String pack) {
        return String.format("%s.%s", PackageCycleArchitectureTest.SAMPLES, pack);
    }

    private JavaClasses compiled() throws IOException {
        return new ArchSamples(this.dir).compiled(PackageCycleArchitectureTest.SAMPLES);
    }

    private void type(final String pack, final String name, final String field)
        throws IOException {
        final String full = PackageCycleArchitectureTest.at(pack);
        final String body;
        if (field.isEmpty()) {
            body = String.format("public final class %s { }", name);
        } else {
            body = String.format(
                "public final class %s { %s field; }",
                name, PackageCycleArchitectureTest.at(field)
            );
        }
        new ArchSamples(this.dir).write(full, name, String.format("package %s;%n%s%n", full, body));
    }
}
