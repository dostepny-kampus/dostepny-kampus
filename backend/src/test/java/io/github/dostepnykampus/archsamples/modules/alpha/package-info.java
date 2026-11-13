/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Przykład modułu z pustą listą dozwolonych zależności.
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.archsamples.modules.alpha;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
