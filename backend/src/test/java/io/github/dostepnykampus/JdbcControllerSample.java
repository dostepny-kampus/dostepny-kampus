/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.RestController;

/**
 * Negatywny przykład kontrolera dla ArchUnit, poza kontekstem testów integracyjnych.
 *
 * @since 0.1.0
 */
@RestController
@Profile("archunit-fixture")
final class JdbcControllerSample {

    Integer query(final JdbcClient jdbc) {
        return jdbc.sql("SELECT 1").query(Integer.class).single();
    }
}
