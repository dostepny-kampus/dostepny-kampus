/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectFile;
import org.junit.platform.suite.api.Suite;

/**
 * Scenariusze serwera wykonywane wprost z plików {@code specs/*.feature}.
 *
 * <p>Wykonują się scenariusze z tagiem {@code @serwer} bez tagu
 * {@code @do-zrobienia}; krok bez definicji daje czerwony test.</p>
 *
 * @since 0.1.0
 */
@Suite
@IncludeEngines("cucumber")
@SelectFile("../specs/uprawnienia.feature")
@SelectFile("../specs/zgloszenie-szybkie.feature")
@ConfigurationParameter(
    key = Constants.GLUE_PROPERTY_NAME, value = "io.github.dostepnykampus.specs"
)
@ConfigurationParameter(
    key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "@serwer and not @do-zrobienia"
)
public final class ServerSpecs {

    /**
     * Zestaw bez własnego stanu.
     */
    public ServerSpecs() {
        // Scenariusze wykonuje silnik Cucumber wskazany w adnotacjach.
    }
}
