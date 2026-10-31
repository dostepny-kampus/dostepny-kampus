/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Testy zadania INFRA: moduły serwera według Spring Modulith.
 *
 * <p>Moduły z propozycji projektu to podpakiety pakietu bazowego.
 * Weryfikacja odrzuca cykle i dostęp do wnętrza cudzego modułu.</p>
 *
 * @since 0.1.0
 */
final class ModularityTest {

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
}
