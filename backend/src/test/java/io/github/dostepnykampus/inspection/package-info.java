/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Testy modułu znaczników pięter: zapis znaczników wysłanych z telefonu.
 *
 * <p>Wymagania: US15.1, BR-20</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "identity :: api", "shared"})
package io.github.dostepnykampus.inspection;

import org.springframework.modulith.ApplicationModule;
