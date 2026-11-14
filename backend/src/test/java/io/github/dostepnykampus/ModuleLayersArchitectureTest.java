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
import java.util.Optional;
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
 * Strażnik warstw w każdym module serwera: warstwą klasy jest pierwszy podpakiet pod pakietem
 * modułu i zależy ona tylko od warstw ze swojego wiersza tabeli i od korzenia modułu;
 * {@code domain} nie zależy też od Springa, Jacksona ani JDBC. Podpakiet spoza listy warstw jest
 * dozwolony tylko jako zapisany temat modułu i zachowuje się jak adapter. Moduły otwarte nie
 * podlegają regule. Wyjątki mają numer issue, a ich lista tylko maleje.
 *
 * @since 0.1.0
 */
final class ModuleLayersArchitectureTest {

    /**
     * Pakiet bazowy serwera.
     */
    private static final String BASE = "io.github.dostepnykampus";

    /**
     * Nazwa, pod którą reguła widzi podpakiet tematyczny modułu.
     */
    private static final String TOPIC = "temat";

    /**
     * Warstwy, od których może zależeć warstwa z klucza.
     */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
        "domain", Set.of(),
        "application", Set.of("domain", ModuleLayersArchitectureTest.TOPIC),
        "web", Set.of("application", "domain", "json", ModuleLayersArchitectureTest.TOPIC),
        "persistence", Set.of("domain", "json"),
        "json", Set.of("domain"),
        ModuleLayersArchitectureTest.TOPIC, Set.of("domain", "json")
    );

    /**
     * Pakiety, od których {@code domain} nie zależy.
     */
    private static final List<String> FRAMEWORKS = List.of(
        "org.springframework", "tools.jackson", "java.sql"
    );

    /**
     * Zapisane podpakiety tematyczne modułów, względem pakietu bazowego serwera.
     */
    private static final Set<String> TOPICS = Set.of();

    /**
     * Naruszenia, które dziś istnieją, i issue, które je usuwa.
     */
    private static final Map<String, Integer> KNOWN = Map.of();

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.layers";

    /**
     * Katalog na źródła i klasy przykładu, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void modulesKeepLayerDirectionOutsideExceptionList() {
        final Set<String> found = ModuleLayersArchitectureTest.production();
        found.removeAll(ModuleLayersArchitectureTest.KNOWN.keySet());
        Assertions.assertEquals(
            Set.of(), found,
            "Zależność łamie kierunek warstw modułu albo podpakiet nie jest warstwą ani tematem"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final List<String> stale = new ArrayList<>(ModuleLayersArchitectureTest.KNOWN.keySet());
        stale.removeAll(ModuleLayersArchitectureTest.production());
        Assertions.assertEquals(
            List.of(), stale,
            "Naruszenie z listy wyjątków już nie istnieje; usuń wpis"
        );
    }

    @Test
    void acceptsLayersInAllowedDirection() throws IOException {
        final String root = String.format("%s.ok", ModuleLayersArchitectureTest.SAMPLES);
        this.source(root, "domain", "Model", "public final class Model { }");
        this.source(
            root, "json", "Content",
            String.format("public final class Content { %s.domain.Model model; }", root)
        );
        this.source(
            root, "application", "Service",
            String.format("public final class Service { %s.domain.Model model; }", root)
        );
        this.source(
            root, "web", "Controller",
            String.format(
                "final class Controller { %1$s.application.Service service; %1$s.json.Content c; }",
                root
            )
        );
        Assertions.assertEquals(
            Set.of(),
            ModuleLayersArchitectureTest.violations(this.compiled(), List.of(root), Set.of()),
            "Reguła odrzuciła zależności zgodne z kierunkiem warstw"
        );
    }

    @Test
    void rejectsLayersAgainstDirection() throws IOException {
        final String root = String.format("%s.bad", ModuleLayersArchitectureTest.SAMPLES);
        this.source(root, "web", "Controller", "public final class Controller { }");
        this.source(root, "persistence", "Rows", "public final class Rows { }");
        this.source(
            root, "domain", "Model",
            String.format(
                "final class Model { %s.web.Controller c; java.sql.Connection db; }", root
            )
        );
        this.source(
            root, "json", "Content",
            String.format("final class Content { %s.persistence.Rows rows; }", root)
        );
        this.source(root, "misc", "Helper", "final class Helper { }");
        Assertions.assertEquals(
            Set.of(
                String.format("%s.domain.Model -> %s.web", root, root),
                String.format("%s.domain.Model -> java.sql", root),
                String.format("%s.json.Content -> %s.persistence", root, root),
                String.format("%s.misc", root)
            ),
            ModuleLayersArchitectureTest.violations(this.compiled(), List.of(root), Set.of()),
            "Reguła przepuściła zależność wbrew kierunkowi warstw albo nieznany podpakiet"
        );
    }

    private static Set<String> production() {
        return ModuleLayersArchitectureTest.violations(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ModuleLayersArchitectureTest.BASE),
            ApplicationModules.of(DostepnyKampusApplication.class).stream()
                .filter(module -> !module.isOpen())
                .map(ApplicationModule::getBasePackage)
                .map(JavaPackage::getName)
                .toList(),
            ModuleLayersArchitectureTest.TOPICS.stream()
                .map(topic -> String.format("%s.%s", ModuleLayersArchitectureTest.BASE, topic))
                .collect(Collectors.toSet())
        );
    }

    private static Set<String> violations(
        final JavaClasses classes, final Collection<String> roots, final Set<String> topics
    ) {
        final Set<String> found = new TreeSet<>();
        for (final JavaClass type : classes) {
            final Optional<String> root = ModuleLayersArchitectureTest.rootOf(type, roots);
            if (root.isEmpty() || type.getPackageName().equals(root.get())) {
                continue;
            }
            final String sub = ModuleLayersArchitectureTest.subpackage(type, root.get());
            final String layer = ModuleLayersArchitectureTest.layer(root.get(), sub, topics);
            if (layer.isEmpty()) {
                found.add(String.format("%s.%s", root.get(), sub));
                continue;
            }
            found.addAll(
                ModuleLayersArchitectureTest.wrong(type, root.get(), layer, topics)
            );
        }
        return found;
    }

    private static Set<String> wrong(
        final JavaClass type, final String root, final String layer, final Set<String> topics
    ) {
        final String owner = type.getName().replaceAll("\\$.*", "");
        final String own = ModuleLayersArchitectureTest.subpackage(type, root);
        final Set<String> found = new TreeSet<>();
        for (final Dependency dependency : type.getDirectDependenciesFromSelf()) {
            final JavaClass target = dependency.getTargetClass();
            final String name = target.getPackageName();
            for (final String framework : ModuleLayersArchitectureTest.FRAMEWORKS) {
                if ("domain".equals(layer) && String.format("%s.", name)
                    .startsWith(String.format("%s.", framework))) {
                    found.add(String.format("%s -> %s", owner, framework));
                }
            }
            if (!name.startsWith(String.format("%s.", root))) {
                continue;
            }
            final String sub = ModuleLayersArchitectureTest.subpackage(target, root);
            final String other = ModuleLayersArchitectureTest.layer(root, sub, topics);
            if (!other.isEmpty() && !sub.equals(own)
                && !ModuleLayersArchitectureTest.ALLOWED.get(layer).contains(other)) {
                found.add(String.format("%s -> %s.%s", owner, root, sub));
            }
        }
        return found;
    }

    private static Optional<String> rootOf(final JavaClass type, final Collection<String> roots) {
        final String name = String.format("%s.", type.getPackageName());
        return roots.stream()
            .filter(root -> name.startsWith(String.format("%s.", root)))
            .findFirst();
    }

    private static String subpackage(final JavaClass type, final String root) {
        return type.getPackageName().substring(root.length() + 1).split("\\.", 2)[0];
    }

    private static String layer(final String root, final String sub, final Set<String> topics) {
        final String result;
        if (ModuleLayersArchitectureTest.ALLOWED.containsKey(sub)
            && !ModuleLayersArchitectureTest.TOPIC.equals(sub)) {
            result = sub;
        } else if (topics.contains(String.format("%s.%s", root, sub))) {
            result = ModuleLayersArchitectureTest.TOPIC;
        } else {
            result = "";
        }
        return result;
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
            this.dir.resolve(ModuleLayersArchitectureTest.SAMPLES.replace('.', '/'))
        );
    }

    private void source(
        final String root, final String layer, final String name, final String body
    ) throws IOException {
        final String pack = String.format("%s.%s", root, layer);
        final Path folder = this.dir.resolve("src").resolve(pack.replace('.', '/'));
        Files.createDirectories(folder);
        Files.writeString(
            folder.resolve(String.format("%s.java", name)),
            String.format("package %s;%n%s%n", pack, body)
        );
    }
}
