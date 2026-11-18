/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.List;

/**
 * Plan budynku z usługi ArcGIS PWr do zapisu w jednej transakcji.
 *
 * <p>Wymagania: US16, US18.2, NFR-40</p>
 *
 * @param building Symbol budynku
 * @param floors Piętra budynku
 * @param rooms Pomieszczenia pięter
 * @param entrances Wejścia PWr, od których wejście budynku bierze piętro
 * @since 0.1.0
 */
public record BuildingPlan(
    String building, List<PlanFloor> floors, List<PlanRoom> rooms, List<PlanEntrance> entrances
) {
}
