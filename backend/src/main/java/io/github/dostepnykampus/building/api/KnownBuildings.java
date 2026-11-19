/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.api;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Budynki PWr znane serwerowi dla innych modułów, np. kontrola budynku zgłoszenia
 * podanego przez klienta.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface KnownBuildings {

    /**
     * Mówi, czy budynek o symbolu istnieje.
     *
     * @param symbol Symbol budynku
     * @return Prawda, gdy budynek istnieje
     */
    boolean known(String symbol);

    /**
     * Szuka etykiety piętra na planie.
     *
     * @param symbol Budynek albo null
     * @param id Piętro albo null
     * @return Etykieta albo puste
     */
    default Optional<String> floorLabel(final @Nullable String symbol, final @Nullable String id) {
        return Optional.empty();
    }

    /**
     * Szuka miejsca wejścia. Wejście wspólne kilku budynków daje wiersz budynku
     * z podpowiedzi, a bez niej budynek o najmniejszym symbolu.
     *
     * <p>Wymagania: BR-8, BR-14</p>
     *
     * @param id Identyfikator wejścia
     * @param symbol Podpowiedź budynku albo null
     * @return Miejsce wejścia albo puste, gdy wejścia nie ma
     */
    default Optional<EntrancePlace> entrance(final String id, final @Nullable String symbol) {
        return Optional.empty();
    }
}
