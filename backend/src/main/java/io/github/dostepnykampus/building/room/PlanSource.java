/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.List;

/**
 * Plany pięter z warstw pięter i pomieszczeń usługi FeatureServer.
 *
 * <p>Wymagania: US16, NFR-40</p>
 *
 * @since 0.1.0
 */
public interface PlanSource {

    /**
     * Piętra z warstwy pięter.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pięter
     * @return Piętra w kolejności usługi
     */
    List<PlanFloor> floors(String service, int layer);

    /**
     * Pomieszczenia z warstwy pomieszczeń.
     *
     * @param service Adres usługi FeatureServer bez końcowego ukośnika
     * @param layer Numer warstwy pomieszczeń
     * @return Pomieszczenia z konturem w kolejności usługi
     */
    List<PlanRoom> rooms(String service, int layer);
}
