/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Warstwy planów PWr {@code dk.plans} z pliku pwr-plans.yaml: jedna warstwa na budynek
 * szczegółowy. Budynek bez wpisu jest podstawowy. Plik zawiera tylko adresy warstw, bez danych
 * PWr.
 *
 * <p>Wymagania: NFR-40, NFR-47, BR-G1</p>
 *
 * @param service Katalog usług ArcGIS PWr bez końcowego ukośnika
 * @param buildings Warstwy budynków szczegółowych
 * @param campus Warstwa wejść mapy kampusu z uwagami i szerokością drzwi
 * @since 0.1.0
 */
@ConfigurationProperties("dk.plans")
public record PlanSettings(URI service, List<Layer> buildings, Campus campus) {

    /**
     * Warstwa wejść całego kampusu w usłudze mapy kampusu PWr.
     *
     * @param name Nazwa usługi FeatureServer
     * @param entrances Numer warstwy wejść
     * @since 0.1.0
     */
    public record Campus(String name, int entrances) {
    }

    /**
     * Warstwa planu jednego budynku.
     *
     * @param symbol Symbol budynku z tabeli buildings
     * @param name Nazwa usługi FeatureServer
     * @param facility Identyfikator budynku w polu FACILITY_ID warstwy pięter
     * @param levels Numer warstwy pięter
     * @param rooms Numer warstwy wielokątów pomieszczeń (Budynek X, nie warstwa symboli)
     * @param entrances Numer warstwy wejść
     * @since 0.1.0
     */
    public record Layer(
        String symbol, String name, String facility, int levels, int rooms, int entrances
    ) {
    }
}
