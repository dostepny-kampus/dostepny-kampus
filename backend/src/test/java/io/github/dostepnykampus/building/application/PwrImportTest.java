/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Przebieg importu PWr uruchamia kroki po kolei z argumentami polecenia i dopiero potem zamyka
 * aplikację; błąd kroku przerywa import.
 *
 * <p>Wymagania: US16, NFR-47</p>
 *
 * @since 0.1.0
 */
final class PwrImportTest {

    @Test
    void runsStepsInOrderWithCommandArguments() {
        final List<String> runs = new ArrayList<>(2);
        new PwrImport(
            List.of(
                args -> runs.add(String.format("plany %s", args.getOptionValues("budynek"))),
                args -> runs.add(String.format("ścieżki %s", args.getOptionNames()))
            )
        ).onApplicationEvent(PwrImportTest.ready(new StaticApplicationContext()));
        MatcherAssert.assertThat(
            "Import nie uruchomił kroków po kolei z argumentami polecenia",
            runs,
            Matchers.contains("plany [D-2]", "ścieżki [budynek]")
        );
    }

    @Test
    void closesApplicationAfterLastStep() {
        final StaticApplicationContext context = new StaticApplicationContext();
        final List<Boolean> active = new ArrayList<>(1);
        new PwrImport(List.of(args -> active.add(context.isActive())))
            .onApplicationEvent(PwrImportTest.ready(context));
        MatcherAssert.assertThat(
            "Import zamknął aplikację przed krokiem albo nie zamknął jej po ostatnim kroku",
            List.of(active, context.isActive()),
            Matchers.contains(List.of(true), false)
        );
    }

    @Test
    void stopsOnFailedStep() {
        final List<String> runs = new ArrayList<>(1);
        final PwrImport run = new PwrImport(
            List.of(
                args -> {
                    throw new IllegalStateException("usługa PWr nie odpowiada");
                },
                args -> runs.add("ścieżki")
            )
        );
        final StaticApplicationContext context = new StaticApplicationContext();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> run.onApplicationEvent(PwrImportTest.ready(context)),
            "Błąd kroku nie przerwał importu"
        );
        MatcherAssert.assertThat(
            "Import po błędzie kroku uruchomił następny krok",
            runs,
            Matchers.empty()
        );
    }

    private static ApplicationReadyEvent ready(final StaticApplicationContext context) {
        context.refresh();
        return new ApplicationReadyEvent(
            new SpringApplication(), new String[] {"--budynek=D-2"}, context, Duration.ZERO
        );
    }
}
