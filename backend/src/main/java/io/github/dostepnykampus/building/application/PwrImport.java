/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Ręczny import z usług PWr (ADR-0012), uruchamiany profilem import-pwr. Gdy aplikacja jest
 * gotowa (po wszystkich zadaniach startu), uruchamia po kolei wszystkie kroki
 * {@link PwrImportStep} z argumentami polecenia, a po ostatnim zamyka aplikację. Wyjątek kroku
 * przerywa import przed następnymi krokami; dane zapisane wcześniej zostają.
 *
 * <p>Wymagania: US16, NFR-47</p>
 *
 * @since 0.1.0
 */
@Component
@Profile("import-pwr")
final class PwrImport implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Kroki importu w kolejności {@code @Order}.
     */
    private final List<PwrImportStep> steps;

    PwrImport(final List<PwrImportStep> steps) {
        this.steps = steps;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        final ApplicationArguments args = new DefaultApplicationArguments(event.getArgs());
        this.steps.forEach(step -> step.run(args));
        SpringApplication.exit(event.getApplicationContext());
    }
}
