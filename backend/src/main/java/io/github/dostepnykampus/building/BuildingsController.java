/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Publiczny odczyt budynków: lista, karta i podpowiedź dla punktu.
 *
 * @since 0.1.0
 */
@RestController
public final class BuildingsController {

    /**
     * Odczyt budynków z bazy.
     */
    private final BuildingQuery query;

    BuildingsController(final BuildingQuery query) {
        this.query = query;
    }

    /**
     * Zwraca budynki posortowane według symbolu, z konturem GeoJSON.
     *
     * @return Lista budynków z atrybucją konturów
     */
    @GetMapping("/buildings")
    BuildingList listBuildings() {
        return this.query.listBuildings();
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
        if (BuildingsController.outside(lat, lon)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
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
    BuildingCard getBuilding(@PathVariable("symbol") final String symbol) {
        return this.query.building(symbol).orElseThrow(BuildingsController::missing);
    }

    private static boolean outside(final double lat, final double lon) {
        return BuildingsController.bad(lat, -90, 90)
            || BuildingsController.bad(lon, -180, 180);
    }

    private static boolean bad(
        final double value, final double lower, final double upper
    ) {
        return !Double.isFinite(value) || value < lower || value > upper;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
