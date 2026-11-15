/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

/**
 * Port zapisu znaczników piętra obejrzanego.
 *
 * <p>Wymagania: US15.1, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface MarkerRepository {

    /**
     * Zapisuje znacznik; znacznik o zapisanym już identyfikatorze zostaje bez zmian.
     *
     * @param marker Znacznik do zapisu
     */
    void place(NewMarker marker);
}
