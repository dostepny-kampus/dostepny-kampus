/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.archsamples.modules.alpha;

import io.github.dostepnykampus.archsamples.modules.beta.Beta;

/**
 * Przykład typu, który sięga do modułu spoza listy dozwolonych zależności.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Alpha {

    /**
     * Zwraca typ z cudzego modułu.
     *
     * @return Obiekt modułu beta
     */
    Beta beta();
}
