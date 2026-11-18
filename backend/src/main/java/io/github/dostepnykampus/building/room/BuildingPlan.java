/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Plan budynku z usługi ArcGIS PWr do zapisu w jednej transakcji.
 *
 * <p>Wymagania: US16, US18.2, NFR-40, BR-24</p>
 *
 * @param building Symbol budynku
 * @param floors Piętra budynku
 * @param rooms Pomieszczenia pięter
 * @param entrances Wpisy warstwy wejść PWr: wejścia z oznaczeniem, nazwą, opisem i drzwiami
 *  oraz wyjścia ewakuacyjne
 * @param edited Dzień ostatniej edycji warstwy wejść albo null, gdy usługa go nie podaje
 * @since 0.1.0
 */
public record BuildingPlan(
    String building, List<PlanFloor> floors, List<PlanRoom> rooms, List<PlanEntrance> entrances,
    @Nullable LocalDate edited
) {

    /**
     * Plan bez dnia edycji warstwy wejść.
     *
     * @param building Symbol budynku
     * @param floors Piętra budynku
     * @param rooms Pomieszczenia pięter
     * @param entrances Wpisy warstwy wejść PWr
     */
    public BuildingPlan(
        final String building, final List<PlanFloor> floors, final List<PlanRoom> rooms,
        final List<PlanEntrance> entrances
    ) {
        this(building, floors, rooms, entrances, null);
    }
}
