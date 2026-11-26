/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import io.github.dostepnykampus.building.domain.BuildingCard;
import io.github.dostepnykampus.building.domain.BuildingList;
import io.github.dostepnykampus.building.domain.Buildings;
import io.github.dostepnykampus.building.domain.FloorList;
import io.github.dostepnykampus.building.domain.MarkedEntrance;
import io.github.dostepnykampus.building.domain.Placement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Odczyt budynków dla warstwy web: kontroler nie sięga do portu
 * {@link Buildings}, tylko do tej usługi.
 *
 * <p>Wymagania: US1, US2, US17, US16</p>
 *
 * @since 0.1.0
 */
@Service
public final class BuildingReading {

    /** Port odczytu budynków. */
    private final Buildings buildings;

    /**
     * Ctor.
     *
     * @param buildings Port odczytu budynków
     */
    public BuildingReading(final Buildings buildings) {
        this.buildings = buildings;
    }

    /**
     * Zwraca budynki posortowane według symbolu.
     *
     * @return Lista budynków z atrybucją konturów
     */
    public BuildingList listBuildings() {
        return this.buildings.listBuildings();
    }

    /**
     * Ustala budynek konturu albo najbliższy w promieniu podpowiedzi.
     *
     * @param lat Szerokość geograficzna w stopniach WGS84
     * @param lon Długość geograficzna w stopniach WGS84
     * @return Położenie punktu względem budynków
     */
    public Placement locate(final double lat, final double lon) {
        return this.buildings.locate(lat, lon);
    }

    /**
     * Szuka karty budynku z wejściami.
     *
     * @param symbol Symbol budynku
     * @return Karta budynku albo puste dla nieznanego symbolu
     */
    public Optional<BuildingCard> building(final String symbol) {
        return this.buildings.building(symbol);
    }

    /**
     * Szuka pięter budynku.
     *
     * @param symbol Symbol budynku
     * @return Piętra budynku ze źródłem danych albo puste dla nieznanego symbolu
     */
    public Optional<FloorList> floors(final String symbol) {
        return this.buildings.floors(symbol);
    }

    /**
     * Wejścia z oznaczeniem zmienionym na obchodzie od {@code since} włącznie.
     *
     * <p>Wymagania: US17.2</p>
     *
     * @param since Chwila, od której liczą się zmiany
     * @return Wejścia z budynkiem od najdawniej zmienionego
     */
    public List<MarkedEntrance> markedSince(final Instant since) {
        return this.buildings.markedSince(since);
    }
}
