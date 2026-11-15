/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
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
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class PackageCycleArchitectureTest {

    /**
     * Korzenie modułów, których typy są dziś używane w podpakietach, i issue, które to usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of();

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.cycle";

    @ArchTest
    void packagesHaveNoCycles(final JavaClasses classes) {
        PackageCycleArchitectureTest.cycles("io.github.dostepnykampus").check(classes);
    }

    @ArchTest
    void subpackagesSkipModuleRootOutsideExceptionList(final JavaClasses classes) {
        KnownViolations.check(
            classes,
            PackageCycleArchitectureTest.KNOWN,
            known -> PackageCycleArchitectureTest.root(
                PackageCycleArchitectureTest.modules(), known
            )
        );
    }

    @Test
    void acceptsOneWayDependencies(@TempDir final Path dir) throws IOException {
        PackageCycleArchitectureTest.type(dir, "way.api", "Event", "");
        PackageCycleArchitectureTest.type(dir, "way.submission", "Model", "way.api.Event");
        PackageCycleArchitectureTest.type(
            dir, "way.submission.web", "Controller", "way.submission.Model"
        );
        PackageCycleArchitectureTest.type(dir, "way.photo", "Model", "way.submission.Model");
        final JavaClasses classes = PackageCycleArchitectureTest.compiled(dir);
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
    void rejectsCycleBetweenTopics(@TempDir final Path dir) throws IOException {
        PackageCycleArchitectureTest.type(dir, "loop.photo", "Model", "loop.submission.Model");
        PackageCycleArchitectureTest.type(dir, "loop.submission", "Model", "loop.photo.Model");
        Assertions.assertTrue(
            PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.SAMPLES)
                .evaluate(PackageCycleArchitectureTest.compiled(dir)).hasViolation(),
            "Reguła przepuściła cykl między tematami"
        );
    }

    @Test
    void rejectsCycleBetweenTopicAndLayer(@TempDir final Path dir) throws IOException {
        PackageCycleArchitectureTest.type(dir, "nest.submission.web", "Controller", "");
        PackageCycleArchitectureTest.type(
            dir, "nest.submission", "Model", "nest.submission.web.Controller"
        );
        PackageCycleArchitectureTest.type(
            dir, "nest.submission.web", "Request", "nest.submission.Model"
        );
        Assertions.assertTrue(
            PackageCycleArchitectureTest.cycles(PackageCycleArchitectureTest.SAMPLES)
                .evaluate(PackageCycleArchitectureTest.compiled(dir)).hasViolation(),
            "Reguła przepuściła cykl między tematem a jego warstwą"
        );
    }

    @Test
    void rejectsSubpackageUsingModuleRoot(@TempDir final Path dir) throws IOException {
        PackageCycleArchitectureTest.type(dir, "top", "Area", "");
        PackageCycleArchitectureTest.type(dir, "top.persistence", "Rows", "top.Area");
        Assertions.assertTrue(
            PackageCycleArchitectureTest.root(
                List.of(PackageCycleArchitectureTest.at("top")), Set.of()
            ).evaluate(PackageCycleArchitectureTest.compiled(dir)).hasViolation(),
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

    private static String at(final String pack) {
        return String.format("%s.%s", PackageCycleArchitectureTest.SAMPLES, pack);
    }

    private static JavaClasses compiled(final Path dir) throws IOException {
        return new ArchSamples(dir).compiled(PackageCycleArchitectureTest.SAMPLES);
    }

    private static void type(
        final Path dir, final String pack, final String name, final String field
    ) throws IOException {
        final String type;
        if (field.isEmpty()) {
            type = field;
        } else {
            type = PackageCycleArchitectureTest.at(field);
        }
        new ArchSamples(dir).type(PackageCycleArchitectureTest.at(pack), name, type);
    }
}
