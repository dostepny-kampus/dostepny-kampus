/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Etykiety pięter na planach budynków.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface FloorLabels {

    /**
     * Zwraca etykietę piętra z planu.
     *
     * @param symbol Symbol budynku albo null, gdy poza budynkiem
     * @param id Identyfikator piętra albo null, gdy podano numer
     * @return Etykieta albo puste, gdy piętra nie ma
     */
    Optional<String> label(@Nullable String symbol, @Nullable String id);
}
