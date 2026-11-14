/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

package io.github.dostepnykampus.building.room;

import java.util.List;

/**
 * Zapis zaimportowanych pięter i pomieszczeń budynku.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface FloorPlans {

    /**
     * Zapisuje piętra oraz pomieszczenia budynku bez duplikatów.
     *
     * @param building Symbol budynku
     * @param floors Lista pięter
     * @param rooms Lista pomieszczeń
     */
    void save(String building, List<PlanFloor> floors, List<PlanRoom> rooms);
}
