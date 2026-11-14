/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Strażnik kierunku warstw w każdym zamkniętym module serwera, w obu układach modułu: warstwy pod
 * korzeniem i warstwy w temacie. Warstwą klasy jest pierwszy segment pakietu pod korzeniem modułu,
 * który jest nazwą warstwy; klasa bez takiego segmentu należy do dziedziny, a klasa w korzeniu
 * modułu do API. Kierunek sprawdza reguła warstw ArchUnit; dziedzina nie zależy od Springa,
 * Jacksona ani JDBC. Wyjątki mają numer issue, a ich lista tylko maleje.
 *
 * @since 0.1.0
 */
final class LayerDirectionArchitectureTest {

    /**
     * Pakiety, od których dziedzina nie zależy.
     */
    private static final List<String> FRAMEWORKS = List.of(
        "org.springframework..", "tools.jackson..", "java.sql.."
    );

    /**
     * Pakiety dziedziny, które dziś zależą od frameworka, i issue, które to usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of(
        "io.github.dostepnykampus.barrier.photo", 1678
    );

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.direction";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void modulesKeepLayerDirection() {
        LayerDirectionArchitectureTest.direction(ModuleLayers.modules())
            .check(LayerDirectionArchitectureTest.production());
    }

    @Test
    void domainSkipsFrameworksOutsideExceptionList() {
        LayerDirectionArchitectureTest.frameworks(
            ModuleLayers.modules(),
            LayerDirectionArchitectureTest.KNOWN.keySet()
        ).check(LayerDirectionArchitectureTest.production());
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final JavaClasses classes = LayerDirectionArchitectureTest.production();
        Assertions.assertEquals(
            List.of(),
            LayerDirectionArchitectureTest.KNOWN.keySet().stream()
                .filter(pack -> !LayerDirectionArchitectureTest.violates(classes, pack))
                .toList(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @Test
    void acceptsBothModuleLayouts() throws IOException {
        this.type("flat.domain", "Model", "");
        this.type("flat.json", "Content", "flat.domain.Model");
        this.type("flat.persistence", "Rows", "flat.json.Content");
        this.type("flat.application", "Service", "flat.domain.Model");
        this.type("flat.web", "Controller", "flat.application.Service");
        this.type("topic.part", "Model", "");
        this.type("topic.part.application", "Service", "topic.part.Model");
        this.type("topic.part.web", "Controller", "topic.part.application.Service");
        Assertions.assertFalse(
            LayerDirectionArchitectureTest.direction(
                Stream.of("flat", "topic").map(LayerDirectionArchitectureTest::at).toList()
            ).evaluate(this.compiled()).hasViolation(),
            "Reguła odrzuciła zależności zgodne z kierunkiem warstw"
        );
    }

    @Test
    void rejectsDomainCallingWeb() throws IOException {
        this.type("up.web", "Controller", "");
        this.type("up.submission", "Model", "up.web.Controller");
        Assertions.assertTrue(
            LayerDirectionArchitectureTest.direction(
                List.of(LayerDirectionArchitectureTest.at("up"))
            ).evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła dziedzinę tematu zależną od warstwy web"
        );
    }

    @Test
    void rejectsAdapterCallingAdapter() throws IOException {
        this.type("side.photo.storage", "Store", "");
        this.type("side.photo.json", "Content", "side.photo.storage.Store");
        Assertions.assertTrue(
            LayerDirectionArchitectureTest.direction(
                List.of(LayerDirectionArchitectureTest.at("side"))
            ).evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła adapter json zależny od adaptera storage"
        );
    }

    @Test
    void rejectsFrameworkInTopicDomain() throws IOException {
        this.type("frame.submission", "Model", "java.sql.Connection");
        Assertions.assertTrue(
            LayerDirectionArchitectureTest.frameworks(
                List.of(LayerDirectionArchitectureTest.at("frame")), Set.of()
            ).evaluate(this.compiled()).hasViolation(),
            "Reguła przepuściła dziedzinę tematu zależną od JDBC"
        );
    }

    private static ArchRule direction(final Collection<String> roots) {
        final Function<String, DescribedPredicate<JavaClass>> layer =
            name -> ModuleLayers.in(roots, name);
        return Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("api").definedBy(layer.apply("api"))
            .layer("domain").definedBy(layer.apply("domain"))
            .layer("application").definedBy(layer.apply("application"))
            .layer("web").definedBy(layer.apply("web"))
            .layer("persistence").definedBy(layer.apply("persistence"))
            .layer("json").definedBy(layer.apply("json"))
            .layer("storage").definedBy(layer.apply("storage"))
            .whereLayer("web").mayNotBeAccessedByAnyLayer()
            .whereLayer("application").mayOnlyBeAccessedByLayers("web")
            .whereLayer("persistence").mayNotBeAccessedByAnyLayer()
            .whereLayer("storage").mayNotBeAccessedByAnyLayer()
            .whereLayer("json").mayOnlyBeAccessedByLayers("web", "persistence");
    }

    private static ArchRule frameworks(final Collection<String> roots, final Set<String> skipped) {
        return ArchRuleDefinition.noClasses()
            .that(ModuleLayers.in(roots, "domain"))
            .and(DescribedPredicate.not(LayerDirectionArchitectureTest.inside(skipped)))
            .should().dependOnClassesThat()
            .resideInAnyPackage(LayerDirectionArchitectureTest.frameworkPackages())
            .allowEmptyShould(true);
    }

    private static boolean violates(final JavaClasses classes, final String pack) {
        return ArchRuleDefinition.noClasses()
            .that().resideInAPackage(String.format("%s..", pack))
            .should().dependOnClassesThat()
            .resideInAnyPackage(LayerDirectionArchitectureTest.frameworkPackages())
            .evaluate(classes).hasViolation();
    }

    private static DescribedPredicate<JavaClass> inside(final Set<String> packs) {
        return DescribedPredicate.describe(
            "w pakiecie z listy wyjątków",
            (JavaClass type) -> packs.stream().anyMatch(
                pack -> String.format("%s.", type.getPackageName())
                    .startsWith(String.format("%s.", pack))
            )
        );
    }

    private static JavaClasses production() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.dostepnykampus");
    }

    private static String[] frameworkPackages() {
        return LayerDirectionArchitectureTest.FRAMEWORKS.toArray(String[]::new);
    }

    private static String at(final String root) {
        return String.format("%s.%s", LayerDirectionArchitectureTest.SAMPLES, root);
    }

    private JavaClasses compiled() throws IOException {
        return new ArchSamples(this.dir).compiled(LayerDirectionArchitectureTest.SAMPLES);
    }

    private void type(final String pack, final String name, final String field)
        throws IOException {
        final String full = LayerDirectionArchitectureTest.at(pack);
        final String body;
        if (field.isEmpty()) {
            body = String.format("public final class %s { }", name);
        } else if (field.startsWith("java.")) {
            body = String.format("public final class %s { %s field; }", name, field);
        } else {
            body = String.format(
                "public final class %s { %s field; }",
                name, LayerDirectionArchitectureTest.at(field)
            );
        }
        new ArchSamples(this.dir).write(full, name, String.format("package %s;%n%s%n", full, body));
    }
}
