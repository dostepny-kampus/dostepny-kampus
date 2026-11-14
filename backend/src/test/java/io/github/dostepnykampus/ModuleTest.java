/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Test na kontekście jednego modułu: Spring ładuje tylko beany modułu z pakietu klasy testu
 * i modułu {@code shared}, a PostGIS i tożsamość testowa są te same co w
 * {@link IntegrationTest}. Bean modułu, który potrzebuje wnętrza innego modułu, nie wstanie.
 * Beany API sąsiednich modułów zastępuje {@code @MockitoBean}.
 *
 * <p>Znacznik {@code kontekst-springa} wyłącza te testy z analizy PIT.</p>
 *
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Tag("kontekst-springa")
@ApplicationModuleTest(extraIncludes = "shared")
@Import(
    {
        PostgisTestConfiguration.class,
        IdentityTestConfiguration.class,
        MockMvcTestConfiguration.class
    }
)
public @interface ModuleTest {
}
