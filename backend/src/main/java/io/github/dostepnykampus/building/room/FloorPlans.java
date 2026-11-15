/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

package io.github.dostepnykampus.building.room;

/**
 * Zapis zaimportowanych pięter i pomieszczeń budynku oraz piętra jego wejść.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface FloorPlans {

    /**
     * Zapisuje piętra oraz pomieszczenia budynku bez duplikatów i ustawia piętro jego wejść.
     *
     * @param plan Plan budynku z usługi
     */
    void save(BuildingPlan plan);
}
