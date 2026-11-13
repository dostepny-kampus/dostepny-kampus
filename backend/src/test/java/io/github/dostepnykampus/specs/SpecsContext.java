/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.dostepnykampus.IntegrationTest;

/**
 * Kontekst Springa dla kroków scenariuszy: ten sam co w testach z
 * {@link IntegrationTest}, więc scenariusze nie zakładają osobnej bazy.
 *
 * @since 0.1.0
 */
@CucumberContextConfiguration
@IntegrationTest
public final class SpecsContext {

    /**
     * Kontekst bez własnego stanu.
     */
    public SpecsContext() {
        // Cucumber tworzy tę klasę sam; stan trzyma kontekst Springa.
    }
}
