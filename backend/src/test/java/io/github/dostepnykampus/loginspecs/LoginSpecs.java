/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.loginspecs;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectFile;
import org.junit.platform.suite.api.Suite;

/**
 * Scenariusze serwera z tagiem {@code @usluga-logowania}: konto zakłada i loguje
 * prawdziwy Keycloak, więc nie wykonuje ich zestaw z tokenami testowymi.
 *
 * @since 0.1.0
 */
@Suite
@Tag("kontekst-springa")
@IncludeEngines("cucumber")
@SelectFile("../specs/uprawnienia.feature")
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME, value = "io.github.dostepnykampus.loginspecs"
)
@ConfigurationParameter(
    key = Constants.FILTER_TAGS_PROPERTY_NAME,
    value = "@serwer and @usluga-logowania and not @do-zrobienia"
)
public final class LoginSpecs {

    /**
     * Zestaw bez własnego stanu.
     */
    public LoginSpecs() {
        // Scenariusze wykonuje silnik Cucumber wskazany w adnotacjach.
    }
}
