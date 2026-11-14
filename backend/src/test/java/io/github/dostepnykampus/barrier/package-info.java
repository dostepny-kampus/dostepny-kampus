/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Testy modułu zgłoszeń: zapis zgłoszeń na serwerze.
 *
 * <p>Wymagania: US4, US7, BR-9</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "shared"})
package io.github.dostepnykampus.barrier;

import org.springframework.modulith.ApplicationModule;
