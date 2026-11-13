/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.building.web.BuildingCardResponse;
import io.github.dostepnykampus.building.web.BuildingListResponse;
import io.github.dostepnykampus.shared.error.InvalidInput;
import io.github.dostepnykampus.shared.error.MissingResource;
import io.github.dostepnykampus.shared.geo.Wgs84;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * Publiczny odczyt budynków: lista, karta i podpowiedź dla punktu. Błędy
 * idą wspólnymi typami z opisem w {@code detail}, a zakres współrzędnych
 * sprawdza {@link Wgs84}.
 *
 * @since 0.1.0
 */
@RestController
public final class BuildingsController {

    /**
     * Odczyt budynków z bazy.
     */
    private final BuildingQuery query;

    /**
     * Parser konturów i położeń GeoJSON do odpowiedzi.
     */
    private final ObjectMapper mapper;

    BuildingsController(final BuildingQuery query, final ObjectMapper mapper) {
        this.query = query;
        this.mapper = mapper;
    }

    /**
     * Zwraca budynki posortowane według symbolu, z konturem GeoJSON.
     *
     * @return Lista budynków z atrybucją konturów
     */
    @GetMapping("/buildings")
    BuildingListResponse listBuildings() {
        return BuildingListResponse.of(this.query.listBuildings(), this.mapper);
    }

    /**
     * Zwraca budynek konturu albo najbliższy w promieniu podpowiedzi.
     *
     * @param lat Szerokość geograficzna w stopniach WGS84
     * @param lon Długość geograficzna w stopniach WGS84
     * @return Podpowiedź budynku; brak symbolu oznacza punkt poza budynkiem
     */
    @GetMapping("/buildings/locate")
    LocateResult locateBuilding(
        @RequestParam("lat") final double lat,
        @RequestParam("lon") final double lon
    ) {
        if (!Wgs84.LATITUDE.holds(lat) || !Wgs84.LONGITUDE.holds(lon)) {
            throw new InvalidInput(
                String.format("Punkt %s, %s jest poza zakresem WGS84", lat, lon)
            );
        }
        return new LocateResults(this.query.locate(lat, lon)).result();
    }

    /**
     * Zwraca kartę budynku z wejściami: główne pierwsze, potem według
     * identyfikatora. Budynek bez wejść ma pustą listę, nieznany symbol daje 404.
     *
     * @param symbol Symbol budynku
     * @return Karta budynku z konturem i wejściami
     */
    @GetMapping("/buildings/{symbol}")
    BuildingCardResponse getBuilding(@PathVariable("symbol") final String symbol) {
        return this.query.building(symbol).map(
            card -> BuildingCardResponse.of(card, this.mapper)
        ).orElseThrow(
            () -> new MissingResource(String.format("Nie ma budynku %s", symbol))
        );
    }
}
